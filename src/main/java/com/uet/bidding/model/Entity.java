package com.uet.bidding.model;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;
// Lớp cơ sở cho hệ thống
public abstract class Entity  implements Serializable{
    // Encapsulation: Sử dụng protected/private kết hợp getter/setter
    protected String id;
    protected LocalDateTime createdAt;
    public Entity() {
        this.id = UUID.randomUUID().toString(); // id User được random khi tạo
        this.createdAt = LocalDateTime.now();  // lưu thời gian tạo
    }
    // Constructor có tham số cho trường hợp cần khôi phục dữ liệu từ Database.
    public Entity(String id, LocalDateTime createdAt) {
        this.id = id;
        this.createdAt = createdAt;
    }
    // Getters và Setters đảm bảo tính đóng gói (Encapsulation)
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    public abstract void printInfo();
}