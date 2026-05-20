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

/**
 * NetworkService - Singleton giao tiếp mạng giữa Client và Server.
 * Sử dụng giao thức Java Serialization qua TCP (đối tượng Message). 
 * Luồng đọc riêng xử lý các thông báo broadcast (như AUCTION_SYNC) một cách bất đồng bộ.
 * Các phương thức public KHÔNG chặn luồng FX — được thiết kế để gọi từ FxAsync.
 */
public class NetworkService {

    // Địa chỉ host và cổng mặc định cho Server, có thể cấu hình qua System Property
    private static final String DEFAULT_HOST = System.getProperty("auction.server.host", "127.0.0.1");
    private static final int DEFAULT_PORT = Integer.getInteger("auction.server.port", 5050);

    // Instance duy nhất (Singleton Pattern)
    private static final NetworkService instance = new NetworkService();

    private ServerConnection sharedConnection; // Kết nối hiện tại tới server
    private final Object connectionLock = new Object(); // Khóa đồng bộ cho việc tạo kết nối
    
    // Lưu trữ các yêu cầu đang chờ phản hồi dựa trên Request ID
    private final Map<String, CompletableFuture<Message>> pendingResponses = new ConcurrentHashMap<>();
    
    // Danh sách các listener đăng ký nhận cập nhật đấu giá
    private final Set<AuctionUpdateListener> auctionUpdateListeners = ConcurrentHashMap.newKeySet();
    
    // Bản snapshot mới nhất của dữ liệu đấu giá
    private volatile List<Map<String, Object>> latestAuctionSnapshot = List.of();

    /**
     * Lấy instance duy nhất của NetworkService.
     */
    public static NetworkService getInstance() {
        return instance;
    }

    /**
     * Lấy kết nối dùng chung, đảm bảo thread-safe. Tạo mới nếu chưa có hoặc đã đóng.
     * 
     * @return ServerConnection hiện tại.
     * @throws IOException Nếu không thể tạo kết nối.
     */
    private ServerConnection getConnection() throws IOException {
        synchronized (connectionLock) {
            if (sharedConnection == null || !sharedConnection.isConnected()) {
                sharedConnection = new ServerConnection(DEFAULT_HOST, DEFAULT_PORT, this::handleIncomingMessage);
                pendingResponses.clear(); // Xóa các yêu cầu cũ khi kết nối lại
            }
            return sharedConnection;
        }
    }

    /**
     * Đóng kết nối hiện tại và hủy tất cả các yêu cầu đang chờ.
     */
    public synchronized void closeConnection() {
        try {
            if (sharedConnection != null) {
                sharedConnection.close();
                sharedConnection = null;
            }
        } catch (IOException ignored) {}
        
        // Hoàn thành tất cả các future đang chờ với một ngoại lệ
        IOException closedException = new IOException("Kết nối tới server đã đóng");
        pendingResponses.values().forEach(future -> future.completeExceptionally(closedException));
        pendingResponses.clear();
    }

