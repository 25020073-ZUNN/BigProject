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

public class NetworkService {

    private static final String DEFAULT_HOST = System.getProperty("auction.server.host", "127.0.0.1");
    private static final int DEFAULT_PORT = Integer.getInteger("auction.server.port", 5050);

    private static final NetworkService instance = new NetworkService();

    public static NetworkService getInstance() {
        return instance;
    }

    public boolean isServerReachable() {
        try {
            Message response = send(Message.Type.PING, Map.of());
            return response.isSuccess();
        } catch (IOException | ClassNotFoundException e) {
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

    private Message send(Message.Type type, Map<String, Object> payload) throws IOException, ClassNotFoundException {
        try (ServerConnection connection = new ServerConnection(DEFAULT_HOST, DEFAULT_PORT)) {
            return connection.send(type, payload);
        }
    }

    private void ensureSuccess(Message response) throws IOException {
        if (!response.isSuccess()) {
            throw new IOException(response.getMessage() == null ? "Server request failed" : response.getMessage());
        }
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
}
