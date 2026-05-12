package com.auction.network.client;

import com.auction.model.user.Admin;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;
import com.auction.model.user.User;
import com.auction.network.Message;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NetworkService là một Singleton class cung cấp các dịch vụ giao tiếp mạng giữa Client và Server.
 * Lớp này duy trì một kết nối duy nhất và có luồng đọc riêng để nhận cả
 * phản hồi request/response lẫn broadcast bất đồng bộ từ server.
 */
public class NetworkService {

    private static final String DEFAULT_HOST = System.getProperty("auction.server.host", "127.0.0.1");
    private static final int DEFAULT_PORT = Integer.getInteger("auction.server.port", 5050);

    private static final NetworkService instance = new NetworkService();

    private ServerConnection sharedConnection;
    private final Map<String, CompletableFuture<Message>> pendingResponses = new ConcurrentHashMap<>();
    private final Set<AuctionUpdateListener> auctionUpdateListeners = ConcurrentHashMap.newKeySet();
    private volatile List<Map<String, Object>> latestAuctionSnapshot = List.of();

    public static NetworkService getInstance() {
        return instance;
    }

    /**
     * Lấy kết nối dùng chung, tự động khởi tạo nếu chưa có hoặc đã bị đóng.
     */
    private synchronized ServerConnection getConnection() throws IOException {
        if (sharedConnection == null || !sharedConnection.isConnected()) {
            sharedConnection = new ServerConnection(DEFAULT_HOST, DEFAULT_PORT, this::handleIncomingMessage);
            pendingResponses.clear();
        }
        return sharedConnection;
    }

    /**
     * Đóng kết nối (thường gọi khi thoát ứng dụng).
     */
    public synchronized void closeConnection() {
        try {
            if (sharedConnection != null) {
                sharedConnection.close();
                sharedConnection = null;
            }
        } catch (IOException ignored) {}
        IOException closedException = new IOException("Kết nối tới server đã đóng");
        pendingResponses.values().forEach(future -> future.completeExceptionally(closedException));
        pendingResponses.clear();
    }

    public boolean isServerReachable() {
        try {
            Message response = send(Message.Type.PING, Map.of());
            return response.isSuccess();
        } catch (Exception e) {
            return false;
        }
    }

    public Map<String, Object> getDatabaseStatus() throws IOException, ClassNotFoundException {
        Message response = send(Message.Type.DB_STATUS, Map.of());
        ensureSuccess(response);
        return response.getPayload();
    }

    public User login(String username, String password) throws IOException, ClassNotFoundException {
        Message response = send(Message.Type.LOGIN, Map.of(
                "username", username,
                "password", password
        ));
        ensureSuccess(response);
        return toUser(response.getPayload());
    }

    public User register(String username, String fullName, String email, String password, String role)
            throws IOException, ClassNotFoundException {
        Message response = send(Message.Type.REGISTER, Map.of(
                "username", username,
                "fullName", fullName,
                "email", email,
                "password", password,
                "role", role
        ));
        ensureSuccess(response);
        return toUser(response.getPayload());
    }

    public List<Map<String, Object>> getAuctions() throws IOException, ClassNotFoundException {
        Message response = send(Message.Type.GET_AUCTIONS, Map.of());
        ensureSuccess(response);
        return decodeAuctionSnapshot(response.getPayload().get("auctions"));
    }

    public Map<String, Object> placeBid(String itemId, String bidderUsername, String amount)
            throws IOException, ClassNotFoundException {
        Message response = send(Message.Type.PLACE_BID, Map.of(
                "itemId", itemId,
                "bidderUsername", bidderUsername,
                "amount", amount
        ));
        ensureSuccess(response);
        return response.getPayload();
    }

