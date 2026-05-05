package com.auction.model;

import com.auction.model.item.Item;
import com.auction.model.user.User;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Lớp Auction đại diện cho một phiên đấu giá.
 * Đã được cập nhật để sử dụng lớp User chung thay vì Bidder/Seller.
 */
public class Auction extends Entity {

    private Item item;
    private User seller;

    private BigDecimal startingPrice;
    private BigDecimal currentPrice;

    private User highestBidder;

    private boolean active;
    private boolean finished;

    private final List<BidTransaction> bidHistory;

    public Auction(Item item, User seller, BigDecimal startingPrice) {
        super();

        if (item == null) {
            throw new IllegalArgumentException("ITEM MUST NOT BE NULL");
        }
        if (seller == null) {
            throw new IllegalArgumentException("SELLER MUST NOT BE NULL");
        }
        if (startingPrice == null || startingPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("STARTING PRICE MUST BE POSITIVE");
        }

        this.item = item;
        this.seller = seller;
        this.startingPrice = startingPrice;
        this.currentPrice = startingPrice;
        this.highestBidder = null;
        this.active = true;
        this.finished = false;
        this.bidHistory = new ArrayList<>();
    }

    public Item getItem() { return item; }
    public User getSeller() { return seller; }
    public BigDecimal getStartingPrice() { return startingPrice; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public User getHighestBidder() { return highestBidder; }
    public boolean isActive() { return active; }
    public boolean isFinished() { return finished; }
    public List<BidTransaction> getBidHistory() { return Collections.unmodifiableList(bidHistory); }

    public void setActive(boolean active) { this.active = active; }

    public void placeBid(User bidder, BigDecimal bidAmount) {
        if (!active || finished) {
            throw new IllegalStateException("AUCTION IS NOT AVAILABLE");
        }

        if (bidder == null) {
            throw new IllegalArgumentException("BIDDER MUST NOT BE NULL");
        }

        if (bidAmount == null || bidAmount.compareTo(currentPrice) <= 0) {
            throw new IllegalArgumentException("BID AMOUNT MUST BE GREATER THAN CURRENT PRICE");
        }

        if (bidder.getId().equals(seller.getId())) {
            throw new IllegalArgumentException("SELLER CANNOT BID ON OWN AUCTION");
        }

        this.currentPrice = bidAmount;
        this.highestBidder = bidder;

        BidTransaction transaction = new BidTransaction(this, bidder, bidAmount);
        bidHistory.add(transaction);
        
        // Cập nhật thông tin cho người dùng
        bidder.joinAuction(this.getId());
        bidder.increaseTotalBids();
    }

    public void closeAuction() {
        if (finished) {
            throw new IllegalStateException("AUCTION HAS ALREADY BEEN CLOSED");
        }
        this.active = false;
        this.finished = true;
        
        if (highestBidder != null) {
            highestBidder.addWonAuction(this.getId());
        }
    }

    public boolean hasWinner() { return highestBidder != null; }

    public User getWinner() {
        if (!finished) {
            throw new IllegalStateException("AUCTION HAS NOT FINISHED YET");
        }
        return highestBidder;
    }

    @Override
    public void printInfo() {
        System.out.println("=== AUCTION INFO ===");
        System.out.println("Auction ID     : " + getId());
        System.out.println("Item           : " + item.getName());
        System.out.println("Seller         : " + seller.getUsername());
        System.out.println("Current Price  : " + currentPrice + " VND");
        System.out.println("Highest Bidder : " + (highestBidder == null ? "None" : highestBidder.getUsername()));
        System.out.println("Active         : " + active);
        System.out.println("Finished       : " + finished);
    }
}
