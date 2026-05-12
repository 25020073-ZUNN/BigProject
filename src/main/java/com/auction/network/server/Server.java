package com.auction.network.server;

import com.auction.config.DBConnection;
import com.auction.dao.UserDao;
import com.auction.model.Auction;
import com.auction.model.BidTransaction;
import com.auction.model.item.Art;
import com.auction.model.item.Electronics;
import com.auction.model.item.Item;
import com.auction.model.item.Vehicle;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;
import com.auction.model.user.User;
import com.auction.network.Message;
import com.auction.service.AuctionService;
import com.auction.service.AuthService;
import com.auction.observer.AuctionObserver;

import java.io.*;
import java.math.BigDecimal;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Lớp Server - Trái tim của hệ thống đấu giá phía máy chủ.
 * Chịu trách nhiệm lắng nghe kết nối từ Client, giải mã các yêu cầu (Message)
 * và điều phối tới các dịch vụ nghiệp vụ (AuthService, AuctionService).
 */
public class Server {

    public static final int DEFAULT_PORT = 5050; // Cổng mặc định mà Server sẽ lắng nghe
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final int port;
    private final AuthService authService; // Dịch vụ xử lý Đăng nhập/Đăng ký
    private final AuctionService auctionService; // Dịch vụ xử lý Đấu giá
    private final UserDao userDao;
    private final ExecutorService executor; // Quản lý luồng (Thread) để xử lý nhiều Client cùng lúc
    private final Set<ClientSession> clientSessions;
    private final AuctionObserver auctionBroadcastObserver;

    private volatile boolean running; // Trạng thái hoạt động của Server
    private ServerSocket serverSocket; // Socket lắng nghe kết nối

    public Server() {
        this(DEFAULT_PORT);
    }

    public Server(int port) {
        this.port = port;
        this.authService = AuthService.getInstance();
        this.auctionService = AuctionService.getInstance();
        this.userDao = new UserDao();
        // Sử dụng CachedThreadPool để tạo luồng mới khi cần hoặc tái sử dụng luồng rảnh
        this.executor = Executors.newCachedThreadPool();
        this.clientSessions = ConcurrentHashMap.newKeySet();
        this.auctionBroadcastObserver = auctions -> broadcastAuctionSnapshot();
        this.auctionService.addAuctionObserver(auctionBroadcastObserver);
    }

    /**
     * Khởi động Server.
     */
    public void start() throws IOException {
        if (running) return;
        serverSocket = new ServerSocket(port);
        running = true;
        System.out.println("Auction server started on port " + port);

        // Vòng lặp chính: Chấp nhận mọi kết nối đến
        while (running) {
            Socket clientSocket = serverSocket.accept(); // Chờ đợi một Client kết nối
            System.out.println("[SERVER] Client mới kết nối: " + clientSocket.getRemoteSocketAddress());
            
            // Khi có Client kết nối, đẩy vào một Thread riêng để xử lý, không làm treo Server
            executor.submit(() -> handleClient(clientSocket));
        }
    }

    /**
     * Dừng Server và giải phóng tài nguyên.
     */
    public void stop() throws IOException {
        running = false;
        if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close();
        auctionService.removeAuctionObserver(auctionBroadcastObserver);
        executor.shutdownNow();
    }

