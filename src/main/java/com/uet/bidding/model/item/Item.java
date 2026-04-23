package com.uet.bidding.model.item;

import com.uet.bidding.model.Entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Lớp trừu tượng đại diện cho một sản phẩm đấu giá
 */
public abstract class Item extends Entity {

    protected String name;
    protected String description;
    protected BigDecimal startingPrice;
    protected BigDecimal currentPrice;
    protected LocalDateTime startTime;
    protected LocalDateTime endTime;
    protected ItemStatus status;
    protected String sellerId;

    public Item(String name, String description, BigDecimal startingPrice,
                LocalDateTime startTime, LocalDateTime endTime, String sellerId) {
        super();
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.currentPrice = startingPrice;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = ItemStatus.OPEN;
        this.sellerId = sellerId;
    }

    public boolean isAuctionActive() {
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(startTime) && now.isBefore(endTime);
    }

    public void updatePrice(BigDecimal newPrice) {
        if (newPrice.compareTo(currentPrice) > 0) {
            this.currentPrice = newPrice;
        }
    }

    public abstract String getCategory();
    public abstract void printInfo();

    // ===== Getters for JavaFX PropertyValueFactory =====
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getStartingPrice() { return startingPrice; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public ItemStatus getStatus() { return status; }
    public String getSellerId() { return sellerId; }

    public void setStatus(ItemStatus status) { this.status = status; }
}
