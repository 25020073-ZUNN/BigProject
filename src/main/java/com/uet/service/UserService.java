package com.uet.service;

import com.uet.bidding.model.user.Admin;
import com.uet.bidding.model.user.Bidder;
import com.uet.bidding.model.user.Seller;
import com.uet.bidding.model.user.User;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Service xử lý toàn bộ logic liên quan đến User
 */
public class UserService {

    private final List<User> users = new CopyOnWriteArrayList<>();

    private static final UserService instance = new UserService();

    public static UserService getInstance() {
        return instance;
    }

    private UserService() {
        initializeMockData();
    }

    /**
     * Khởi tạo dữ liệu mẫu (dùng raw password, User sẽ tự hash)
     */
    private void initializeMockData() {
        String commonPassword = "password";

        users.add(new Bidder("bidder1", "bidder1@example.com", commonPassword));
        users.add(new Bidder("bidder2", "bidder2@example.com", commonPassword));

        users.add(new Seller("seller1", "seller1@example.com", commonPassword));
        users.add(new Seller("seller2", "seller2@example.com", commonPassword));

        users.add(new Admin("admin", "admin@auction.com", commonPassword, "SUPER_ADMIN"));
    }

    /**
     * Đăng nhập
     */
    public Optional<User> login(String username, String password) {

        if (username == null || password == null) {
            return Optional.empty();
        }

        return users.stream()
                .filter(u -> u.getUsername().equalsIgnoreCase(username))
                .filter(u -> u.verifyPassword(password)) // ✅ KHÔNG hash ở đây
                .filter(User::isActive)
                .findFirst();
    }

    /**
     * Đăng ký
     */
    public boolean register(User user) {

        if (user == null) return false;

        if (isBlank(user.getUsername()) || isBlank(user.getEmail())) {
            return false;
        }

        boolean exists = users.stream().anyMatch(u ->
                u.getUsername().equalsIgnoreCase(user.getUsername()) ||
                        u.getEmail().equalsIgnoreCase(user.getEmail())
        );

        if (exists) return false;

        return users.add(user); // password đã được hash trong User
    }

    public Optional<User> getUserById(String id) {
        if (id == null) return Optional.empty();

        return users.stream()
                .filter(u -> u.getId().equals(id))
                .findFirst();
    }

    public Optional<User> getUserByUsername(String username) {
        if (username == null) return Optional.empty();

        return users.stream()
                .filter(u -> u.getUsername().equalsIgnoreCase(username))
                .findFirst();
    }

    public List<User> getAllUsers() {
        return new ArrayList<>(users);
    }

    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }
}