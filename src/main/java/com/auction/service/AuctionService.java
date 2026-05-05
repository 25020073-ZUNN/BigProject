package com.auction.service;

import com.auction.model.Auction;
import com.auction.model.item.Item;
import com.auction.model.user.User;
import com.auction.factory.ItemFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service quản lý các phiên đấu giá (Singleton).
 * Đã được cập nhật để sử dụng lớp User chung.
 */
public class AuctionService {

    private static AuctionService instance;
    private List<Auction> auctions;

    private AuctionService() {
        auctions = new ArrayList<>();
        loadSampleData();
    }

    public static synchronized AuctionService getInstance() {
        if (instance == null) {
            instance = new AuctionService();
        }
        return instance;
    }

    private void loadSampleData() {
        User user1 = new User("user_001", "user1@example.com", "pass");
        User user2 = new User("user_002", "user2@example.com", "pass");

        Item item1 = ItemFactory.createElectronics(
                "iPhone 15 Pro Max", 
                "Máy mới 99%, đầy đủ phụ kiện", 
                new BigDecimal("25000000"),
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(2),
                user1.getId(),
                "Apple",
                12
        );

        Item item2 = ItemFactory.createElectronics(
                "ASUS ROG Strix G15", 
                "Laptop gaming cấu hình cao", 
                new BigDecimal("30500000"),
                LocalDateTime.now().minusHours(5),
                LocalDateTime.now().plusHours(12),
                user2.getId(),
                "ASUS",
                24
        );

        auctions.add(new Auction(item1, user1, item1.getCurrentPrice()));
        auctions.add(new Auction(item2, user2, item2.getCurrentPrice()));
    }

    public List<Auction> getAllAuctions() {
        return auctions;
    }

    public List<Item> getAllItems() {
        return auctions.stream().map(Auction::getItem).collect(Collectors.toList());
    }

    public Auction getAuctionByItem(Item item) {
        return auctions.stream()
                .filter(a -> a.getItem().equals(item))
                .findFirst()
                .orElse(null);
    }

    public boolean placeBid(Auction auction, User bidder, BigDecimal amount) {
        if (auction == null || bidder == null) return false;
        try {
            auction.placeBid(bidder, amount);
            return true;
        } catch (Exception e) {
            System.err.println("Bid failed: " + e.getMessage());
            return false;
        }
    }
}
