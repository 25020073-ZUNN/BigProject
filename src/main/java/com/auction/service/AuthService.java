package com.auction.service;

import com.auction.dao.UserDao;
import com.auction.model.user.User;

import java.util.Optional;

public class AuthService {
    private static final AuthService instance = new AuthService();

    private final UserDao userDao;
    private final UserService userService;

    private AuthService() {
        this.userDao = new UserDao();
        this.userService = UserService.getInstance();
    }

    public static AuthService getInstance() {
        return instance;
    }

    public Optional<User> login(String username, String password) {
        if (username == null || password == null) {
            return Optional.empty();
        }

        if (userDao.isDatabaseAvailable()) {
            User user = userDao.login(username, password);
            if (user != null) {
                return Optional.of(user);
            }
        }

        return userService.login(username, password);
    }

    public boolean register(User user) {
        if (user == null) {
            return false;
        }

        if (userDao.isDatabaseAvailable()) {
            return userDao.register(user);
        }

        return userService.register(user);
    }
}
