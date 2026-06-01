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
import com.auction.service.ImageStorageService;
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
import java.util.Base64;
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
 * Giao tiếp qua giao thức JSON trên TCP (newline-delimited JSON).
 */
public class Server {

    public static final int DEFAULT_PORT = 5050;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final int port;
    private final AuthService authService;
    private final AuctionService auctionService;
    private final ImageStorageService imageStorageService;
    private final UserDao userDao;
    private final ExecutorService executor;
    private final Set<ClientSession> clientSessions;
    private final AuctionObserver auctionBroadcastObserver;
    private final Map<String, ResetToken> resetTokens = new ConcurrentHashMap<>();

    public static class ResetToken {
        private final String email;
        private final String token;
        private final LocalDateTime expiryTime;

        public ResetToken(String email, String token, LocalDateTime expiryTime) {
            this.email = email;
            this.token = token;
            this.expiryTime = expiryTime;
        }

        public String getEmail() { return email; }
        public String getToken() { return token; }
        public LocalDateTime getExpiryTime() { return expiryTime; }
    }

    private volatile boolean running;
    private ServerSocket serverSocket;

    public Server() {
        this(DEFAULT_PORT);
    }

    public Server(int port) {
        this.port = port;
        this.authService = AuthService.getInstance();
        this.auctionService = AuctionService.getInstance();
        this.imageStorageService = new ImageStorageService();
        this.userDao = new UserDao();
        this.executor = Executors.newCachedThreadPool();
        this.clientSessions = ConcurrentHashMap.newKeySet();
        this.auctionBroadcastObserver = auctions -> broadcastAuctionSnapshot();
        this.auctionService.addAuctionObserver(auctionBroadcastObserver);
    }

