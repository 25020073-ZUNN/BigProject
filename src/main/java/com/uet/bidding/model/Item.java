package com.uet.bidding.model;
import java.time.LocalDateTime;
public abstract class Item extends Entity{
    protected String name;            // Tên sản phẩm
    protected String description;      // Mô tả sản phẩm
    protected double startPrice;       // giá khởi điểm
    protected double currentHighestPrice;  // giá cao nhất
    protected LocalDateTime startTime;  // time mở phiên cho sản phẩm
    protected LocalDateTime endTime;   // time kết thúc
    protected String sellerId; // liên kết với id seller
    public Item()
    {
        super();
    }
    public Item(String name ,String description)
}