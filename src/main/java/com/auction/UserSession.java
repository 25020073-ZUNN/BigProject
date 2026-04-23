package com.auction;

import com.auction.model.user.User;

public class UserSession {
    private static User loggedInUser;

    public static void login(User user) {
        loggedInUser = user;
    }

    public static void logout() {
        loggedInUser = null;
    }

    public static User getLoggedInUser() {
        return loggedInUser;
    }

    public static boolean isLoggedIn() {
        return loggedInUser != null;
    }
}
