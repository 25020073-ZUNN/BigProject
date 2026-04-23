package com.auction.network;

import com.auction.config.DBConnection;
import com.auction.model.Auction;
import com.auction.model.item.Item;
import com.auction.model.user.Bidder;
import com.auction.model.user.User;
import com.auction.service.AuctionService;
import com.auction.service.AuthService;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class Server {

    public static final int DEFAULT_PORT = 5050;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final int port;
    private final AuthService authService;
    private final AuctionService auctionService;
    private final ExecutorService executor;

    private volatile boolean running;
    private ServerSocket serverSocket;

    public Server() {
        this(DEFAULT_PORT);
    }

    public Server(int port) {
        this.port = port;
        this.authService = AuthService.getInstance();
        this.auctionService = AuctionService.getInstance();
        this.executor = Executors.newCachedThreadPool();
    }

    public void start() throws IOException {
        if (running) {
            return;
        }

        serverSocket = new ServerSocket(port);
        running = true;
        System.out.println("Auction server started on port " + port);

        while (running) {
            Socket clientSocket = serverSocket.accept();
            executor.submit(() -> handleClient(clientSocket));
        }
    }

    public void stop() throws IOException {
        running = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }
        executor.shutdownNow();
    }

    private void handleClient(Socket clientSocket) {
        try (Socket socket = clientSocket;
             ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream())) {

            while (running && !socket.isClosed()) {
                Object rawRequest = inputStream.readObject();
                if (!(rawRequest instanceof Message request)) {
                    outputStream.writeObject(Message.failure(null, "Invalid request format"));
                    outputStream.flush();
                    continue;
                }

                Message response = handleRequest(request);
                outputStream.writeObject(response);
                outputStream.flush();
            }
        } catch (EOFException ignored) {
        } catch (Exception e) {
            System.err.println("Client handling failed: " + e.getMessage());
        }
    }

    private Message handleRequest(Message request) {
        try {
            return switch (request.getType()) {
                case PING -> Message.success(request, Map.of("status", "pong"));
                case LOGIN -> handleLogin(request);
                case REGISTER -> handleRegister(request);
                case GET_AUCTIONS -> handleGetAuctions(request);
                case PLACE_BID -> handlePlaceBid(request);
                case DB_STATUS -> handleDatabaseStatus(request);
                case ERROR -> Message.failure(request, "Client sent error message");
            };
        } catch (Exception e) {
            return Message.failure(request, e.getMessage());
        }
    }

    private Message handleLogin(Message request) {
        Map<String, Object> payload = request.getPayload();
        String username = stringValue(payload.get("username"));
        String password = stringValue(payload.get("password"));

        return authService.login(username, password)
                .map(user -> Message.success(request, userPayload(user)))
                .orElseGet(() -> Message.failure(request, "Invalid username/email or password"));
    }

    private Message handleRegister(Message request) {
        Map<String, Object> payload = request.getPayload();
        String username = stringValue(payload.get("username"));
        String fullName = stringValue(payload.get("fullName"));
        String email = stringValue(payload.get("email"));
        String password = stringValue(payload.get("password"));
        String role = stringValue(payload.get("role"));

        if (username == null || username.isBlank()
                || email == null || email.isBlank()
                || password == null || password.isBlank()) {
            return Message.failure(request, "Missing required registration fields");
        }

        User user = "SELLER".equalsIgnoreCase(role)
                ? new com.auction.model.user.Seller(username, email, String.valueOf(password.hashCode()))
                : new Bidder(username, email, String.valueOf(password.hashCode()));
        user.setFullname(fullName == null || fullName.isBlank() ? username : fullName);

        boolean created = authService.register(user);
        if (!created) {
            return Message.failure(request, "Register failed: username or email already exists");
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

        if (auction == null) {
            return Message.failure(request, "Auction not found");
        }

        if (bidderName == null || bidderName.isBlank()) {
            return Message.failure(request, "Bidder username is required");
        }

        Bidder bidder = new Bidder(bidderName, bidderName + "@example.com", String.valueOf("password".hashCode()));
        boolean success = auctionService.placeBid(auction, bidder, new BigDecimal(amountText));
        if (!success) {
            return Message.failure(request, "Bid failed");
        }

        return Message.success(request, auctionPayload(auction));
    }

    private Message handleDatabaseStatus(Message request) {
        boolean available = authService.isDatabaseAvailable();
        return Message.success(request, Map.of(
                "available", available,
                "dbUrl", DBConnection.getConfiguredUrl(),
                "dbUser", DBConnection.getConfiguredUser()
        ));
    }

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

    private Map<String, Object> auctionPayload(Auction auction) {
        Item item = auction.getItem();
        Map<String, Object> payload = new HashMap<>();
        payload.put("auctionId", auction.getId());
        payload.put("itemId", item.getId());
        payload.put("itemName", item.getName());
        payload.put("category", item.getCategory());
        payload.put("sellerId", auction.getSeller().getId());
        payload.put("sellerName", auction.getSeller().getUsername());
        payload.put("startingPrice", auction.getStartingPrice().toPlainString());
        payload.put("currentPrice", auction.getCurrentPrice().toPlainString());
        payload.put("active", auction.isActive());
        payload.put("finished", auction.isFinished());
        payload.put("endTime", item.getEndTime().format(DATE_FORMATTER));
        return payload;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }

    public static void main(String[] args) throws IOException {
        int serverPort = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        new Server(serverPort).start();
    }
}
