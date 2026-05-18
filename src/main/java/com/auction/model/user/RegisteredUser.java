package com.auction.model.user;

public class RegisteredUser extends User {
    public RegisteredUser(String username, String email, String passwordHash) {
        super(username, email, passwordHash);
    }

    public RegisteredUser(String username, String fullname, String email, String passwordHash) {
        super(username, fullname, email, passwordHash);
    }

    @Override
    public String getRole() {
        return "USER";
    }
}
