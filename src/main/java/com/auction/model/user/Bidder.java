package com.auction.model.user;

public class Bidder extends User {
    public Bidder(String username, String email, String passwordHash) {
        super(username, email, passwordHash);
    }

    public Bidder(String username, String fullname, String email, String passwordHash) {
        super(username, fullname, email, passwordHash);
    }

    @Override
    public String getRole() {
        return "BIDDER";
    }
}
