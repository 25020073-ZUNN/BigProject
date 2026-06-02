package com.auction.network.client;

import com.auction.model.user.Admin;
import com.auction.model.user.Bidder;
import com.auction.model.user.RegisteredUser;
import com.auction.model.user.Seller;
import com.auction.model.user.User;
import com.auction.network.Message;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * NetworkService - Singleton giao tiếp mạng giữa Client và Server.
 * Sử dụng giao thức JSON qua TCP. Luồng đọc riêng xử lý broadcast bất đồng bộ.
 * Các phương thức public KHÔNG chặn luồng FX — gọi từ FxAsync.
 */
public class NetworkService {

    private static final String DEFAULT_HOST = System.getProperty("auction.server.host", "100.69.190.79");
    private static final int DEFAULT_PORT = Integer.getInteger("auction.server.port", 5050);

    private static final NetworkService instance = new NetworkService();

    private ServerConnection sharedConnection;
    private final Object connectionLock = new Object();
    private final Map<String, CompletableFuture<Message>> pendingResponses = new ConcurrentHashMap<>();
    private final Set<AuctionUpdateListener> auctionUpdateListeners = ConcurrentHashMap.newKeySet();
    private volatile List<Map<String, Object>> latestAuctionSnapshot = List.of();

    public static NetworkService getInstance() {
        return instance;
    }

    /**
     * Lấy kết nối dùng chung, thread-safe. Tạo mới nếu cần.
     */
    private ServerConnection getConnection() throws IOException {
        synchronized (connectionLock) {
            if (sharedConnection == null || !sharedConnection.isConnected()) {
                sharedConnection = new ServerConnection(DEFAULT_HOST, DEFAULT_PORT, this::handleIncomingMessage);
                pendingResponses.clear();
            }
            return sharedConnection;
        }
    }

