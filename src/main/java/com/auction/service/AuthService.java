package com.auction.service;

import com.auction.dao.UserDao;
import com.auction.model.user.User;

import java.util.Optional;

public class AuthService {
    private static final AuthService instance = new AuthService();

    private final UserDao userDao;

    private AuthService() {
        this.userDao = new UserDao();
    }

    public static AuthService getInstance() {
        return instance;
    }

    public Optional<User> login(String username, String password) {
        if (username == null || password == null) {
            return Optional.empty();
        }

        ensureDatabaseAvailable();

        User user = userDao.login(username, password);
        if (user != null) {
            return Optional.of(user);
        }

        return Optional.empty();
    }

    public boolean register(User user) {
        if (user == null) {
            return false;
        }

        ensureDatabaseAvailable();
        return userDao.register(user);
    }

    public boolean isDatabaseAvailable() {
        return userDao.isDatabaseAvailable();
    }

    private void ensureDatabaseAvailable() {
        if (!userDao.isDatabaseAvailable()) {
            throw new IllegalStateException("Database is unavailable. Please check DB_URL, DB_USER, DB_PASSWORD.");
        }
    }
}
