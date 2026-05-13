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
 * Giao tiếp qua giao thức JSON trên TCP (newline-delimited JSON).
 */
public class Server {

    public static final int DEFAULT_PORT = 5050;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final int port;
    private final AuthService authService;
    private final AuctionService auctionService;
    private final UserDao userDao;
    private final ExecutorService executor;
    private final Set<ClientSession> clientSessions;
    private final AuctionObserver auctionBroadcastObserver;

    private volatile boolean running;
    private ServerSocket serverSocket;

    public Server() {
        this(DEFAULT_PORT);
    }

    public Server(int port) {
        this.port = port;
        this.authService = AuthService.getInstance();
        this.auctionService = AuctionService.getInstance();
        this.userDao = new UserDao();
        this.executor = Executors.newCachedThreadPool();
        this.clientSessions = ConcurrentHashMap.newKeySet();
        this.auctionBroadcastObserver = auctions -> broadcastAuctionSnapshot();
        this.auctionService.addAuctionObserver(auctionBroadcastObserver);
    }

    public void start() throws IOException {
        if (running)
            return;
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
                case DB_STATUS -> handleDatabaseStatus(request);
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

    private Message handleRegister(Message request) {
        Map<String, Object> payload = request.getPayload();
        String username = stringValue(payload.get("username"));
        String fullName = stringValue(payload.get("fullName"));
        String email = stringValue(payload.get("email"));
        String password = stringValue(payload.get("password"));
        String role = stringValue(payload.get("role"));

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

        if (role == null || role.isBlank()) {
            role = "BIDDER";
        }

        User user;
        if ("SELLER".equalsIgnoreCase(role)) {
            user = new Seller(username, fullName == null || fullName.isBlank() ? username : fullName, email,
                    String.valueOf(password.hashCode()));
        } else {
            user = new Bidder(username, fullName == null || fullName.isBlank() ? username : fullName, email,
                    String.valueOf(password.hashCode()));
        }

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
        boolean success = auctionService.placeBid(auction, bidder, new BigDecimal(amountText));

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

    private Message handleDatabaseStatus(Message request) {
        boolean available = authService.isDatabaseAvailable();
        return Message.success(request, Map.of(
                "available", available,
                "dbUrl", DBConnection.getConfiguredUrl(),
                "dbUser", DBConnection.getConfiguredUser()));
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