    /**
     * Kiểm tra xem server có đang hoạt động hay không bằng cách gửi gói tin PING.
     */
    public boolean isServerReachable() {
        try {
            Message response = send(Message.Type.PING, Map.of());
            return response.isSuccess();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Lấy trạng thái cơ sở dữ liệu từ server.
     */
    public Map<String, Object> getDatabaseStatus() throws IOException {
        Message response = send(Message.Type.DB_STATUS, Map.of());
        ensureSuccess(response);
        return response.getPayload();
    }

    /**
     * Gửi yêu cầu đăng nhập.
     * 
     * @return Đối tượng User sau khi đăng nhập thành công.
     */
    public User login(String username, String password) throws IOException {
        Message response = send(Message.Type.LOGIN, Map.of(
                "username", username,
                "password", password
        ));
        ensureSuccess(response);
        return toUser(response.getPayload());
    }

    /**
     * Gửi yêu cầu đăng ký tài khoản mới.
     */
    public User register(String username, String fullName, String email, String password)
            throws IOException {
        Message response = send(Message.Type.REGISTER, Map.of(
                "username", username,
                "fullName", fullName,
                "email", email,
                "password", password
        ));
        ensureSuccess(response);
        return toUser(response.getPayload());
    }

    /**
     * Gửi yêu cầu cập nhật thông tin cá nhân.
     */
    public User updateProfile(String username, String fullName, String email) throws IOException {
        Message response = send(Message.Type.UPDATE_PROFILE, Map.of(
                "username", username,
                "fullName", fullName,
                "email", email
        ));
        ensureSuccess(response);
        return toUser(response.getPayload());
    }

    /**
     * Gửi yêu cầu xóa tài khoản.
     */
    public void deleteAccount(String username) throws IOException {
        Message response = send(Message.Type.DELETE_ACCOUNT, Map.of(
                "username", username
        ));
        ensureSuccess(response);
    }

    /**
     * Lấy danh sách các cuộc đấu giá hiện có từ server.
     */
    public List<Map<String, Object>> getAuctions() throws IOException {
        Message response = send(Message.Type.GET_AUCTIONS, Map.of());
        ensureSuccess(response);
        return decodeAuctionSnapshot(response.getPayload().get("auctions"));
    }

    /**
     * Gửi yêu cầu trả giá cho một sản phẩm.
     */
    public Map<String, Object> placeBid(String itemId, String bidderUsername, String amount)
            throws IOException {
        Message response = send(Message.Type.PLACE_BID, Map.of(
                "itemId", itemId,
                "bidderUsername", bidderUsername,
                "amount", amount
        ));
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
     * Gửi một Message và chờ phản hồi đồng bộ (nhưng không chặn luồng chính lâu dài).
     * Chỉ quá trình khởi tạo kết nối là được đồng bộ hóa.
     * 
     * @param type Loại tin nhắn.
     * @param payload Dữ liệu đi kèm.
     * @return Message phản hồi từ server.
     * @throws IOException Nếu có lỗi xảy ra.
     */
    private Message send(Message.Type type, Map<String, Object> payload) throws IOException {
        Message request = new Message(type, payload);
        CompletableFuture<Message> pendingResponse = new CompletableFuture<>();
        pendingResponses.put(request.getRequestId(), pendingResponse);
        try {
            getConnection().send(request);
            // Chờ phản hồi từ luồng nhận dữ liệu
            return pendingResponse.join();
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException ioEx) throw ioEx;
            throw new IOException(cause == null ? "Yêu cầu server thất bại" : cause.getMessage(), cause);
        } catch (IOException e) {
            closeConnection();
            throw e;
        } finally {
            // Đảm bảo xóa yêu cầu khỏi map sau khi hoàn thành hoặc lỗi
            pendingResponses.remove(request.getRequestId());
        }
    }

    /**
     * Kiểm tra xem phản hồi từ server có thành công hay không.
     */
    private void ensureSuccess(Message response) throws IOException {
        if (!response.isSuccess()) {
            throw new IOException(response.getMessage() == null ? "Yêu cầu server thất bại" : response.getMessage());
        }
    }

    /**
     * Đăng ký một listener để nhận cập nhật khi có thay đổi đấu giá.
     * Ngay khi đăng ký, listener sẽ nhận được snapshot mới nhất hiện có.
     */
    public void addAuctionUpdateListener(AuctionUpdateListener listener) {
        if (listener == null) return;
        auctionUpdateListeners.add(listener);
        try {
            getConnection(); // Đảm bảo kết nối đã được thiết lập để nhận broadcast
        } catch (IOException ignored) {}
        
        List<Map<String, Object>> snapshot = latestAuctionSnapshot;
        if (!snapshot.isEmpty()) {
            for (Map<String, Object> auction : snapshot) {
                listener.onAuctionUpdated(auction);
            }
        }
    }

    /**
     * Hủy đăng ký listener.
     */
    public void removeAuctionUpdateListener(AuctionUpdateListener listener) {
        auctionUpdateListeners.remove(listener);
    }

    /**
     * Lấy bản snapshot mới nhất của các cuộc đấu giá.
     */
    public List<Map<String, Object>> getLatestAuctionSnapshot() {
        return new ArrayList<>(latestAuctionSnapshot);
    }

    /**
     * Chuyển đổi payload dữ liệu người dùng thành đối tượng domain User cụ thể.
     */
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

        // Xử lý nạp tiền cho số dư ban đầu
        Object rawBalance = payload.getOrDefault("balance", 0);
        long balance = (rawBalance instanceof Number n) ? n.longValue() : Long.parseLong(String.valueOf(rawBalance));
        if (balance > 0) {
            user.deposit(balance);
        }

        return user;
    }

    /**
     * Trình xử lý tin nhắn đến được gọi từ luồng đọc của ServerConnection.
     * 
     * @param message Tin nhắn nhận được.
     */
    private void handleIncomingMessage(Message message) {
        // Nếu là tin nhắn đồng bộ dữ liệu đấu giá (broadcast từ server)
        if (message.getType() == Message.Type.AUCTION_SYNC) {
            List<Map<String, Object>> auctions = decodeAuctionSnapshot(message.getPayload().get("auctions"));
            latestAuctionSnapshot = List.copyOf(auctions);
            notifyAuctionListeners(auctions);
            return;
        }

        // Nếu là phản hồi cho một yêu cầu cụ thể (trùng Request ID)
        CompletableFuture<Message> pendingResponse = pendingResponses.get(message.getRequestId());
        if (pendingResponse != null) {
            pendingResponse.complete(message);
        }
    }

    /**
     * Thông báo cho các listeners về sự thay đổi của danh sách đấu giá.
     */
    private void notifyAuctionListeners(List<Map<String, Object>> auctions) {
        for (AuctionUpdateListener listener : auctionUpdateListeners) {
            // Thông báo cập nhật. Ở đây chọn notify phần tử đầu tiên 
            // như một tín hiệu báo hiệu toàn bộ danh sách đã thay đổi.
            if (!auctions.isEmpty()) {
                listener.onAuctionUpdated(auctions.get(0));
            }
        }
    }

    /**
     * Giải mã dữ liệu thô từ payload thành danh sách các Map đấu giá chuẩn hóa.
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> decodeAuctionSnapshot(Object auctionsPayload) {
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