    public void start() throws IOException {
        if (running)
            return;
        imageStorageService.start();
        serverSocket = new ServerSocket(port);
        running = true;
        System.out.println("Auction server started on port " + port + " (JSON protocol)");

        while (running) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("[SERVER] Client mới kết nối: " + clientSocket.getRemoteSocketAddress());
            executor.submit(() -> handleClient(clientSocket));
        }
    }

    public void stop() throws IOException {
        running = false;
        if (serverSocket != null && !serverSocket.isClosed())
            serverSocket.close();
        imageStorageService.stop();
        auctionService.removeAuctionObserver(auctionBroadcastObserver);
        executor.shutdownNow();
    }

    /**
     * Xử lý giao tiếp với một Client. Đọc/ghi JSON qua TCP.
     */
    private void handleClient(Socket clientSocket) {
        String clientAddress = clientSocket.getRemoteSocketAddress().toString();
        try (Socket socket = clientSocket;
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                PrintWriter writer = new PrintWriter(
                        new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {

            ClientSession session = new ClientSession(socket, writer);
            clientSessions.add(session);
            session.send(buildAuctionSyncMessage());

            String line;
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

                Message response = handleRequest(request);
                session.send(response);
            }
        } catch (Exception e) {
            if (running) {
                System.err.println("[SERVER] Lỗi xử lý Client [" + clientAddress + "]: " + e.getMessage());
            }
        } finally {
            clientSessions.removeIf(session -> session.matches(clientSocket));
            System.out.println("[SERVER] Client đã ngắt kết nối: " + clientAddress);
        }
    }

    /**
     * Bộ điều phối yêu cầu.
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
                case GET_USERS -> handleGetUsers(request);
                case SET_USER_ACTIVE -> handleSetUserActive(request);
                case DELETE_AUCTION -> handleDeleteAuction(request);
                case UPDATE_AUCTION -> handleUpdateAuction(request);
                case SELLER_DELETE_AUCTION -> handleSellerDeleteAuction(request);
                case UPDATE_PROFILE -> handleUpdateProfile(request);
                case DELETE_ACCOUNT -> handleDeleteAccount(request);
                case GET_CURRENT_USER -> handleGetCurrentUser(request);
                case DB_STATUS -> handleDatabaseStatus(request);
                case REQUEST_PASSWORD_RESET -> handleRequestPasswordReset(request);
                case RESET_PASSWORD -> handleResetPassword(request);
                case AUCTION_SYNC -> Message.failure(request, "Client không được phép gửi AUCTION_SYNC");
                case ERROR -> Message.failure(request, "Client gửi tin nhắn lỗi");
            };
        } catch (Exception e) {
            return Message.failure(request, e.getMessage());
        }
    }

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

    private Message handleGetCurrentUser(Message request) {
        Map<String, Object> payload = request.getPayload();
        String username = stringValue(payload.get("username"));

        if (username == null || username.isBlank()) {
            return Message.failure(request, "Thiếu tên đăng nhập");
        }

        auctionService.refreshAuctions();
        User user = userDao.findByUsername(username);
        if (user == null || !user.isActive()) {
            return Message.failure(request, "Không tìm thấy tài khoản đang hoạt động");
        }
        return Message.success(request, userPayload(user));
    }

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

        User user = new RegisteredUser(username, fullName == null || fullName.isBlank() ? username : fullName, email,
                org.mindrot.jbcrypt.BCrypt.hashpw(password, org.mindrot.jbcrypt.BCrypt.gensalt()));

        boolean created = authService.register(user);
        if (!created) {
            return Message.failure(request, "Đăng ký thất bại: Tên đăng nhập hoặc Email đã tồn tại");
        }

        return Message.success(request, userPayload(user));
    }

    private Message handleGetAuctions(Message request) {
        List<Map<String, Object>> auctions = auctionService.getAllAuctions().stream()
                .map(this::auctionPayload)
                .collect(Collectors.toList());
        return Message.success(request, Map.of("auctions", auctions));
    }

    private Message handlePlaceBid(Message request) {
        Map<String, Object> payload = request.getPayload();
        String itemId = stringValue(payload.get("itemId"));
        String bidderName = stringValue(payload.get("bidderUsername"));
        String amountText = stringValue(payload.get("amount"));

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
        if (bidder.getId().equals(auction.getSeller().getId())) {
            return Message.failure(request, "Bạn không thể đấu giá sản phẩm do chính mình đăng bán");
        }

        BigDecimal bidAmount = new BigDecimal(amountText);
        BigDecimal bidderBalance = BigDecimal.valueOf(bidder.getBalance());
        if (bidderBalance.compareTo(bidAmount) < 0) {
            return Message.failure(request, "Số dư tài khoản không đủ để thực hiện đặt giá (Số dư hiện tại: " + bidder.getBalance() + " VNĐ)");
        }

        boolean success = auctionService.placeBid(auction, bidder, bidAmount);

        if (!success)
            return Message.failure(request, "Đặt giá thất bại (có thể giá của bạn thấp hơn giá hiện tại)");

        return Message.success(request, auctionPayload(auction));
    }

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
                ? new HashMap<>((Map<String, Object>) rawMap)
                : new HashMap<>();

        if (sellerUsername == null || sellerUsername.isBlank()) {
            return Message.failure(request, "Thiếu thông tin người bán");
        }

        User seller = userDao.findByUsername(sellerUsername);
        if (seller == null) {
            return Message.failure(request, "Không tìm thấy người bán: " + sellerUsername);
        }

        try {
            attachStoredImageUrl(attributes);

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

    private Message handleGetUsers(Message request) {
        Map<String, Object> payload = request.getPayload();
        String adminUsername = stringValue(payload.get("adminUsername"));
        User admin = userDao.findByUsername(adminUsername);
        if (admin == null || !"ADMIN".equalsIgnoreCase(admin.getRole())) {
            return Message.failure(request, "Quyền truy cập bị từ chối");
        }
        List<Map<String, Object>> users = userDao.findAll().stream()
                .map(this::userPayload)
                .collect(Collectors.toList());
        return Message.success(request, Map.of("users", users));
    }

    private void attachStoredImageUrl(Map<String, Object> attributes) throws IOException {
        String imageBase64 = stringValue(attributes.get("imageBase64"));
        if (imageBase64 == null || imageBase64.isBlank()) {
            return;
        }

        byte[] imageBytes = Base64.getDecoder().decode(imageBase64);
        String originalFileName = stringValue(attributes.get("imageFileName"));
        String imageUrl = imageStorageService.storeImage(imageBytes, originalFileName);
        attributes.put("imageUrl", imageUrl);
        attributes.remove("imageBase64");
    }

    private Message handleSetUserActive(Message request) {
        Map<String, Object> payload = request.getPayload();
        String adminUsername = stringValue(payload.get("adminUsername"));
        String targetUsername = stringValue(payload.get("targetUsername"));
        boolean active = Boolean.parseBoolean(stringValue(payload.get("active")));
        User admin = userDao.findByUsername(adminUsername);
        if (admin == null || !"ADMIN".equalsIgnoreCase(admin.getRole())) {
            return Message.failure(request, "Quyền truy cập bị từ chối");
        }
        if (admin.getUsername().equals(targetUsername) && !active) {
            return Message.failure(request, "Admin không thể tự khóa tài khoản đang đăng nhập");
        }
        boolean updated = userDao.setUserActive(targetUsername, active);
        if (!updated) {
            return Message.failure(request, "Không tìm thấy tài khoản cần cập nhật");
        }
        return Message.success(request, Map.of("success", true));
    }

    private Message handleDeleteAuction(Message request) {
        Map<String, Object> payload = request.getPayload();
        String adminUsername = stringValue(payload.get("adminUsername"));
        String auctionId = stringValue(payload.get("auctionId"));
        User admin = userDao.findByUsername(adminUsername);
        if (admin == null || !"ADMIN".equalsIgnoreCase(admin.getRole())) {
            return Message.failure(request, "Quyền truy cập bị từ chối");
        }
        boolean success = auctionService.deleteAuction(auctionId);
        if (!success) {
            return Message.failure(request, "Xóa phiên đấu giá thất bại");
        }
        return Message.success(request, Map.of("deleted", true));
    }

    @SuppressWarnings("unchecked")
    private Message handleUpdateAuction(Message request) {
        Map<String, Object> payload = request.getPayload();
        String auctionId = stringValue(payload.get("auctionId"));
        String sellerUsername = stringValue(payload.get("sellerUsername"));
        String itemType = stringValue(payload.get("itemType"));
        String name = stringValue(payload.get("name"));
        String description = stringValue(payload.get("description"));
        String startingPriceStr = stringValue(payload.get("startingPrice"));
        String bidStepStr = stringValue(payload.get("bidStep"));
        String startTimeStr = stringValue(payload.get("startTime"));
        String endTimeStr = stringValue(payload.get("endTime"));

        Map<String, Object> attributes = payload.get("attributes") instanceof Map<?, ?> rawMap
                ? new HashMap<>((Map<String, Object>) rawMap)
                : new HashMap<>();

        if (sellerUsername == null || sellerUsername.isBlank()) {
            return Message.failure(request, "Thiếu thông tin người bán");
        }
        if (auctionId == null || auctionId.isBlank()) {
            return Message.failure(request, "Thiếu mã phiên đấu giá");
        }

        User seller = userDao.findByUsername(sellerUsername);
        if (seller == null) {
            return Message.failure(request, "Không tìm thấy người bán: " + sellerUsername);
        }

        try {
            attachStoredImageUrl(attributes);

            BigDecimal startingPrice = new BigDecimal(startingPriceStr);
            BigDecimal bidStep = new BigDecimal(bidStepStr);
            LocalDateTime startTime = LocalDateTime.parse(startTimeStr, DATE_FORMATTER);
            LocalDateTime endTime = LocalDateTime.parse(endTimeStr, DATE_FORMATTER);

            boolean updated = auctionService.updateAuction(
                    auctionId, seller.getId(), itemType, name, description,
                    startingPrice, bidStep, startTime, endTime, attributes);

            if (!updated) {
                return Message.failure(request, "Không thể cập nhật phiên đấu giá.");
            }

            return Message.success(request, Map.of("updated", true));
        } catch (IllegalStateException e) {
            return Message.failure(request, e.getMessage());
        } catch (IllegalArgumentException e) {
            return Message.failure(request, e.getMessage());
        } catch (Exception e) {
            return Message.failure(request, "Lỗi cập nhật phiên: " + e.getMessage());
        }
    }

    private Message handleSellerDeleteAuction(Message request) {
        Map<String, Object> payload = request.getPayload();
        String sellerUsername = stringValue(payload.get("sellerUsername"));
        String auctionId = stringValue(payload.get("auctionId"));

        if (sellerUsername == null || sellerUsername.isBlank()) {
            return Message.failure(request, "Thiếu thông tin người bán");
        }
        if (auctionId == null || auctionId.isBlank()) {
            return Message.failure(request, "Thiếu mã phiên đấu giá");
        }

        User seller = userDao.findByUsername(sellerUsername);
        if (seller == null) {
            return Message.failure(request, "Không tìm thấy người bán: " + sellerUsername);
        }

        try {
            boolean success = auctionService.sellerDeleteAuction(auctionId, seller.getId());
            if (!success) {
                return Message.failure(request, "Xóa phiên đấu giá thất bại");
            }
            return Message.success(request, Map.of("deleted", true));
        } catch (IllegalStateException e) {
            return Message.failure(request, e.getMessage());
        } catch (IllegalArgumentException e) {
            return Message.failure(request, e.getMessage());
        } catch (Exception e) {
            return Message.failure(request, "Lỗi xóa phiên: " + e.getMessage());
        }
    }

    private Message handleDatabaseStatus(Message request) {
        boolean available = authService.isDatabaseAvailable();
        return Message.success(request, Map.of(
                "available", available,
                "dbUrl", DBConnection.getConfiguredUrl(),
                "dbUser", DBConnection.getConfiguredUser()));
    }

    private Message handleRequestPasswordReset(Message request) {
        Map<String, Object> payload = request.getPayload();
        String usernameOrEmail = stringValue(payload.get("emailOrUsername"));
        if (usernameOrEmail == null || usernameOrEmail.isBlank()) {
            return Message.failure(request, "Thiếu thông tin tên đăng nhập hoặc email");
        }
        User user = userDao.findByUsername(usernameOrEmail);
        if (user == null) {
            user = userDao.findByEmail(usernameOrEmail);
        }
        if (user == null) {
            return Message.failure(request, "Không tìm thấy tài khoản tương ứng");
        }
        String toEmail = user.getEmail();
        if (toEmail == null || toEmail.isBlank()) {
            return Message.failure(request, "Tài khoản không có email hợp lệ");
        }

        String token = String.format("%06d", new java.util.Random().nextInt(1000000));
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(5);
        resetTokens.put(toEmail, new ResetToken(toEmail, token, expiry));

        sendEmail(toEmail, token);
        return Message.success(request, Map.of("email", toEmail));
    }

    private Message handleResetPassword(Message request) {
        Map<String, Object> payload = request.getPayload();
        String usernameOrEmail = stringValue(payload.get("emailOrUsername"));
        String token = stringValue(payload.get("token"));
        String newPassword = stringValue(payload.get("newPassword"));

        if (usernameOrEmail == null || usernameOrEmail.isBlank() || token == null || token.isBlank() || newPassword == null || newPassword.isBlank()) {
            return Message.failure(request, "Thiếu thông tin xác nhận");
        }

        User user = userDao.findByUsername(usernameOrEmail);
        if (user == null) {
            user = userDao.findByEmail(usernameOrEmail);
        }
        if (user == null) {
            return Message.failure(request, "Không tìm thấy tài khoản tương ứng");
        }
        String email = user.getEmail();

        ResetToken storedToken = resetTokens.get(email);
        if (storedToken == null || !storedToken.getToken().equals(token)) {
            return Message.failure(request, "Mã xác nhận không đúng");
        }
        if (LocalDateTime.now().isAfter(storedToken.getExpiryTime())) {
            resetTokens.remove(email);
            return Message.failure(request, "Mã xác nhận đã hết hạn (chỉ có hiệu lực trong 5 phút)");
        }

        String newPasswordHash = org.mindrot.jbcrypt.BCrypt.hashpw(newPassword, org.mindrot.jbcrypt.BCrypt.gensalt());
        boolean success = userDao.updatePassword(email, newPasswordHash);
        resetTokens.remove(email);

        if (success) {
            return Message.success(request, Map.of("success", true));
        } else {
            return Message.failure(request, "Không thể cập nhật mật khẩu mới");
        }
    }

    private void sendEmail(String toEmail, String token) {
        // Luôn in ra console để developer/tester có thể lấy mã ngay (kể cả khi SMTP fail)
        System.out.println("[EMAIL] GỬI MÃ XÁC NHẬN ĐẾN: " + toEmail + " | MÃ OTP: " + token + " (Hết hạn sau 5 phút)");

        new Thread(() -> {
            try {
                // Đọc cấu hình SMTP từ file smtp.properties trên classpath
                java.util.Properties smtpConfig = new java.util.Properties();
                try (var is = getClass().getClassLoader().getResourceAsStream("smtp.properties")) {
                    if (is == null) {
                        System.err.println("[EMAIL] Không tìm thấy smtp.properties trên classpath!");
                        return;
                    }
                    smtpConfig.load(is);
                }

                final String smtpUser = smtpConfig.getProperty("smtp.user");
                final String smtpPassword = smtpConfig.getProperty("smtp.password");

                java.util.Properties props = new java.util.Properties();
                props.put("mail.smtp.host", smtpConfig.getProperty("smtp.host", "smtp.gmail.com"));
                props.put("mail.smtp.port", smtpConfig.getProperty("smtp.port", "587"));
                props.put("mail.smtp.auth", smtpConfig.getProperty("smtp.auth", "true"));
                props.put("mail.smtp.starttls.enable", smtpConfig.getProperty("smtp.starttls", "true"));

                javax.mail.Session session = javax.mail.Session.getInstance(props,
                        new javax.mail.Authenticator() {
                            protected javax.mail.PasswordAuthentication getPasswordAuthentication() {
                                return new javax.mail.PasswordAuthentication(smtpUser, smtpPassword);
                            }
                        });

                javax.mail.Message message = new javax.mail.internet.MimeMessage(session);
                message.setFrom(new javax.mail.internet.InternetAddress(smtpUser));
                message.setRecipients(
                        javax.mail.Message.RecipientType.TO,
                        javax.mail.internet.InternetAddress.parse(toEmail));
                message.setSubject("Team 6 - UET - Reset Password");
                message.setText(
                        "Token reset password của bạn:\n\n" + token + "\n\n" +
                        "Mã này có hiệu lực trong 5 phút.\n" +
                        "Nếu bạn không yêu cầu đặt lại mật khẩu, hãy bỏ qua email này.");

                javax.mail.Transport.send(message);
                System.out.println("[EMAIL] Đã gửi mail thành công qua SMTP tới: " + toEmail);
            } catch (Exception e) {
                System.err.println("[EMAIL] Lỗi gửi mail qua SMTP: " + e.getMessage());
            }
        }, "email-sender").start();
    }

    // ===== Payload builders =====

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

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }

    // ===== Broadcast =====

    private void broadcastAuctionSnapshot() {
        if (clientSessions.isEmpty())
            return;

        Message snapshot = buildAuctionSyncMessage();
        String jsonLine = snapshot.toJson();

        List<ClientSession> disconnected = new ArrayList<>();
        for (ClientSession session : clientSessions) {
            try {
                session.sendRaw(jsonLine);
            } catch (IOException e) {
                disconnected.add(session);
            }
        }
        clientSessions.removeAll(disconnected);
    }

    private Message buildAuctionSyncMessage() {
        List<Map<String, Object>> auctions = auctionService.getAllAuctions().stream()
                .map(this::auctionPayload)
                .collect(Collectors.toList());
        return new Message(UUID.randomUUID().toString(), Message.Type.AUCTION_SYNC, Map.of("auctions", auctions), true,
                null);
    }

    public static void main(String[] args) throws IOException {
        int serverPort = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        new Server(serverPort).start();
    }

    // ===== Client Session (JSON over TCP) =====

    private static final class ClientSession {
        private final Socket socket;
        private final PrintWriter writer;

        private ClientSession(Socket socket, PrintWriter writer) {
            this.socket = socket;
            this.writer = writer;
        }

        private synchronized void send(Message message) throws IOException {
            writer.println(message.toJson());
            if (writer.checkError())
                throw new IOException("Write failed");
        }

        private synchronized void sendRaw(String jsonLine) throws IOException {
            writer.println(jsonLine);
            if (writer.checkError())
                throw new IOException("Write failed");
        }

        private boolean matches(Socket candidate) {
            return socket == candidate;
        }
    }
}
/*Server.java là trung tâm điều phối của hệ thống đấu giá.
Nó nhận request JSON từ client qua TCP, xác thực dữ liệu, gọi các Service xử lý nghiệp vụ, truy cập database thông qua DAO và trả response.
Server hỗ trợ đa client bằng thread pool, cập nhật realtime bằng Observer và broadcast, đồng thời tích hợp xác thực, reset mật khẩu và quản lý ảnh.*/