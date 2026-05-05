package com.auction.model;

import com.auction.model.user.Bidder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.logging.Logger;

public class BidTransaction extends Entity {
    private static final Logger LOGGER = Logger.getLogger(BidTransaction.class.getName());

    // Phiên đấu giá mà lần bid này thuộc về
    private Auction auction;

    // Người đã ra giá
    private Bidder bidder;

    // Số tiền được bid
    private BigDecimal bidAmount;

    // Thời điểm thực hiện bid
    private LocalDateTime bidTime;

    // Constructor
    public BidTransaction(Auction auction, Bidder bidder, BigDecimal bidAmount) {
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

    // Getter
    public Auction getAuction() {
        return auction;
    }

    public Bidder getBidder() {
        return bidder;
    }

    public BigDecimal getBidAmount() {
        return bidAmount;
    }

    public LocalDateTime getBidTime() {
        return bidTime;
    }

    // Setter
    public void setBidAmount(BigDecimal bidAmount) {
        if (bidAmount == null || bidAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("BID AMOUNT MUST BE POSITIVE");
        }
        this.bidAmount = bidAmount;
    }

    // In thông tin giao dịch bid
    @Override
    public void printInfo() {
        LOGGER.info(() -> """
                === BID TRANSACTION INFO ===
                Transaction ID : %s
                Auction ID     : %s
                Bidder         : %s
                Bid Amount     : %s VND
                Bid Time       : %s
                """.formatted(getId(), auction.getId(), bidder.getUsername(), bidAmount, bidTime));
    }
}
