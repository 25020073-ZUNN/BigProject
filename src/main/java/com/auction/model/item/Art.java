package com.auction.model.item;

import java.math.BigDecimal;
import java.sql.SQLOutput; // Import này có vẻ không được sử dụng, có thể xem xét xóa nếu không cần thiết.
import java.time.LocalDateTime;

/**
 * Lớp Art (Nghệ thuật) đại diện cho một loại mặt hàng đấu giá cụ thể là tác phẩm nghệ thuật.
 * Kế thừa từ lớp Item, bổ sung các thuộc tính đặc trưng của tác phẩm nghệ thuật.
 */
public class Art extends Item {

    private String artist;     // Thuộc tính lưu trữ tên tác giả của tác phẩm nghệ thuật.
    private int yearCreated;   // Thuộc tính lưu trữ năm sáng tác của tác phẩm nghệ thuật.

    /**
     * Constructor để khởi tạo một đối tượng Art mới.
     *
     * @param name Tên của tác phẩm nghệ thuật.
     * @param description Mô tả chi tiết về tác phẩm.
     * @param startingPrice Giá khởi điểm của tác phẩm nghệ thuật.
     * @param startTime Thời gian bắt đầu đấu giá.
     * @param endTime Thời gian kết thúc đấu giá.
     * @param sellerId ID của người bán (người tạo đấu giá) tác phẩm này.
     * @param artist Tên của nghệ sĩ/tác giả.
     * @param yearCreated Năm tác phẩm được sáng tác.
     */
    public Art(String name, String description, BigDecimal startingPrice,
               LocalDateTime startTime, LocalDateTime endTime, String sellerId,
               String artist, int yearCreated) {

        // Gọi constructor của lớp cha (Item) để khởi tạo các thuộc tính chung của mặt hàng.
        super(name, description, startingPrice, startTime, endTime, sellerId);
        // Khởi tạo các thuộc tính đặc trưng của Art.
        this.artist = artist;
        this.yearCreated = yearCreated;
    }

    /**
     * Ghi đè phương thức getCategory từ lớp cha để trả về danh mục cụ thể là "Art".
     *
     * @return Chuỗi "Art" đại diện cho danh mục của mặt hàng.
     */
    @Override
    public String getCategory() {
        return "Art";
    }

    /**
     * Phương thức getter để lấy tên tác giả của tác phẩm nghệ thuật.
     *
     * @return Tên tác giả.
     */
    public String getArtist() {
        return artist;
    }

    /**
     * Phương thức getter để lấy năm sáng tác của tác phẩm nghệ thuật.
     *
     * @return Năm sáng tác.
     */
    public int getYearCreated() {
        return yearCreated;
    }

    /**
     * Ghi đè phương thức printInfo từ lớp cha để hiển thị thông tin chi tiết
     * về tác phẩm nghệ thuật theo định dạng cụ thể.
     */
    @Override
        public void printInfo(){
        System.out.println("🎨 Art: " + name // Hiển thị biểu tượng và tên tác phẩm.
                + " | Artist: " + artist // Hiển thị tên tác giả.
                + " | Price: " + currentPrice); // Hiển thị giá hiện tại của tác phẩm.
    }
}
