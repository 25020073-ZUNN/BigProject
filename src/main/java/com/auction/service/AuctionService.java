package com.auction.service;

import com.auction.model.Auction;
import com.auction.model.item.Item;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;
import com.auction.factory.ItemFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Service quản lý các phiên đấu giá (Singleton)
 */
public class AuctionService {
    private static final Logger LOGGER = Logger.getLogger(AuctionService.class.getName());

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
        Seller seller1 = new Seller("seller_001", "seller1@example.com", "pass");
        Seller seller2 = new Seller("seller_002", "seller2@example.com", "pass");

        Item item1 = ItemFactory.createElectronics(
                "iPhone 15 Pro Max", 
                "Máy mới 99%, đầy đủ phụ kiện", 
                new BigDecimal("25000000"),
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(2),
                seller1.getId(),
                "Apple",
                12
        );

        Item item2 = ItemFactory.createElectronics(
                "ASUS ROG Strix G15", 
                "Laptop gaming cấu hình cao", 
                new BigDecimal("30500000"),
                LocalDateTime.now().minusHours(5),
                LocalDateTime.now().plusHours(12),
                seller2.getId(),
                "ASUS",
                24
        );

        auctions.add(new Auction(item1, seller1, item1.getCurrentPrice()));
        auctions.add(new Auction(item2, seller2, item2.getCurrentPrice()));
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

    public boolean placeBid(Auction auction, Bidder bidder, BigDecimal amount) {
        if (auction == null || bidder == null) return false;
        try {
            auction.placeBid(bidder, amount);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Bid failed", e);
            return false;
        }
    }
}
