package com.auction.service;

import com.auction.model.user.Admin;
import com.auction.model.user.User;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Service xử lý toàn bộ logic liên quan đến User.
 * Đã được cập nhật để sử dụng lớp User chung.
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

    private void initializeMockData() {
        String commonPasswordHash = String.valueOf("password".hashCode());

        // Sử dụng User cho tất cả người dùng bình thường
        users.add(new User("user1", "user1@example.com", commonPasswordHash));
        users.add(new User("user2", "user2@example.com", commonPasswordHash));
        users.add(new User("user3", "user3@example.com", commonPasswordHash));

        users.add(new Admin("admin", "admin@auction.com", commonPasswordHash, "SUPER_ADMIN"));
    }

    public Optional<User> login(String username, String password) {
        if (username == null || password == null) return Optional.empty();

        return users.stream()
                .filter(u -> u.getUsername().equalsIgnoreCase(username))
                .filter(u -> u.verifyPassword(password))
                .filter(User::isActive)
                .findFirst();
    }

    public boolean register(User user) {
        if (user == null) return false;
        if (isBlank(user.getUsername()) || isBlank(user.getEmail())) return false;

        boolean exists = users.stream().anyMatch(u ->
                u.getUsername().equalsIgnoreCase(user.getUsername()) ||
                u.getEmail().equalsIgnoreCase(user.getEmail())
        );

        if (exists) return false;
        return users.add(user);
    }

    public Optional<User> getUserById(String id) {
        if (id == null) return Optional.empty();
        return users.stream().filter(u -> u.getId().equals(id)).findFirst();
    }

    public Optional<User> getUserByUsername(String username) {
        if (username == null) return Optional.empty();
        return users.stream().filter(u -> u.getUsername().equalsIgnoreCase(username)).findFirst();
    }

    public List<User> getAllUsers() {
        return new ArrayList<>(users);
    }

    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }
}
