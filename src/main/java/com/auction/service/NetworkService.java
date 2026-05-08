package com.auction.service;

import com.auction.client.network.ServerConnection;
import com.auction.model.user.Admin;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;
import com.auction.model.user.User;
import com.auction.network.Message;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * NetworkService là một Singleton class cung cấp các dịch vụ giao tiếp mạng giữa Client và Server.
 * Lớp này xử lý việc gửi các yêu cầu như đăng nhập, đăng ký, lấy danh sách đấu giá và đặt giá thầu.
 */
public class NetworkService {

    // Địa chỉ host mặc định của Server, lấy từ system property hoặc mặc định là localhost
    private static final String DEFAULT_HOST = System.getProperty("auction.server.host", "127.0.0.1");
    // Cổng (port) mặc định của Server
    private static final int DEFAULT_PORT = Integer.getInteger("auction.server.port", 5050);

    // Instance duy nhất của NetworkService (Singleton pattern)
    private static final NetworkService instance = new NetworkService();

    /**
     * Lấy instance duy nhất của NetworkService.
     * @return NetworkService instance
     */
    public static NetworkService getInstance() {
        return instance;
    }

    /**
     * Kiểm tra xem Server có đang hoạt động hay không bằng cách gửi một tin nhắn PING.
     * @return true nếu Server phản hồi thành công, ngược lại false
     */
    public boolean isServerReachable() {
        try {
            Message response = send(Message.Type.PING, Map.of());
            return response.isSuccess();
        } catch (IOException | ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Lấy trạng thái của cơ sở dữ liệu từ Server.
     * @return Map chứa các thông tin về trạng thái DB
     * @throws IOException Nếu có lỗi kết nối mạng
     * @throws ClassNotFoundException Nếu không tìm thấy class trong quá trình deserialize
     */
    public Map<String, Object> getDatabaseStatus() throws IOException, ClassNotFoundException {
        Message response = send(Message.Type.DB_STATUS, Map.of());
        ensureSuccess(response);
        return response.getPayload();
    }

    /**
     * Thực hiện đăng nhập người dùng.
     * @param username Tên đăng nhập
     * @param password Mật khẩu
     * @return Đối tượng User sau khi đăng nhập thành công
     * @throws IOException Nếu đăng nhập thất bại hoặc lỗi mạng
     * @throws ClassNotFoundException Lỗi deserialize dữ liệu
     */
    public User login(String username, String password) throws IOException, ClassNotFoundException {
        Message response = send(Message.Type.LOGIN, Map.of(
                "username", username,
                "password", password
        ));
        ensureSuccess(response);
        return toUser(response.getPayload());
    }

    /**
     * Đăng ký tài khoản người dùng mới.
     * @param username Tên đăng nhập
     * @param fullName Họ và tên
     * @param email Địa chỉ email
     * @param password Mật khẩu
     * @param role Vai trò (BIDDER, SELLER, ADMIN)
     * @return Đối tượng User vừa được tạo
     * @throws IOException Nếu đăng ký thất bại hoặc lỗi mạng
     */
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

    /**
     * Lấy danh sách các cuộc đấu giá đang diễn ra từ Server.
     * @return Danh sách các Map chứa thông tin đấu giá
     * @throws IOException Lỗi kết nối
     */
    public List<Map<String, Object>> getAuctions() throws IOException, ClassNotFoundException {
        Message response = send(Message.Type.GET_AUCTIONS, Map.of());
        ensureSuccess(response);

        Object auctions = response.getPayload().get("auctions");
        if (!(auctions instanceof List<?> rawList)) {
            return List.of();
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object element : rawList) {
            if (element instanceof Map<?, ?> rawMap) {
                result.add(rawMap.entrySet().stream()
                        .collect(java.util.stream.Collectors.toMap(
                                entry -> String.valueOf(entry.getKey()),
                                Map.Entry::getValue
                        )));
            }
        }
        return result;
    }

    /**
     * Thực hiện đặt giá thầu cho một vật phẩm.
     * @param itemId ID của vật phẩm
     * @param bidderUsername Tên người đặt giá
     * @param amount Số tiền đặt giá
     * @return Kết quả phản hồi từ server
     * @throws IOException Nếu đặt giá thất bại
     */
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
     * Gửi một thông điệp đến Server và nhận phản hồi.
     * @param type Loại thông điệp
     * @param payload Dữ liệu gửi đi
     * @return Thông điệp phản hồi từ Server
     */
    private Message send(Message.Type type, Map<String, Object> payload) throws IOException, ClassNotFoundException {
        try (ServerConnection connection = new ServerConnection(DEFAULT_HOST, DEFAULT_PORT)) {
            return connection.send(type, payload);
        }
    }

    /**
     * Kiểm tra xem phản hồi từ Server có thành công hay không.
     * @param response Thông điệp từ Server
     * @throws IOException Nếu phản hồi báo lỗi
     */
    private void ensureSuccess(Message response) throws IOException {
        if (!response.isSuccess()) {
            throw new IOException(response.getMessage() == null ? "Server request failed" : response.getMessage());
        }
    }

    /**
     * Chuyển đổi dữ liệu nhận được từ Server thành đối tượng User cụ thể dựa trên role.
     * @param payload Map chứa thông tin người dùng
     * @return Đối tượng User (Admin, Seller, hoặc Bidder)
     */
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
}
