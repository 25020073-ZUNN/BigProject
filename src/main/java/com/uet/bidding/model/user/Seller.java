package com.uet.bidding.model.user;
import java.util.ArrayList;
import java.util.List;

/**
 * Lớp Seller đại diện cho người bán trong hệ thống đấu giá.
 * Seller kế thừa từ User nên sẽ có các thông tin chung như:
 * username, email, passwordHash, id, ...
 */
public class Seller extends User {

    // Danh sách id các món hàng mà người bán đã đăng bán
    private List<String> listedItemIds;

    // Điểm đánh giá trung bình của người bán
    private double rating;

    // Tổng số lượt đánh giá
    private int totalRatings;

    /**
     * Constructor khởi tạo người bán.
     * Gọi constructor của lớp cha User để gán thông tin cơ bản.
     */
    public Seller(String username, String email, String passwordHash) {
        super(username, email, passwordHash);
        this.listedItemIds = new ArrayList<>();
        this.rating = 0.0;
        this.totalRatings = 0;
    }

    /**
     * Trả về vai trò của user.
     */
    @Override
    public String getRole() {
        return "SELLER";
    }

    /**
     * Thêm id của một món hàng vào danh sách hàng đã đăng bán.
     */
    public void addListedItem(String itemId) {
        listedItemIds.add(itemId);
    }

    /**
     * Trả về danh sách item id mà seller đã đăng.
     * Dùng bản sao để tránh code bên ngoài sửa trực tiếp list gốc.
     */
    public List<String> getListedItemIds() {
        return new ArrayList<>(listedItemIds);
    }

    /**
     * Thêm một lượt đánh giá mới cho người bán.
     *
     * Công thức cập nhật điểm trung bình:
     * newAvg = (oldAvg * n + newVal) / (n + 1)
     *
     * Cách này giúp cập nhật nhanh mà không cần lưu toàn bộ
     * lịch sử các điểm đánh giá cũ.
     */
    public void addRating(double newRating) {
        // Điểm đánh giá chỉ được nằm trong khoảng 0 đến 5
        if (newRating < 0.0 || newRating > 5.0) {
            throw new IllegalArgumentException("Rating must be 0-5");
        }
        // Cập nhật điểm trung bình mới
        this.rating = (this.rating * totalRatings + newRating) / (totalRatings + 1);

        // Tăng số lượt đánh giá lên 1
        this.totalRatings++;
    }

    /**
     * Lấy điểm đánh giá trung bình hiện tại.
     */
    public double getRating() {
        return rating;
    }

    /**
     * Lấy tổng số lượt đánh giá.
     */
    public int getTotalRatings() {
        return totalRatings;
    }
}