package com.uet.bidding.model;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lớp cha (base class) cho tất cả các entity trong hệ thống
 * Ví dụ: User, Item, Auction, BidTransaction,...
 *
 * Mục đích:
 * - Tránh lặp code (id, createdAt,...)
 * - Chuẩn hóa dữ liệu chung
 */
public abstract class Entity {

    protected String id;                  // ID duy nhất của object
    protected LocalDateTime createdAt;    // Thời điểm tạo
    protected LocalDateTime updatedAt;    // Thời điểm cập nhật gần nhất

    /**
     * Constructor mặc định
     * - Tự sinh id bằng UUID
     * - Gán thời gian tạo và cập nhật
     */
    public Entity() {
        this.id = UUID.randomUUID().toString(); // sinh id random
        this.createdAt = LocalDateTime.now();   // thời điểm tạo
        this.updatedAt = LocalDateTime.now();   // ban đầu = createdAt
    }
    /**
     * Getter lấy id
     */
    public String getId() {
        return id;
    }

    /**
     * Getter lấy thời gian tạo
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    /**
     * Getter lấy thời gian cập nhật
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Hàm cập nhật lại thời gian modified
     * Gọi khi object bị thay đổi
     */
    public void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}