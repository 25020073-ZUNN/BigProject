package com.auction.model;

import com.auction.model.user.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Lớp BidTransaction đại diện cho một lần đặt giá.
 */
public class BidTransaction extends Entity {

    private Auction auction;
    private User bidder;
    private BigDecimal bidAmount;
    private LocalDateTime bidTime;

    public BidTransaction(Auction auction, User bidder, BigDecimal bidAmount) {
        super();

        if (auction == null) {
            throw new IllegalArgumentException("AUCTION MUST NOT BE NULL");
        }
        if (bidder == null) {
            throw new IllegalArgumentException("BIDDER MUST NOT BE NULL");
        }
        if (bidAmount == null || bidAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("BID AMOUNT MUST BE POSITIVE");
        }

        this.auction = auction;
        this.bidder = bidder;
        this.bidAmount = bidAmount;
        this.bidTime = LocalDateTime.now();
    }

    public Auction getAuction() { return auction; }
    public User getBidder() { return bidder; }
    public BigDecimal getBidAmount() { return bidAmount; }
    public LocalDateTime getBidTime() { return bidTime; }

    @Override
    public void printInfo() {
        System.out.println("=== BID TRANSACTION INFO ===");
        System.out.println("Transaction ID : " + getId());
        System.out.println("Auction ID     : " + auction.getId());
        System.out.println("Bidder         : " + bidder.getUsername());
        System.out.println("Bid Amount     : " + bidAmount + " VND");
        System.out.println("Bid Time       : " + bidTime);
    }
}
