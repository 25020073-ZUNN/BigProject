package com.auction.service;

import com.auction.model.Auction;
import com.auction.model.item.Item;
import com.auction.model.user.User;
import com.auction.factory.ItemFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AuctionService - Dịch vụ quản lý các phiên đấu giá.
 * Lớp này sử dụng mẫu thiết kế Singleton để quản lý danh sách các cuộc đấu giá trong bộ nhớ hệ thống.
 * Chịu trách nhiệm cung cấp dữ liệu về các tài sản đang đấu giá và xử lý logic đặt giá (bidding).
 */
public class AuctionService {

    private static AuctionService instance; // Instance duy nhất của AuctionService
    private List<Auction> auctions; // Danh sách lưu trữ các phiên đấu giá hiện có

    /**
     * Constructor riêng tư. Khởi tạo danh sách và nạp dữ liệu mẫu khi Server khởi động.
     */
    private AuctionService() {
        auctions = new ArrayList<>();
        loadSampleData();
    }

    /**
     * Phương thức tĩnh để lấy instance duy nhất (Thread-safe với synchronized).
     */
    public static synchronized AuctionService getInstance() {
        if (instance == null) {
            instance = new AuctionService();
        }
        return instance;
    }

    /**
     * Nạp dữ liệu mẫu (Mock Data) để kiểm thử hệ thống khi chưa có dữ liệu thực từ Database.
     * Tạo ra một số tài sản như iPhone, Laptop để hiển thị trên giao diện.
     */
    private void loadSampleData() {
        // Tạo người dùng mẫu (Người bán)
        User user1 = new User("user_001", "user1@example.com", "pass");
        User user2 = new User("user_002", "user2@example.com", "pass");

        // Sử dụng ItemFactory để tạo các mặt hàng điện tử mẫu
        Item item1 = ItemFactory.createElectronics(
                "iPhone 15 Pro Max", 
                "Máy mới 99%, đầy đủ phụ kiện", 
                new BigDecimal("25000000"),
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(2),
                user1.getId(),
                "Apple",
                12
        );

        Item item2 = ItemFactory.createElectronics(
                "ASUS ROG Strix G15", 
                "Laptop gaming cấu hình cao", 
                new BigDecimal("30500000"),
                LocalDateTime.now().minusHours(5),
                LocalDateTime.now().plusHours(12),
                user2.getId(),
                "ASUS",
                24
        );

        // Thêm vào danh sách quản lý
        auctions.add(new Auction(item1, user1, item1.getCurrentPrice()));
        auctions.add(new Auction(item2, user2, item2.getCurrentPrice()));
    }

    /**
     * Lấy danh sách tất cả các phiên đấu giá hiện có.
     */
    public List<Auction> getAllAuctions() {
        return auctions;
    }

    /**
     * Chỉ lấy danh sách các mặt hàng (Item) từ các phiên đấu giá.
     */
    public List<Item> getAllItems() {
        return auctions.stream().map(Auction::getItem).collect(Collectors.toList());
    }

    /**
     * Tìm kiếm phiên đấu giá dựa trên một mặt hàng cụ thể.
     */
    public Auction getAuctionByItem(Item item) {
        return auctions.stream()
                .filter(a -> a.getItem().equals(item))
                .findFirst()
                .orElse(null);
    }

    /**
     * Xử lý logic đặt giá cho một phiên đấu giá.
     * @param auction Phiên đấu giá mục tiêu
     * @param bidder Người tham gia đặt giá
     * @param amount Số tiền đặt giá mới
     * @return true nếu đặt giá thành công (số tiền cao hơn giá hiện tại), false nếu thất bại.
     */
    public boolean placeBid(Auction auction, User bidder, BigDecimal amount) {
        // Kiểm tra tính hợp lệ của đối tượng
        if (auction == null || bidder == null) return false;
        
        try {
            // Gọi phương thức placeBid bên trong lớp Auction để thực hiện kiểm tra giá
            // Phương thức này sẽ ném ngoại lệ nếu số tiền không hợp lệ hoặc thấp hơn giá hiện tại
            auction.placeBid(bidder, amount);
            return true;
        } catch (Exception e) {
            // Ghi nhận lỗi nếu quá trình đặt giá thất bại
            System.err.println("Đặt giá thất bại: " + e.getMessage());
            return false;
        }
    }
}
