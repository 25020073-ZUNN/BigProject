package com.uet.bidding.model;

import com.uet.bidding.model.item.Item;
import com.uet.bidding.model.user.Bidder;
import com.uet.bidding.model.user.Seller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Auction extends Entity {

    // Thông tin cơ bản của phiên đấu giá
    private Item item;
    private Seller seller;

    // Giá khởi điểm và giá hiện tại
    private long startingPrice;
    private long currentPrice;

    // Người đang trả giá cao nhất
    private Bidder highestBidder;

    // Trạng thái phiên đấu giá
    private boolean active;
    private boolean finished;

    // Lưu lịch sử các lần bid
    private final List<BidTransaction> bidHistory;

    // Constructor
    public Auction(Item item, Seller seller, long startingPrice) {
        super();

        if (item == null) {
            throw new IllegalArgumentException("ITEM MUST NOT BE NULL");
        }
        if (seller == null) {
            throw new IllegalArgumentException("SELLER MUST NOT BE NULL");
        }
        if (startingPrice <= 0) {
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

    // Getter
    public Item getItem() {
        return item;
    }

    public Seller getSeller() {
        return seller;
    }

    public long getStartingPrice() {
        return startingPrice;
    }

    public long getCurrentPrice() {
        return currentPrice;
    }

    public Bidder getHighestBidder() {
        return highestBidder;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isFinished() {
        return finished;
    }

    public List<BidTransaction> getBidHistory() {
        return Collections.unmodifiableList(bidHistory);
    }

    // Setter
    public void setActive(boolean active) {
        this.active = active;
    }

    // Phương thức đặt giá
    public void placeBid(Bidder bidder, long bidAmount) {
        // Không cho bid nếu phiên đã đóng
        if (!active || finished) {
            throw new IllegalStateException("AUCTION IS NOT AVAILABLE");
        }

        if (bidder == null) {
            throw new IllegalArgumentException("BIDDER MUST NOT BE NULL");
        }

        if (bidAmount <= currentPrice) {
            throw new IllegalArgumentException(
                    "BID AMOUNT MUST BE GREATER THAN CURRENT PRICE"
            );
        }

        // Có thể thêm kiểm tra bidder có phải seller không
        if (bidder.getId().equals(seller.getId())) {
            throw new IllegalArgumentException("SELLER CANNOT BID ON OWN AUCTION");
        }

        // Cập nhật giá hiện tại và người dẫn đầu
        this.currentPrice = bidAmount;
        this.highestBidder = bidder;

        // Lưu lịch sử giao dịch bid
        BidTransaction transaction = new BidTransaction(this, bidder, bidAmount);
        bidHistory.add(transaction);
    }

    // Kết thúc phiên đấu giá
    public void closeAuction() {
        if (finished) {
            throw new IllegalStateException("AUCTION HAS ALREADY BEEN CLOSED");
        }

        this.active = false;
        this.finished = true;
    }
    // Kiểm tra có người thắng chưa
    public boolean hasWinner() {
        return highestBidder != null;
    }

    // Lấy người thắng
    public Bidder getWinner() {
        if (!finished) {
            throw new IllegalStateException("AUCTION HAS NOT FINISHED YET");
        }
        return highestBidder;
    }

    // In thông tin phiên đấu giá
    public void printInfo() {
        System.out.println("=== AUCTION INFO ===");
        System.out.println("Auction ID     : " + getId());
        System.out.println("Item           : " + item.getName());
        System.out.println("Seller         : " + seller.getUsername());
        System.out.println("Starting Price : " + startingPrice + " VND");
        System.out.println("Current Price  : " + currentPrice + " VND");
        System.out.println("Highest Bidder : " + (highestBidder == null ? "None" : highestBidder.getUsername()));
        System.out.println("Active         : " + active);
        System.out.println("Finished       : " + finished);
        System.out.println("Total Bids     : " + bidHistory.size());
    }
}