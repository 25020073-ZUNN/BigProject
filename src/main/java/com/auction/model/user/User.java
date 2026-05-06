package com.auction.model.user;

import com.auction.model.Entity;
import java.util.ArrayList;
import java.util.List;

/**
 * Lớp User đại diện cho người dùng bình thường trong hệ thống.
 * Một User hiện tại có thể đóng cả hai vai trò: Người mua (Bidder) và Người bán (Seller).
 */
public abstract class User extends Entity {

    // --- Thuộc tính cơ bản ---
    private String username;
    private String fullname;
    private String email;
    private String passwordHash;
    private long balance;
    private boolean active;

    // --- Thuộc tính của Người đấu giá (Bidder) ---
    private List<String> joinedAuctionIds = new ArrayList<>();
    private List<String> wonAuctionIds = new ArrayList<>();
    private int totalBids = 0;

    // --- Thuộc tính của Người bán (Seller) ---
    private List<String> listedItemIds = new ArrayList<>();
    private double rating = 0.0;
    private int totalRatings = 0;

    // CONSTRUCTOR
    public User(String username, String email, String passwordHash) {
        this(username, username, email, passwordHash);
    }

    public User(String username, String fullname, String email, String passwordHash) {
        super();
        this.username = username;
        this.fullname = fullname;
        this.email = email;
        this.passwordHash = passwordHash;
        this.balance = 0L;
        this.active = true;
    }

    // GETTER & SETTER
    public String getUsername() { return username; }
    public String getFullname() { return fullname; }
    public void setFullname(String fullname) { this.fullname = fullname; }
    public String getEmail() { return email; }
    public long getBalance() { return balance; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getPasswordHash() { return passwordHash; }

    /**
     * Trả về vai trò. Với lớp này mặc định là USER.
     */
    public abstract String getRole();

    public void setEmail(String email) {
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("EMAIL KHÔNG HỢP LỆ: " + email);
        }
        this.email = email;
    }

    // --- Quản lý tài chính ---
    public void deposit(long amount) {
        if (amount <= 0) throw new IllegalArgumentException("SỐ TIỀN NẠP PHẢI DƯƠNG");
        this.balance += amount;
    }

    public void withdraw(long amount) {
        if (amount <= 0) throw new IllegalArgumentException("SỐ TIỀN RÚT PHẢI DƯƠNG");
        if (amount > this.balance) throw new InsufficientFundsException("Số dư không đủ");
        this.balance -= amount;
    }

    public boolean verifyPassword(String rawPassword) {
        return this.passwordHash.equals(String.valueOf(rawPassword.hashCode()));
    }

    // --- Tính năng của Người đấu giá ---
    public void joinAuction(String auctionId) {
        if (auctionId != null && !joinedAuctionIds.contains(auctionId)) {
            joinedAuctionIds.add(auctionId);
        }
    }

    public void addWonAuction(String auctionId) {
        if (auctionId != null && !wonAuctionIds.contains(auctionId)) {
            wonAuctionIds.add(auctionId);
        }
    }

    public void increaseTotalBids() { this.totalBids++; }
    public List<String> getJoinedAuctionIds() { return new ArrayList<>(joinedAuctionIds); }
    public List<String> getWonAuctionIds() { return new ArrayList<>(wonAuctionIds); }

    // --- Tính năng của Người bán ---
    public void addListedItem(String itemId) {
        if (itemId != null) listedItemIds.add(itemId);
    }

    public List<String> getListedItemIds() { return new ArrayList<>(listedItemIds); }

    public void addRating(double newRating) {
        if (newRating < 0.0 || newRating > 5.0) throw new IllegalArgumentException("Rating từ 0-5");
        this.rating = (this.rating * totalRatings + newRating) / (totalRatings + 1);
        this.totalRatings++;
    }

    public double getRating() { return rating; }

    public void printInfo() {
        System.out.println("=== THÔNG TIN NGƯỜI DÙNG (" + getRole() + ") ===");
        System.out.println("ID       : " + getId());
        System.out.println("Username : " + username);
        System.out.println("Email    : " + email);
        System.out.println("Số dư    : " + balance + " VND");
    }
}
