package com.uet.bidding.model.item;

import com.uet.bidding.model.Entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Lớp trừu tượng đại diện cho một sản phẩm đấu giá
 * Các loại item cụ thể (Art, Electronics, Vehicle,...) sẽ kế thừa từ đây
 */
public abstract class Item extends Entity {

    protected String name;                // Tên sản phẩm
    protected String description;         // Mô tả sản phẩm

    protected BigDecimal startingPrice;   // Giá khởi điểm
    protected BigDecimal currentPrice;    // Giá hiện tại cao nhất

    protected LocalDateTime startTime;    // Thời gian bắt đầu đấu giá
    protected LocalDateTime endTime;      // Thời gian kết thúc đấu giá

    protected ItemStatus status;          // Trạng thái phiên đấu giá

    protected String sellerId;            // ID của người bán

    /**
     * Constructor khởi tạo item
     */
    public Item(String name, String description, BigDecimal startingPrice,
                LocalDateTime startTime, LocalDateTime endTime, String sellerId) {

        super(); // gọi constructor của Entity

        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.currentPrice = startingPrice; // ban đầu = giá khởi điểm
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = ItemStatus.OPEN;
        this.sellerId = sellerId;
    }

    /**
     * Kiểm tra xem item có đang trong trạng thái đấu giá hay không
     */
    public boolean isAuctionActive() {
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(startTime)
                && now.isBefore(endTime)
                && status == ItemStatus.RUNNING;
    }

    /**
     * Cập nhật giá hiện tại (chỉ update nếu giá mới cao hơn)
     */
    public void updatePrice(BigDecimal newPrice) {
        if (newPrice.compareTo(currentPrice) > 0) {
            this.currentPrice = newPrice;
        }
    }

    /**
     * Lấy loại item (dùng polymorphism)
     */
    public abstract String getCategory();

    /**
     * In thông tin item (override ở class con)
     */
    public abstract void printInfo();

    // ===== Getter =====

    public String getName() {
        return name;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public ItemStatus getStatus() {
        return status;
    }

    public void setStatus(ItemStatus status) {
        this.status = status;
    }
}