    public synchronized void closeConnection() {
        try {
            if (sharedConnection != null) {
                sharedConnection.close();
                sharedConnection = null;
            }
        } catch (IOException ignored) {
        }
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

    public Map<String, Object> getDatabaseStatus() throws IOException {
        Message response = send(Message.Type.DB_STATUS, Map.of());
        ensureSuccess(response);
        return response.getPayload();
    }

    public User login(String username, String password) throws IOException {
        Message response = send(Message.Type.LOGIN, Map.of(
                "username", username,
                "password", password));
        ensureSuccess(response);
        return toUser(response.getPayload());
    }

    public User register(String username, String fullName, String email, String password)
            throws IOException {
        Message response = send(Message.Type.REGISTER, Map.of(
                "username", username,
                "fullName", fullName,
                "email", email,
                "password", password));
        ensureSuccess(response);
        return toUser(response.getPayload());
    }

    public User updateProfile(String username, String fullName, String email) throws IOException {
        Message response = send(Message.Type.UPDATE_PROFILE, Map.of(
                "username", username,
                "fullName", fullName,
                "email", email));
        ensureSuccess(response);
        return toUser(response.getPayload());
    }

    public void deleteAccount(String username) throws IOException {
        Message response = send(Message.Type.DELETE_ACCOUNT, Map.of(
                "username", username));
        ensureSuccess(response);
    }

    public String requestPasswordReset(String emailOrUsername) throws IOException {
        Message response = send(Message.Type.REQUEST_PASSWORD_RESET, Map.of(
                "emailOrUsername", emailOrUsername));
        ensureSuccess(response);
        return (String) response.getPayload().get("email");
    }

    public void resetPassword(String emailOrUsername, String token, String newPassword) throws IOException {
        Message response = send(Message.Type.RESET_PASSWORD, Map.of(
                "emailOrUsername", emailOrUsername,
                "token", token,
                "newPassword", newPassword));
        ensureSuccess(response);
    }

    public User getCurrentUser(String username) throws IOException {
        Message response = send(Message.Type.GET_CURRENT_USER, Map.of(
                "username", username));
        ensureSuccess(response);
        return toUser(response.getPayload());
    }

    public List<Map<String, Object>> getUsers(String adminUsername) throws IOException {
        Message response = send(Message.Type.GET_USERS, Map.of(
                "adminUsername", adminUsername));
        ensureSuccess(response);
        return decodeMapList(response.getPayload().get("users"));
    }

    public void setUserActive(String adminUsername, String targetUsername, boolean active) throws IOException {
        Message response = send(Message.Type.SET_USER_ACTIVE, Map.of(
                "adminUsername", adminUsername,
                "targetUsername", targetUsername,
                "active", active));
        ensureSuccess(response);
    }

    public void deleteAuction(String adminUsername, String auctionId) throws IOException {
        Message response = send(Message.Type.DELETE_AUCTION, Map.of(
                "adminUsername", adminUsername,
                "auctionId", auctionId));
        ensureSuccess(response);
    }

    public List<Map<String, Object>> getAuctions() throws IOException {
        Message response = send(Message.Type.GET_AUCTIONS, Map.of());
        ensureSuccess(response);
        return decodeAuctionSnapshot(response.getPayload().get("auctions"));
    }

    public Map<String, Object> placeBid(String itemId, String bidderUsername, String amount)
            throws IOException {
        Message response = send(Message.Type.PLACE_BID, Map.of(
                "itemId", itemId,
                "bidderUsername", bidderUsername,
                "amount", amount));
        ensureSuccess(response);
        return response.getPayload();
    }

    /**
     * Gửi yêu cầu tạo phiên đấu giá mới tới Server.
     */
    public void createAuction(String itemType, String name, String description,
            String startingPrice, String bidStep,
            String startTime, String endTime,
            String sellerUsername, Map<String, Object> attributes)
            throws IOException {
        Map<String, Object> payload = new HashMap<>();
        payload.put("itemType", itemType);
        payload.put("name", name);
        payload.put("description", description);
        payload.put("startingPrice", startingPrice);
        payload.put("bidStep", bidStep);
        payload.put("startTime", startTime);
        payload.put("endTime", endTime);
        payload.put("sellerUsername", sellerUsername);
        payload.put("attributes", new HashMap<>(attributes));

        Message response = send(Message.Type.CREATE_AUCTION, payload);
        ensureSuccess(response);
    }

    /**
     * Gửi yêu cầu cập nhật phiên đấu giá (chỉ khi phiên chưa bắt đầu).
     */
    public void updateAuction(String auctionId, String itemType, String name, String description,
            String startingPrice, String bidStep,
            String startTime, String endTime,
            String sellerUsername, Map<String, Object> attributes)
            throws IOException {
        Map<String, Object> payload = new HashMap<>();
        payload.put("auctionId", auctionId);
        payload.put("itemType", itemType);
        payload.put("name", name);
        payload.put("description", description);
        payload.put("startingPrice", startingPrice);
        payload.put("bidStep", bidStep);
        payload.put("startTime", startTime);
        payload.put("endTime", endTime);
        payload.put("sellerUsername", sellerUsername);
        payload.put("attributes", new HashMap<>(attributes));

        Message response = send(Message.Type.UPDATE_AUCTION, payload);
        ensureSuccess(response);
    }

    /**
     * Người bán xóa phiên đấu giá của chính mình (chỉ khi phiên chưa bắt đầu).
     */
    public void sellerDeleteAuction(String sellerUsername, String auctionId) throws IOException {
        Message response = send(Message.Type.SELLER_DELETE_AUCTION, Map.of(
                "sellerUsername", sellerUsername,
                "auctionId", auctionId));
        ensureSuccess(response);
    }

    /**
     * Gửi message và chờ phản hồi. KHÔNG synchronized trên toàn bộ method —
     * chỉ connection init là synchronized — để nhiều thread có thể gửi đồng thời.
     */
    private Message send(Message.Type type, Map<String, Object> payload) throws IOException {
        Message request = new Message(type, payload);
        CompletableFuture<Message> pendingResponse = new CompletableFuture<>();
        pendingResponses.put(request.getRequestId(), pendingResponse);
        try {
            getConnection().send(request);
            return pendingResponse.get(15, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new IOException("Server không phản hồi sau 15 giây. Kiểm tra lại kết nối.");
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException ioEx)
                throw ioEx;
            throw new IOException(cause == null ? "Server request failed" : cause.getMessage(), cause);
        } catch (java.util.concurrent.ExecutionException e) {
            Throwable cause = e.getCause();
            throw new IOException(cause == null ? "Server request failed" : cause.getMessage(), cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Yêu cầu bị gián đoạn");
        } catch (IOException e) {
            closeConnection();
            throw e;
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
        if (listener == null)
            return;
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
        } else if ("USER".equalsIgnoreCase(role)) {
            user = new RegisteredUser(username, email, "");
        } else if ("SELLER".equalsIgnoreCase(role)) {
            user = new Seller(username, email, "");
        } else {
            user = new Bidder(username, email, "");
        }

        user.setId(String.valueOf(payload.getOrDefault("id", user.getId())));
        user.setFullname(String.valueOf(payload.getOrDefault("fullName", username)));
        user.setActive(Boolean.parseBoolean(String.valueOf(payload.getOrDefault("active", true))));

        Object rawBalance = payload.getOrDefault("balance", 0);
        long balance = (rawBalance instanceof Number n) ? n.longValue() : Long.parseLong(String.valueOf(rawBalance));
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
            // Thông báo một lần cho mỗi listener với snapshot đầu tiên,
            // không gọi N lần cho N auction (gây dư thừa Platform.runLater)
            if (!auctions.isEmpty()) {
                listener.onAuctionUpdated(auctions.get(0));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> decodeAuctionSnapshot(Object auctionsPayload) {
        return decodeMapList(auctionsPayload);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> decodeMapList(Object auctionsPayload) {
        if (!(auctionsPayload instanceof List<?> rawList)) {
            return List.of();
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object element : rawList) {
            if (element instanceof Map<?, ?> rawMap) {
                Map<String, Object> normalizedMap = new HashMap<>();
                for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                    normalizedMap.put(String.valueOf(entry.getKey()), entry.getValue());
                }
                result.add(normalizedMap);
            }
        }
        return result;
    }
}
/*NetworkService là lớp Singleton chịu trách nhiệm giao tiếp giữa Client và Server bằng JSON qua TCP.
Nó quản lý kết nối dùng chung, gửi request bất đồng bộ bằng CompletableFuture, hỗ trợ realtime update thông qua listener và đảm bảo thread-safe khi nhiều thao tác mạng diễn ra đồng thời.*/