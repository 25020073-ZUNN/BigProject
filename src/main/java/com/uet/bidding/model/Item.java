package com.uet.bidding.model;
import java.time.LocalDateTime;
public abstract class Item extends Entity {
    protected String name;                // Tên sản phẩm
    protected String description;         // Mô tả sản phẩm
    protected double startingPrice;       // Giá khởi điểm
    protected double currentHighestPrice; // Giá cao nhất
    protected LocalDateTime startTime;    // Thời gian mở phiên cho sản phẩm
    protected LocalDateTime endTime;      // Thời gian kết thúc
    protected String sellerId;            // Liên kết với id seller
    public Item() {
        super();
    }
    public Item(String name, String description, double startingPrice, LocalDateTime startTime, LocalDateTime endTime, String sellerId) {
        super();
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.currentHighestPrice = startingPrice; // Lúc mới đăng, giá cao nhất chính là giá khởi điểm
        this.startTime = startTime;
        this.endTime = endTime;
        this.sellerId = sellerId;
    }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public double getCurrentHighestPrice() { return currentHighestPrice; }
    public void setCurrentHighestPrice(double currentHighestPrice) { this.currentHighestPrice = currentHighestPrice; }
    // Kiểm tra sản phẩm có trong thời gian đấu giá không
    public boolean isAuctionOpen() {
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(startTime) && now.isBefore(endTime);
    }
    // Checking ______________________________________________________
    @Override
    // In thông tin sản phẩm ***
    public void printInfo() {
        System.out.println("--- Thông tin sản phẩm ---");
        System.out.println("ID: " + getId());
        System.out.println("Tên: " + name);
        System.out.println("Mô tả: " + description);
        System.out.println("Giá hiện tại: " + currentHighestPrice);
        System.out.println("Thời gian đóng: " + endTime);
    }
}