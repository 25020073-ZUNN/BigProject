package com.auction.model;

import com.auction.model.item.Item;
import com.auction.model.user.User;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Lớp Auction đại diện cho một phiên đấu giá cụ thể.
 * Quản lý thông tin về sản phẩm, người bán, lịch sử đặt giá và trạng thái của phiên đấu giá.
 */
public class Auction extends Entity {

    private Item item; // Sản phẩm đang được đấu giá
    private User seller; // Người đăng bán sản phẩm

    private BigDecimal startingPrice; // Giá khởi điểm
    private BigDecimal currentPrice; // Giá hiện tại (giá cao nhất đã đặt)
    private BigDecimal minimumBidStep; // Bước giá tối thiểu của phiên đấu giá

    private User highestBidder; // Người đang giữ mức giá cao nhất

    private boolean active; // Trạng thái đang hoạt động (có thể đặt giá)
    private boolean finished; // Trạng thái đã kết thúc

    private final List<BidTransaction> bidHistory; // Lịch sử các lượt đặt giá

    /**
     * Khởi tạo một phiên đấu giá mới.
     */
    public Auction(Item item, User seller, BigDecimal startingPrice) {
        super();

        // Kiểm tra tính hợp lệ của dữ liệu đầu vào
        if (item == null) {
            throw new IllegalArgumentException("Sản phẩm không được để trống");
        }
        if (seller == null) {
            throw new IllegalArgumentException("Người bán không được để trống");
        }
        if (startingPrice == null || startingPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Giá khởi điểm phải là số dương");
        }

        this.item = item;
        this.seller = seller;
        this.startingPrice = startingPrice;
        this.currentPrice = startingPrice;
        this.minimumBidStep = BigDecimal.ZERO;
        this.highestBidder = null;
        this.active = true;
        this.finished = false;
        this.bidHistory = new ArrayList<>();
    }

    // Các hàm Getter/Setter
    public Item getItem() { return item; }
    public User getSeller() { return seller; }
    public BigDecimal getStartingPrice() { return startingPrice; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public BigDecimal getMinimumBidStep() { return minimumBidStep; }
    public User getHighestBidder() { return highestBidder; }
    public boolean isActive() { return active; }
    public boolean isFinished() { return finished; }
    public List<BidTransaction> getBidHistory() { return Collections.unmodifiableList(bidHistory); }

    public void setActive(boolean active) { this.active = active; }
    public void setFinished(boolean finished) { this.finished = finished; }
    public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }
    public void setHighestBidder(User highestBidder) { this.highestBidder = highestBidder; }
    public void setMinimumBidStep(BigDecimal minimumBidStep) { this.minimumBidStep = minimumBidStep; }

    /**
     * Thực hiện đặt giá mới cho phiên đấu giá.
     * @param bidder Người đặt giá
     * @param bidAmount Số tiền đặt giá mới
     */
    public synchronized void placeBid(User bidder, BigDecimal bidAmount) {
        // 1. Kiểm tra trạng thái phiên đấu giá
        if (!active || finished) {
            throw new IllegalStateException("Phiên đấu giá đã đóng hoặc không khả dụng");
        }

        if (bidder == null) {
            throw new IllegalArgumentException("Người đặt giá không hợp lệ");
        }

        // 2. Kiểm tra giá đặt mới có cao hơn giá hiện tại không
        if (bidAmount == null || bidAmount.compareTo(currentPrice) <= 0) {
            throw new IllegalArgumentException("Giá đặt phải cao hơn giá hiện tại");
        }

        // 3. Người bán không được phép tự đấu giá sản phẩm của chính mình
        if (bidder.getId().equals(seller.getId())) {
            throw new IllegalArgumentException("Người bán không thể đặt giá cho sản phẩm của mình");
        }

        // Cập nhật thông tin giá cao nhất và người thắng hiện tại
        this.currentPrice = bidAmount;
        this.highestBidder = bidder;
        this.item.updatePrice(bidAmount);

        // Lưu vào lịch sử giao dịch
        BidTransaction transaction = new BidTransaction(this, bidder, bidAmount);
        bidHistory.add(transaction);
        
        // Cập nhật thông tin cho người dùng
        bidder.joinAuction(this.getId());
        bidder.increaseTotalBids();
    }

    /**
     * Dùng riêng cho lớp DAO khi cần nạp lại lịch sử bid cũ từ cơ sở dữ liệu.
     *
     * Tại sao cần hàm này:
     * - `getBidHistory()` trả về danh sách chỉ đọc để bảo vệ dữ liệu nghiệp vụ.
     * - Nhưng khi khởi động ứng dụng, chúng ta vẫn cần "đổ" lịch sử thật từ DB vào object.
     * - Hàm này tách biệt rõ với `placeBid()` để tránh vô tình chạy lại toàn bộ nghiệp vụ
     *   như kiểm tra giá, cập nhật thống kê người dùng, hoặc tạo timestamp mới.
     */
    public void addHistoricalBid(BidTransaction transaction) {
        if (transaction != null) {
            bidHistory.add(transaction);
        }
    }

    /**
     * Đóng phiên đấu giá khi hết thời gian hoặc được yêu cầu dừng.
     */
    public void closeAuction() {
        if (finished) {
            throw new IllegalStateException("Phiên đấu giá này đã được đóng trước đó");
        }
        this.active = false;
        this.finished = true;
        
        // Nếu có người đặt giá, ghi nhận phiên đấu giá này vào danh sách đã thắng của họ
        if (highestBidder != null) {
            highestBidder.addWonAuction(this.getId());
        }
    }

    public boolean hasWinner() { return highestBidder != null; }

    /**
     * Lấy người thắng cuộc sau khi kết thúc.
     */
    public User getWinner() {
        if (!finished) {
            throw new IllegalStateException("Phiên đấu giá chưa kết thúc");
        }
        return highestBidder;
    }

    @Override
    public void printInfo() {
        System.out.println("=== THÔNG TIN ĐẤU GIÁ ===");
        System.out.println("Mã đấu giá     : " + getId());
        System.out.println("Sản phẩm       : " + item.getName());
        System.out.println("Người bán      : " + seller.getUsername());
        System.out.println("Giá hiện tại   : " + currentPrice + " VND");
        System.out.println("Người dẫn đầu  : " + (highestBidder == null ? "Chưa có" : highestBidder.getUsername()));
        System.out.println("Trạng thái     : " + (active ? "Đang diễn ra" : "Đã dừng"));
    }
}
