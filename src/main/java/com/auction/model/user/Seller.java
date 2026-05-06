package com.auction.model.user;

public class Seller extends User {
    public Seller(String username, String email, String passwordHash) {
        super(username, email, passwordHash);
    }

    public Seller(String username, String fullname, String email, String passwordHash) {
        super(username, fullname, email, passwordHash);
    }

    @Override
    public String getRole() {
        return "SELLER";
    }
}