    /**
     * Gửi tin nhắn qua kết nối dùng chung.
     */
    private synchronized Message send(Message.Type type, Map<String, Object> payload) throws IOException, ClassNotFoundException {
        Message request = new Message(type, payload);
        CompletableFuture<Message> pendingResponse = new CompletableFuture<>();
        pendingResponses.put(request.getRequestId(), pendingResponse);
        try {
            getConnection().send(request);
            return pendingResponse.join();
        } catch (IOException e) {
            // Nếu lỗi, đóng kết nối cũ để lần sau tự động tạo lại cái mới
            closeConnection();
            throw e;
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof ClassNotFoundException classNotFoundException) {
                throw classNotFoundException;
            }
            if (cause instanceof IOException ioException) {
                throw ioException;
            }
            throw new IOException(cause == null ? "Server request failed" : cause.getMessage(), cause);
        } finally {
            pendingResponses.remove(request.getRequestId());
        }
    }

    private void ensureSuccess(Message response) throws IOException {
        if (!response.isSuccess()) {
            throw new IOException(response.getMessage() == null ? "Server request failed" : response.getMessage());
        }
    }

    public void addAuctionUpdateListener(AuctionUpdateListener listener) {
        if (listener == null) {
            return;
        }
        auctionUpdateListeners.add(listener);
        try {
            getConnection();
        } catch (IOException ignored) {
        }
        List<Map<String, Object>> snapshot = latestAuctionSnapshot;
        if (!snapshot.isEmpty()) {
            for (Map<String, Object> auction : snapshot) {
                listener.onAuctionUpdated(auction);
            }
        }
    }

    public void removeAuctionUpdateListener(AuctionUpdateListener listener) {
        auctionUpdateListeners.remove(listener);
    }

    public List<Map<String, Object>> getLatestAuctionSnapshot() {
        return new ArrayList<>(latestAuctionSnapshot);
    }

    private User toUser(Map<String, Object> payload) {
        String username = String.valueOf(payload.getOrDefault("username", ""));
        String email = String.valueOf(payload.getOrDefault("email", username + "@example.com"));
        String role = String.valueOf(payload.getOrDefault("role", "USER"));

        User user;
        if ("ADMIN".equalsIgnoreCase(role)) {
            user = new Admin(username, email, "", "STANDARD");
        } else if ("SELLER".equalsIgnoreCase(role)) {
            user = new Seller(username, email, "");
        } else {
            user = new Bidder(username, email, "");
        }

        user.setId(String.valueOf(payload.getOrDefault("id", user.getId())));
        user.setFullname(String.valueOf(payload.getOrDefault("fullName", username)));
        user.setActive(Boolean.parseBoolean(String.valueOf(payload.getOrDefault("active", true))));

        long balance = Long.parseLong(String.valueOf(payload.getOrDefault("balance", 0L)));
        if (balance > 0) {
            user.deposit(balance);
        }

        return user;
    }

    private void handleIncomingMessage(Message message) {
        if (message.getType() == Message.Type.AUCTION_SYNC) {
            List<Map<String, Object>> auctions = decodeAuctionSnapshot(message.getPayload().get("auctions"));
            latestAuctionSnapshot = List.copyOf(auctions);
            notifyAuctionListeners(auctions);
            return;
        }

        CompletableFuture<Message> pendingResponse = pendingResponses.get(message.getRequestId());
        if (pendingResponse != null) {
            pendingResponse.complete(message);
        }
    }

    private void notifyAuctionListeners(List<Map<String, Object>> auctions) {
        for (AuctionUpdateListener listener : auctionUpdateListeners) {
            for (Map<String, Object> auction : auctions) {
                listener.onAuctionUpdated(auction);
            }
        }
    }

    private List<Map<String, Object>> decodeAuctionSnapshot(Object auctionsPayload) {
        if (!(auctionsPayload instanceof List<?> rawList)) {
            return List.of();
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object element : rawList) {
            if (element instanceof Map<?, ?> rawMap) {
                java.util.Map<String, Object> normalizedMap = new java.util.HashMap<>();
                for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                    normalizedMap.put(String.valueOf(entry.getKey()), entry.getValue());
                }
                result.add(normalizedMap);
            }
        }
        return result;
    }
}
