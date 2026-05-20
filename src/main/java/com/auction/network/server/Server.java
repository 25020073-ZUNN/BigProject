package com.auction.network.server;

import com.auction.config.DBConnection;
import com.auction.dao.UserDao;
import com.auction.model.Auction;
import com.auction.model.BidTransaction;
import com.auction.model.item.Art;
import com.auction.model.item.Electronics;
import com.auction.model.item.Item;
import com.auction.model.item.Vehicle;
import com.auction.model.user.RegisteredUser;
import com.auction.model.user.User;
import com.auction.network.Message;
import com.auction.service.AuctionService;
import com.auction.service.AuthService;
import com.auction.util.ValidationUtil;
import com.auction.observer.AuctionObserver;

import java.io.*;
import java.math.BigDecimal;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
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
 * Chịu trách nhiệm quản lý kết nối từ client, xử lý các yêu cầu (đăng nhập, trả giá, tạo đấu giá)
 * và phát sóng (broadcast) các cập nhật trạng thái phiên đấu giá tới tất cả client đang kết nối.
 * Giao tiếp qua giao thức JSON trên TCP (mỗi tin nhắn là một dòng JSON).
 */
public class Server {

    // Cổng mặc định của Server
    public static final int DEFAULT_PORT = 5050;
    // Định dạng thời gian chuẩn để trao đổi giữa Client và Server
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final int port;                             // Cổng server đang lắng nghe
    private final AuthService authService;              // Dịch vụ xác thực và quản lý người dùng
    private final AuctionService auctionService;        // Dịch vụ quản lý logic đấu giá
    private final UserDao userDao;                      // Truy cập dữ liệu người dùng trực tiếp
    private final ExecutorService executor;             // Quản lý các luồng (threads) xử lý client
    private final Set<ClientSession> clientSessions;    // Danh sách các phiên kết nối đang hoạt động
    private final AuctionObserver auctionBroadcastObserver; // Observer để nhận biết khi nào cần gửi cập nhật cho client

    private volatile boolean running;                   // Trạng thái hoạt động của server
    private ServerSocket serverSocket;                  // Socket server chính

    /**
     * Khởi tạo Server với cổng mặc định.
     */
    public Server() {
        this(DEFAULT_PORT);
    }

    /**
     * Khởi tạo Server với cổng chỉ định.
     * Thiết lập các dịch vụ, bộ nhớ đệm và observer.
     */
    public Server(int port) {
        this.port = port;
        this.authService = AuthService.getInstance();
        this.auctionService = AuctionService.getInstance();
        this.userDao = new UserDao();
        // Sử dụng CachedThreadPool để linh hoạt số lượng thread theo số lượng client
        this.executor = Executors.newCachedThreadPool();
        // Sử dụng Concurrent Set để đảm bảo thread-safe khi thêm/xóa session
        this.clientSessions = ConcurrentHashMap.newKeySet();
        
        // Thiết lập observer: Mỗi khi AuctionService thông báo thay đổi, server sẽ gửi snapshot mới nhất cho client
        this.auctionBroadcastObserver = auctions -> broadcastAuctionSnapshot();
        this.auctionService.addAuctionObserver(auctionBroadcastObserver);
    }

