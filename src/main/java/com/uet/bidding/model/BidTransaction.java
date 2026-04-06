package com.uet.bidding.model;

import com.uet.bidding.model.user.Bidder;

import java.time.LocalDateTime;

public class BidTransaction extends Entity {

    // Phiên đấu giá mà lần bid này thuộc về
    private Auction auction;

    // Người đã ra giá
    private Bidder bidder;

    // Số tiền được bid
    private long bidAmount;

    // Thời điểm thực hiện bid
    private LocalDateTime bidTime;

    // Constructor
    public BidTransaction(Auction auction, Bidder bidder, long bidAmount) {
        super();

        // Kiểm tra dữ liệu đầu vào để tránh tạo object lỗi
        if (auction == null) {
            throw new IllegalArgumentException("AUCTION MUST NOT BE NULL");
        }
        if (bidder == null) {
            throw new IllegalArgumentException("BIDDER MUST NOT BE NULL");
        }
        if (bidAmount <= 0) {
            throw new IllegalArgumentException("BID AMOUNT MUST BE POSITIVE");
        }

        this.auction = auction;
        this.bidder = bidder;
        this.bidAmount = bidAmount;
        this.bidTime = LocalDateTime.now();
    }

    // Getter
    public Auction getAuction() {
        return auction;
    }

    public Bidder getBidder() {
        return bidder;
    }

    public long getBidAmount() {
        return bidAmount;
    }

    public LocalDateTime getBidTime() {
        return bidTime;
    }

    // Setter
    // Thường BidTransaction là dữ liệu lịch sử nên ít khi cho sửa.
    // Nếu muốn "read-only" hoàn toàn thì có thể xóa hết setter.
    public void setBidAmount(long bidAmount) {
        if (bidAmount <= 0) {
            throw new IllegalArgumentException("BID AMOUNT MUST BE POSITIVE");
        }
        this.bidAmount = bidAmount;
    }

    // In thông tin giao dịch bid
    public void printInfo() {
        System.out.println("=== BID TRANSACTION INFO ===");
        System.out.println("Transaction ID : " + getId());
        System.out.println("Auction ID     : " + auction.getId());
        System.out.println("Bidder         : " + bidder.getUsername());
        System.out.println("Bid Amount     : " + bidAmount + " VND");
        System.out.println("Bid Time       : " + bidTime);
    }
}