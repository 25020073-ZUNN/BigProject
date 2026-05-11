package com.auction.model.item;

import com.auction.model.Entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Lớp trừu tượng Item đại diện cho một sản phẩm trong hệ thống đấu giá.
 * Các lớp con như Electronics, Art, Vehicle sẽ kế thừa từ lớp này.
 */
public abstract class Item extends Entity {

    protected String name; // Tên sản phẩm
    protected String description; // Mô tả chi tiết sản phẩm
    protected BigDecimal startingPrice; // Giá khởi điểm khi bắt đầu đấu giá
    protected BigDecimal currentPrice; // Giá hiện tại sau các lượt đặt giá
    protected LocalDateTime startTime; // Thời điểm bắt đầu phiên đấu giá
    protected LocalDateTime endTime; // Thời điểm kết thúc phiên đấu giá
    protected ItemStatus status; // Trạng thái của mặt hàng (MỞ, ĐÓNG, ĐÃ BÁN, ...)
    protected String sellerId; // ID của người bán mặt hàng này

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

    /**
     * Kiểm tra xem phiên đấu giá của mặt hàng này có đang trong thời gian diễn ra hay không.
     */
    public boolean isAuctionActive() {
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(startTime) && now.isBefore(endTime);
    }

    /**
     * Cập nhật giá mới cho sản phẩm nếu giá mới cao hơn giá hiện tại.
     */
    public void updatePrice(BigDecimal newPrice) {
        if (newPrice.compareTo(currentPrice) > 0) {
            this.currentPrice = newPrice;
        }
    }

    public abstract String getCategory(); // Lấy tên phân loại (Điện tử, Xe cộ, ...)
    public abstract void printInfo(); // In thông tin chi tiết ra Console

    // ===== Getters hỗ trợ JavaFX PropertyValueFactory để hiển thị dữ liệu lên TableView =====
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getStartingPrice() { return startingPrice; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public ItemStatus getStatus() { return status; }
    public String getSellerId() { return sellerId; }

    public void setStatus(ItemStatus status) { this.status = status; }
    public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
}