    /**
     * Bắt đầu chạy Server. Lắng nghe các kết nối đến và giao cho executor xử lý.
     */
    public void start() throws IOException {
        if (running)
            return;
        serverSocket = new ServerSocket(port);
        running = true;
        System.out.println("Auction server started on port " + port + " (JSON protocol)");

        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[SERVER] Client mới kết nối: " + clientSocket.getRemoteSocketAddress());
                // Mỗi client sẽ được xử lý trong một luồng riêng biệt
                executor.submit(() -> handleClient(clientSocket));
            } catch (IOException e) {
                if (running) System.err.println("[SERVER] Lỗi chấp nhận kết nối: " + e.getMessage());
            }
        }
    }

    /**
     * Dừng Server, đóng socket và giải phóng tài nguyên.
     */
    public void stop() throws IOException {
        running = false;
        if (serverSocket != null && !serverSocket.isClosed())
            serverSocket.close();
        // Gỡ bỏ observer để tránh rò rỉ bộ nhớ
        auctionService.removeAuctionObserver(auctionBroadcastObserver);
        executor.shutdownNow();
    }

    /**
     * Xử lý giao tiếp với một Client cụ thể. Đọc yêu cầu JSON theo dòng và phản hồi JSON.
     */
    private void handleClient(Socket clientSocket) {
        String clientAddress = clientSocket.getRemoteSocketAddress().toString();
        try (Socket socket = clientSocket;
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                PrintWriter writer = new PrintWriter(
                        new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {

            // Tạo session và thêm vào danh sách quản lý
            ClientSession session = new ClientSession(socket, writer);
            clientSessions.add(session);
            
            // Ngay khi kết nối, gửi snapshot dữ liệu đấu giá hiện tại cho client
            session.send(buildAuctionSyncMessage());

            String line;
            // Đọc tin nhắn từ client cho đến khi kết nối đóng
            while (running && !socket.isClosed() && (line = reader.readLine()) != null) {
                Message request;
                try {
                    request = Message.fromJson(line);
                } catch (Exception e) {
                    session.send(Message.failure(null, "Định dạng JSON không hợp lệ"));
                    continue;
                }

                if (request == null || request.getType() == null) {
                    session.send(Message.failure(null, "Yêu cầu không hợp lệ"));
                    continue;
                }

                // Xử lý yêu cầu và gửi phản hồi
                Message response = handleRequest(request);
                session.send(response);
            }
        } catch (Exception e) {
            if (running) {
                System.err.println("[SERVER] Lỗi xử lý Client [" + clientAddress + "]: " + e.getMessage());
            }
        } finally {
            // Xóa session khi client ngắt kết nối
            clientSessions.removeIf(session -> session.matches(clientSocket));
            System.out.println("[SERVER] Client đã ngắt kết nối: " + clientAddress);
        }
    }

    /**
     * Bộ điều phối (dispatcher) yêu cầu dựa trên loại Message.
     */
    private Message handleRequest(Message request) {
        try {
            return switch (request.getType()) {
                case PING -> Message.success(request, Map.of("status", "pong"));
                case LOGIN -> handleLogin(request);
                case REGISTER -> handleRegister(request);
                case GET_AUCTIONS -> handleGetAuctions(request);
                case PLACE_BID -> handlePlaceBid(request);
                case CREATE_AUCTION -> handleCreateAuction(request);
                case UPDATE_PROFILE -> handleUpdateProfile(request);
                case DELETE_ACCOUNT -> handleDeleteAccount(request);
                case DB_STATUS -> handleDatabaseStatus(request);
                case AUCTION_SYNC -> Message.failure(request, "Client không được phép gửi AUCTION_SYNC");
                case ERROR -> Message.failure(request, "Client gửi tin nhắn lỗi");
            };
        } catch (Exception e) {
            return Message.failure(request, e.getMessage());
        }
    }

    /**
     * Xử lý đăng nhập người dùng.
     */
    private Message handleLogin(Message request) {
        Map<String, Object> payload = request.getPayload();
        String username = stringValue(payload.get("username"));
        String password = stringValue(payload.get("password"));

        if (!com.auction.util.ValidationUtil.isUsernameValid(username)) {
            return Message.failure(request, "Tên đăng nhập không hợp lệ");
        }

        return authService.login(username, password)
                .map(user -> Message.success(request, userPayload(user)))
                .orElseGet(() -> Message.failure(request, "Tên đăng nhập hoặc mật khẩu không đúng"));
    }

    /**
     * Xử lý cập nhật thông tin cá nhân.
     */
    private Message handleUpdateProfile(Message request) {
        Map<String, Object> payload = request.getPayload();
        String username = stringValue(payload.get("username"));
        String fullName = stringValue(payload.get("fullName"));
        String email = stringValue(payload.get("email"));

        if (!ValidationUtil.isEmailValid(email)) {
            return Message.failure(request, "Email không hợp lệ");
        }

        boolean success = userDao.updateProfile(username, fullName, email);
        if (success) {
            User updatedUser = userDao.findByUsername(username);
            return Message.success(request, userPayload(updatedUser));
        } else {
            return Message.failure(request, "Cập nhật thông tin thất bại");
        }
    }

    /**
     * Xử lý yêu cầu xóa tài khoản.
     */
    private Message handleDeleteAccount(Message request) {
        Map<String, Object> payload = request.getPayload();
        String username = stringValue(payload.get("username"));

        boolean success = userDao.deleteAccount(username);
        if (success) {
            return Message.success(request, Map.of("success", true));
        } else {
            return Message.failure(request, "Xóa tài khoản thất bại");
        }
    }

    /**
     * Xử lý đăng ký tài khoản mới. Thực hiện kiểm tra tính hợp lệ của dữ liệu.
     */
    private Message handleRegister(Message request) {
        Map<String, Object> payload = request.getPayload();
        String username = stringValue(payload.get("username"));
        String fullName = stringValue(payload.get("fullName"));
        String email = stringValue(payload.get("email"));
        String password = stringValue(payload.get("password"));

        if (username == null || username.isBlank() || email == null || email.isBlank() || password == null
                || password.isBlank()) {
            return Message.failure(request, "Thiếu thông tin đăng ký bắt buộc");
        }

        if (!ValidationUtil.isUsernameValid(username)) {
            return Message.failure(request, "Tên đăng nhập không hợp lệ (3-16 ký tự, chỉ chữ và số)");
        }
        if (!ValidationUtil.isEmailValid(email)) {
            return Message.failure(request, "Định dạng email không hợp lệ");
        }
        if (!ValidationUtil.isPasswordValid(password)) {
            return Message.failure(request, "Mật khẩu quá yếu (cần ít nhất 8 ký tự, có chữ hoa, chữ thường và số)");
        }

        // Tạo người dùng mới (mật khẩu được lưu dưới dạng hash đơn giản cho mục đích minh họa)
        User user = new RegisteredUser(username, fullName == null || fullName.isBlank() ? username : fullName, email,
                String.valueOf(password.hashCode()));

        boolean created = authService.register(user);
        if (!created) {
            return Message.failure(request, "Đăng ký thất bại: Tên đăng nhập hoặc Email đã tồn tại");
        }

        return Message.success(request, userPayload(user));
    }

    /**
     * Trả về danh sách tất cả các cuộc đấu giá hiện tại.
     */
    private Message handleGetAuctions(Message request) {
        List<Map<String, Object>> auctions = auctionService.getAllAuctions().stream()
                .map(this::auctionPayload)
                .collect(Collectors.toList());
        return Message.success(request, Map.of("auctions", auctions));
    }

    /**
     * Xử lý yêu cầu đặt giá (bid). Kiểm tra điều kiện và thực hiện trả giá.
     */
    private Message handlePlaceBid(Message request) {
        Map<String, Object> payload = request.getPayload();
        String itemId = stringValue(payload.get("itemId"));
        String bidderName = stringValue(payload.get("bidderUsername"));
        String amountText = stringValue(payload.get("amount"));

        // Làm mới dữ liệu từ DB để tránh lỗi trạng thái cũ
        auctionService.refreshAuctions();
        Auction auction = auctionService.getAllAuctions().stream()
                .filter(current -> current.getItem().getId().equals(itemId))
                .findFirst()
                .orElse(null);

        if (auction == null)
            return Message.failure(request, "Không tìm thấy phiên đấu giá");
        if (bidderName == null || bidderName.isBlank())
            return Message.failure(request, "Tên người đặt giá là bắt buộc");

        User bidder = userDao.findByUsername(bidderName);
        if (bidder == null) {
            return Message.failure(request, "Người đặt giá chưa tồn tại trong cơ sở dữ liệu");
        }
        // Kiểm tra không cho phép tự trả giá trên sản phẩm của mình
        if (bidder.getId().equals(auction.getSeller().getId())) {
            return Message.failure(request, "Bạn không thể đấu giá sản phẩm do chính mình đăng bán");
        }

        boolean success = auctionService.placeBid(auction, bidder, new BigDecimal(amountText));

        if (!success)
            return Message.failure(request, "Đặt giá thất bại (có thể giá của bạn thấp hơn giá hiện tại)");

        return Message.success(request, auctionPayload(auction));
    }

    /**
     * Xử lý yêu cầu tạo phiên đấu giá mới kèm sản phẩm.
     */
    @SuppressWarnings("unchecked")
    private Message handleCreateAuction(Message request) {
        Map<String, Object> payload = request.getPayload();
        String itemType = stringValue(payload.get("itemType"));
        String name = stringValue(payload.get("name"));
        String description = stringValue(payload.get("description"));
        String startingPriceStr = stringValue(payload.get("startingPrice"));
        String bidStepStr = stringValue(payload.get("bidStep"));
        String startTimeStr = stringValue(payload.get("startTime"));
        String endTimeStr = stringValue(payload.get("endTime"));
        String sellerUsername = stringValue(payload.get("sellerUsername"));

        Map<String, Object> attributes = payload.get("attributes") instanceof Map<?, ?> rawMap
                ? (Map<String, Object>) rawMap
                : Map.of();

        if (sellerUsername == null || sellerUsername.isBlank()) {
            return Message.failure(request, "Thiếu thông tin người bán");
        }

        User seller = userDao.findByUsername(sellerUsername);
        if (seller == null) {
            return Message.failure(request, "Không tìm thấy người bán: " + sellerUsername);
        }

        try {
            BigDecimal startingPrice = new BigDecimal(startingPriceStr);
            BigDecimal bidStep = new BigDecimal(bidStepStr);
            LocalDateTime startTime = LocalDateTime.parse(startTimeStr, DATE_FORMATTER);
            LocalDateTime endTime = LocalDateTime.parse(endTimeStr, DATE_FORMATTER);

            boolean created = auctionService.createAuction(
                    itemType, name, description, startingPrice, bidStep,
                    startTime, endTime, seller, attributes);

            if (!created) {
                return Message.failure(request, "Không thể tạo phiên đấu giá. Kiểm tra kết nối DB.");
            }

            return Message.success(request, Map.of("created", true));
        } catch (Exception e) {
            return Message.failure(request, "Lỗi tạo phiên: " + e.getMessage());
        }
    }

    /**
     * Trả về trạng thái kết nối tới cơ sở dữ liệu.
     */
    private Message handleDatabaseStatus(Message request) {
        boolean available = authService.isDatabaseAvailable();
        return Message.success(request, Map.of(
                "available", available,
                "dbUrl", DBConnection.getConfiguredUrl(),
                "dbUser", DBConnection.getConfiguredUser()));
    }

    // ===== Các phương thức hỗ trợ xây dựng Payload (Dữ liệu gửi đi) =====

    /**
     * Chuyển đổi đối tượng User sang Map để gửi qua JSON.
     */
    private Map<String, Object> userPayload(User user) {
        return Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "fullName", user.getFullname(),
                "email", user.getEmail(),
                "role", user.getRole(),
                "balance", user.getBalance(),
                "active", user.isActive());
    }

    /**
     * Chuyển đổi đối tượng Auction và các thành phần liên quan sang Map.
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
        payload.put("highestBidder",
                auction.getHighestBidder() == null ? null : userPayload(auction.getHighestBidder()));
        payload.put("item", itemPayload(item));
        payload.put("bidHistory", auction.getBidHistory().stream().map(this::bidPayload).collect(Collectors.toList()));
        return payload;
    }

    /**
     * Chuyển đổi đối tượng Item sang Map.
     */
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
        payload.put("imageUrl", item.getImageUrl() == null ? "" : item.getImageUrl());
        payload.put("attributes", itemAttributesPayload(item));
        return payload;
    }

    /**
     * Chuyển đổi đối tượng BidTransaction sang Map.
     */
    private Map<String, Object> bidPayload(BidTransaction transaction) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", transaction.getId());
        payload.put("bidAmount", transaction.getBidAmount().toPlainString());
        payload.put("bidTime", transaction.getBidTime().format(DATE_FORMATTER));
        payload.put("bidder", userPayload(transaction.getBidder()));
        return payload;
    }

    /**
     * Xử lý các thuộc tính đặc thù của từng loại sản phẩm (Điện tử, Xe, Nghệ thuật).
     */
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
     * Hỗ trợ lấy giá trị chuỗi an toàn từ Object.
     */
    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }

    // ===== Cơ chế Broadcast (Phát sóng cập nhật) =====

    /**
     * Gửi trạng thái mới nhất của tất cả phiên đấu giá tới toàn bộ client đang kết nối.
     */
    private void broadcastAuctionSnapshot() {
        if (clientSessions.isEmpty())
            return;

        Message snapshot = buildAuctionSyncMessage();
        String jsonLine = snapshot.toJson();

        List<ClientSession> disconnected = new ArrayList<>();
        for (ClientSession session : clientSessions) {
            try {
                // Gửi dữ liệu thô để tối ưu hiệu suất (không cần gọi toJson nhiều lần)
                session.sendRaw(jsonLine);
            } catch (IOException e) {
                disconnected.add(session);
            }
        }
        // Loại bỏ các session lỗi (client đã ngắt kết nối âm thầm)
        clientSessions.removeAll(disconnected);
    }

    /**
     * Xây dựng tin nhắn đồng bộ hóa danh sách đấu giá.
     */
    private Message buildAuctionSyncMessage() {
        List<Map<String, Object>> auctions = auctionService.getAllAuctions().stream()
                .map(this::auctionPayload)
                .collect(Collectors.toList());
        return new Message(UUID.randomUUID().toString(), Message.Type.AUCTION_SYNC, Map.of("auctions", auctions), true,
                null);
    }

    /**
     * Điểm bắt đầu của ứng dụng Server.
     */
    public static void main(String[] args) throws IOException {
        int serverPort = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        new Server(serverPort).start();
    }

    // ===== Lớp nội bộ ClientSession (Quản lý kết nối JSON qua TCP) =====

    private static final class ClientSession {
        private final Socket socket;
        private final PrintWriter writer;

        private ClientSession(Socket socket, PrintWriter writer) {
            this.socket = socket;
            this.writer = writer;
        }

        /**
         * Gửi một đối tượng Message sau khi chuyển đổi sang JSON.
         */
        private synchronized void send(Message message) throws IOException {
            writer.println(message.toJson());
            if (writer.checkError())
                throw new IOException("Ghi dữ liệu thất bại");
        }

        /**
         * Gửi trực tiếp chuỗi JSON (đã có sẵn).
         */
        private synchronized void sendRaw(String jsonLine) throws IOException {
            writer.println(jsonLine);
            if (writer.checkError())
                throw new IOException("Ghi dữ liệu thô thất bại");
        }

        /**
         * Kiểm tra xem session này có ứng với socket cho trước không.
         */
        private boolean matches(Socket candidate) {
            return socket == candidate;
        }
    }
}