    /**
     * Xử lý giao tiếp với một Client cụ thể.
     */
    private void handleClient(Socket clientSocket) {
        String clientAddress = clientSocket.getRemoteSocketAddress().toString();
        // Sử dụng try-with-resources để tự động đóng socket và luồng stream khi kết thúc
        try (Socket socket = clientSocket;
             ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream())) {
            ClientSession session = new ClientSession(socket, outputStream);
            clientSessions.add(session);
            session.send(buildAuctionSyncMessage());

            // Liên tục lắng nghe tin nhắn từ Client này cho đến khi ngắt kết nối
            while (running && !socket.isClosed()) {
                Object rawRequest = inputStream.readObject(); // Đọc đối tượng gửi từ Client
                
                // Kiểm tra định dạng tin nhắn có phải là lớp Message không
                if (!(rawRequest instanceof Message request)) {
                    outputStream.writeObject(Message.failure(null, "Định dạng yêu cầu không hợp lệ"));
                    outputStream.flush();
                    continue;
                }

                // Xử lý yêu cầu và lấy phản hồi tương ứng
                Message response = handleRequest(request);
                
                // Gửi phản hồi về cho Client
                session.send(response);
            }
        } catch (EOFException ignored) {
            // Xảy ra khi Client ngắt kết nối đột ngột
        } catch (Exception e) {
            System.err.println("[SERVER] Lỗi xử lý Client [" + clientAddress + "]: " + e.getMessage());
        } finally {
            clientSessions.removeIf(session -> session.matches(clientSocket));
            System.out.println("[SERVER] Client đã ngắt kết nối: " + clientAddress);
        }
    }

    /**
     * Bộ điều phối yêu cầu (Request Dispatcher).
     * Dựa vào loại tin nhắn (Type) để gọi phương thức xử lý phù hợp.
     */
    private Message handleRequest(Message request) {
        try {
            return switch (request.getType()) {
                case PING -> Message.success(request, Map.of("status", "pong"));
                case LOGIN -> handleLogin(request);
                case REGISTER -> handleRegister(request);
                case GET_AUCTIONS -> handleGetAuctions(request);
                case PLACE_BID -> handlePlaceBid(request);
                case DB_STATUS -> handleDatabaseStatus(request); // Đây là nơi nhận lệnh check DB từ Home
                case AUCTION_SYNC -> Message.failure(request, "Client không được phép gửi AUCTION_SYNC");
                case ERROR -> Message.failure(request, "Client gửi tin nhắn lỗi");
            };
        } catch (Exception e) {
            return Message.failure(request, e.getMessage());
        }
    }

    /**
     * Xử lý đăng nhập. Gọi sang AuthService để kiểm tra trong DB qua UserDao.
     */
    private Message handleLogin(Message request) {
        Map<String, Object> payload = request.getPayload();
        String username = stringValue(payload.get("username"));
        String password = stringValue(payload.get("password"));

        return authService.login(username, password)
                .map(user -> Message.success(request, userPayload(user)))
                .orElseGet(() -> Message.failure(request, "Tên đăng nhập hoặc mật khẩu không đúng"));
    }

    /**
     * Xử lý đăng ký thành viên mới.
     */
    private Message handleRegister(Message request) {
        Map<String, Object> payload = request.getPayload();
        String username = stringValue(payload.get("username"));
        String fullName = stringValue(payload.get("fullName"));
        String email = stringValue(payload.get("email"));
        String password = stringValue(payload.get("password"));
        String role = stringValue(payload.get("role"));

        if (username == null || username.isBlank() || email == null || email.isBlank() || password == null || password.isBlank()) {
            return Message.failure(request, "Thiếu thông tin đăng ký bắt buộc");
        }

        if (role == null || role.isBlank()) {
            role = "BIDDER";
        }

        User user;
        if ("SELLER".equalsIgnoreCase(role)) {
            user = new Seller(username, fullName == null || fullName.isBlank() ? username : fullName, email, String.valueOf(password.hashCode()));
        } else {
            user = new Bidder(username, fullName == null || fullName.isBlank() ? username : fullName, email, String.valueOf(password.hashCode()));
        }

        boolean created = authService.register(user);
        if (!created) {
            return Message.failure(request, "Đăng ký thất bại: Tên đăng nhập hoặc Email đã tồn tại");
        }

        return Message.success(request, userPayload(user));
    }

    /**
     * Lấy danh sách tất cả các phiên đấu giá đang diễn ra.
     */
    private Message handleGetAuctions(Message request) {
        List<Map<String, Object>> auctions = auctionService.getAllAuctions().stream()
                .map(this::auctionPayload)
                .collect(Collectors.toList());
        return Message.success(request, Map.of("auctions", auctions));
    }

    /**
     * Xử lý việc đặt giá của người dùng.
     */
    private Message handlePlaceBid(Message request) {
        Map<String, Object> payload = request.getPayload();
        String itemId = stringValue(payload.get("itemId"));
        String bidderName = stringValue(payload.get("bidderUsername"));
        String amountText = stringValue(payload.get("amount"));

        // Tìm phiên đấu giá tương ứng với ID tài sản
        Auction auction = auctionService.getAllAuctions().stream()
                .filter(current -> current.getItem().getId().equals(itemId))
                .findFirst()
                .orElse(null);

        if (auction == null) return Message.failure(request, "Không tìm thấy phiên đấu giá");
        if (bidderName == null || bidderName.isBlank()) return Message.failure(request, "Tên người đặt giá là bắt buộc");

        /*
         * Ghi chú quan trọng:
         * Sau khi chuyển sang DB thật, bidder dùng để đặt giá phải là user có thật trong DB.
         * Nếu tiếp tục tạo user giả trong RAM như trước, câu lệnh INSERT vào `bid_transactions`
         * sẽ lỗi khóa ngoại vì `bidder_id` không tồn tại trong bảng `users`.
         */
        User bidder = userDao.findByUsername(bidderName);
        if (bidder == null) {
            bidder = new Bidder(bidderName, bidderName + "@example.com", "");
            return Message.failure(request, "Người đặt giá chưa tồn tại trong cơ sở dữ liệu");
        }
        boolean success = auctionService.placeBid(auction, bidder, new BigDecimal(amountText));
        
        if (!success) return Message.failure(request, "Đặt giá thất bại (có thể giá của bạn thấp hơn giá hiện tại)");

        return Message.success(request, auctionPayload(auction));
    }

    /**
     * Kiểm tra trạng thái kết nối Database.
     * Đây là phương thức trả lời câu hỏi của bạn về việc "check" ở đâu.
     */
    private Message handleDatabaseStatus(Message request) {
        // AuthService.isDatabaseAvailable() sẽ gọi tới DBConnection để kiểm tra
        boolean available = authService.isDatabaseAvailable();
        return Message.success(request, Map.of(
                "available", available,
                "dbUrl", DBConnection.getConfiguredUrl(), // Lấy URL cấu hình từ DBConnection
                "dbUser", DBConnection.getConfiguredUser() // Lấy Username cấu hình từ DBConnection
        ));
    }

    /**
     * Chuyển đổi đối tượng User sang Map để gửi qua mạng.
     */
    private Map<String, Object> userPayload(User user) {
        return Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "fullName", user.getFullname(),
                "email", user.getEmail(),
                "role", user.getRole(),
                "balance", user.getBalance(),
                "active", user.isActive()
        );
    }

    /**
     * Chuyển đổi đối tượng Auction sang Map để gửi qua mạng.
     */
    private Map<String, Object> auctionPayload(Auction auction) {
        Item item = auction.getItem();
        Map<String, Object> payload = new HashMap<>();
        payload.put("auctionId", auction.getId());
        payload.put("itemId", item.getId());
        payload.put("itemName", item.getName());
        payload.put("description", item.getDescription());
        payload.put("category", item.getCategory());
        payload.put("sellerId", auction.getSeller().getId());
        payload.put("sellerName", auction.getSeller().getUsername());
        payload.put("startingPrice", auction.getStartingPrice().toPlainString());
        payload.put("currentPrice", auction.getCurrentPrice().toPlainString());
        payload.put("bidStep", auction.getMinimumBidStep() == null ? "0" : auction.getMinimumBidStep().toPlainString());
        payload.put("active", auction.isActive());
        payload.put("finished", auction.isFinished());
        payload.put("startTime", item.getStartTime().format(DATE_FORMATTER));
        payload.put("endTime", item.getEndTime().format(DATE_FORMATTER));
        payload.put("seller", userPayload(auction.getSeller()));
        payload.put("highestBidder", auction.getHighestBidder() == null ? null : userPayload(auction.getHighestBidder()));
        payload.put("item", itemPayload(item));
        payload.put("bidHistory", auction.getBidHistory().stream().map(this::bidPayload).collect(Collectors.toList()));
        return payload;
    }

    private Map<String, Object> itemPayload(Item item) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", item.getId());
        payload.put("name", item.getName());
        payload.put("description", item.getDescription());
        payload.put("category", item.getCategory());
        payload.put("sellerId", item.getSellerId());
        payload.put("startingPrice", item.getStartingPrice().toPlainString());
        payload.put("currentPrice", item.getCurrentPrice().toPlainString());
        payload.put("startTime", item.getStartTime().format(DATE_FORMATTER));
        payload.put("endTime", item.getEndTime().format(DATE_FORMATTER));
        payload.put("attributes", itemAttributesPayload(item));
        return payload;
    }

    private Map<String, Object> bidPayload(BidTransaction transaction) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", transaction.getId());
        payload.put("bidAmount", transaction.getBidAmount().toPlainString());
        payload.put("bidTime", transaction.getBidTime().format(DATE_FORMATTER));
        payload.put("bidder", userPayload(transaction.getBidder()));
        return payload;
    }

    private Map<String, Object> itemAttributesPayload(Item item) {
        Map<String, Object> attributes = new HashMap<>();
        if (item instanceof Electronics electronics) {
            attributes.put("brand", electronics.getBrand());
            attributes.put("warrantyMonths", electronics.getWarrantyMonths());
        } else if (item instanceof Vehicle vehicle) {
            attributes.put("manufacturer", vehicle.getManufacturer());
            attributes.put("year", vehicle.getYear());
            attributes.put("mileage", vehicle.getMileage());
        } else if (item instanceof Art art) {
            attributes.put("artist", art.getArtist());
            attributes.put("yearCreated", art.getYearCreated());
        }
        return attributes;
    }

    /**
     * Tiện ích chuyển đổi giá trị sang String và cắt khoảng trắng.
     */
    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }

    private void broadcastAuctionSnapshot() {
        if (clientSessions.isEmpty()) {
            return;
        }

        Message snapshot = buildAuctionSyncMessage();
        List<ClientSession> disconnectedSessions = new ArrayList<>();
        for (ClientSession session : clientSessions) {
            try {
                session.send(snapshot);
            } catch (IOException e) {
                disconnectedSessions.add(session);
            }
        }
        clientSessions.removeAll(disconnectedSessions);
    }

    private Message buildAuctionSyncMessage() {
        List<Map<String, Object>> auctions = auctionService.getAllAuctions().stream()
                .map(this::auctionPayload)
                .collect(Collectors.toList());
        return new Message(UUID.randomUUID().toString(), Message.Type.AUCTION_SYNC, Map.of("auctions", auctions), true, null);
    }

    /**
     * Điểm chạy chương trình Server.
     */
    public static void main(String[] args) throws IOException {
        int serverPort = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        new Server(serverPort).start();
    }

    private static final class ClientSession {
        private final Socket socket;
        private final ObjectOutputStream outputStream;

        private ClientSession(Socket socket, ObjectOutputStream outputStream) {
            this.socket = socket;
            this.outputStream = outputStream;
        }

        private synchronized void send(Message message) throws IOException {
            outputStream.writeObject(message);
            outputStream.flush();
        }

        private boolean matches(Socket candidate) {
            return socket == candidate;
        }
    }

    /**
     * Lớp ServerConnection chịu trách nhiệm thiết lập và quản lý kết nối Socket từ Client tới Server.
     * Nó sử dụng ObjectStream để truyền tải các đối tượng Message qua mạng.
     */
    public static class ServerConnection implements Closeable {

        private final Socket socket; // Socket kết nối đến server
        private final ObjectOutputStream outputStream; // Luồng xuất dữ liệu (gửi message)
        private final ObjectInputStream inputStream; // Luồng nhập dữ liệu (nhận phản hồi)

        /**
         * Khởi tạo một kết nối mới tới Server.
         * @param host Địa chỉ IP hoặc tên miền của Server
         * @param port Cổng dịch vụ của Server
         * @throws IOException Nếu không thể thiết lập kết nối hoặc khởi tạo luồng dữ liệu
         */
        public ServerConnection(String host, int port) throws IOException {
            this.socket = new Socket(host, port);
            // Lưu ý: Phải khởi tạo ObjectOutputStream trước ObjectInputStream để tránh tình trạng deadlock (treo luồng)
            this.outputStream = new ObjectOutputStream(socket.getOutputStream());
            this.inputStream = new ObjectInputStream(socket.getInputStream());
        }

        /**
         * Kiểm tra xem kết nối có còn hoạt động không.
         */
        public boolean isConnected() {
            return socket != null && !socket.isClosed() && socket.isConnected();
        }

        /**
         * Gửi một yêu cầu tới Server dựa trên loại Message và dữ liệu đi kèm.
         */
        public Message send(Message.Type type, Map<String, Object> payload) throws IOException, ClassNotFoundException {
            return send(new Message(type, payload));
        }

        /**
         * Gửi đối tượng Message tới Server và đợi nhận phản hồi.
         * @param message Đối tượng chứa thông tin yêu cầu
         * @return Message Phản hồi từ Server
         * @throws IOException Nếu có lỗi trong quá trình truyền tải dữ liệu
         * @throws ClassNotFoundException Nếu lớp của đối tượng nhận được không tồn tại ở phía Client
         */
        public Message send(Message message) throws IOException, ClassNotFoundException {
            // Ghi đối tượng vào luồng xuất
            outputStream.writeObject(message);
            outputStream.flush(); // Đẩy dữ liệu đi ngay lập tức

            // Đọc phản hồi từ luồng nhập
            Object response = inputStream.readObject();
            if (!(response instanceof Message serverMessage)) {
                throw new IOException("Phản hồi từ server không đúng định dạng Message");
            }
            return serverMessage;
        }

        /**
         * Đóng tất cả các tài nguyên kết nối (Luồng và Socket).
         */
        @Override
        public void close() throws IOException {
            if (inputStream != null) inputStream.close();
            if (outputStream != null) outputStream.close();
            if (socket != null) socket.close();
        }
    }
}
