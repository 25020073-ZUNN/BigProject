package com.uet.service;

import com.uet.bidding.model.Auction;
import com.uet.bidding.model.item.Art;
import com.uet.bidding.model.user.Bidder;
import com.uet.bidding.model.user.Seller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service to handle all auction-related business logic.
 * Follows Clean Architecture by separating business logic from the UI.
 */
public class AuctionService {

    private final List<Auction> auctions = new ArrayList<>();

    public AuctionService() {
        // Initialize with some mock data for demonstration
        initializeMockData();
    }

    private void initializeMockData() {
        Seller seller1 = new Seller("seller_a", "seller_a@example.com", "pass");
        Seller seller2 = new Seller("seller_b", "seller_b@example.com", "pass");

        Art art1 = new Art("Điện thoại iPhone 15 Pro Max", "Brand new", new BigDecimal("25000000"),
                LocalDateTime.now(), LocalDateTime.now().plusDays(2), seller1.getId(), "Tech", 2000);
        Auction auction1 = new Auction(art1, seller1, new BigDecimal("25000000"));

        Art art2 = new Art("Laptop ASUS ROG Strix", "Gaming laptop", new BigDecimal("30500000"),
                LocalDateTime.now(), LocalDateTime.now().plusDays(1), seller2.getId(), "Tech", 2000);
        Auction auction2 = new Auction(art2, seller2, new BigDecimal("30500000"));

        auctions.add(auction1);
        auctions.add(auction2);
    }

    public List<Auction> getAllAuctions() {
        return new ArrayList<>(auctions);
    }

    public Optional<Auction> getAuctionById(String id) {
        return auctions.stream()
                .filter(a -> a.getId().equals(id))
                .findFirst();
    }

    /**
     * Business logic for placing a bid.
     * Validates the bid and updates the auction state.
     */
    public boolean placeBid(Auction auction, Bidder bidder, BigDecimal amount) {
        if (auction == null || bidder == null || amount == null) {
            return false;
        }

        try {
            // Business logic is encapsulated within the Auction model
            auction.placeBid(bidder, amount);
            return true;
        } catch (IllegalArgumentException | IllegalStateException e) {
            // Logic validation failed (e.g., bid too low, auction closed)
            System.err.println("Bid rejected: " + e.getMessage());
            return false;
        }
    }
}
