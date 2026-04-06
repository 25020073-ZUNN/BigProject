package com.uet.bidding.model.user;

import java.util.ArrayList;
import java.util.List;

/**
 * Lớp Bidder đại diện cho người tham gia đấu giá trong hệ thống.
 *
 * Bidder kế thừa từ User nên có các thuộc tính chung như:
 * - id
 * - username
 * - email
 * - passwordHash
 * - balance
 * - active
 *
 * Ngoài ra Bidder có thêm các thông tin riêng liên quan đến
 * việc tham gia đấu giá như lịch sử bid và các auction đã thắng.
 */
public class Bidder extends User {

    // Danh sách id các phiên đấu giá mà bidder đã tham gia ra giá
    private List<String> joinedAuctionIds;

    // Danh sách id các phiên đấu giá mà bidder đã thắng
    private List<String> wonAuctionIds;

    // Tổng số lần bidder đã đặt giá
    private int totalBids;

    /**
     * Constructor khởi tạo đối tượng Bidder.
     *
     * @param username tên đăng nhập
     * @param email email người dùng
     * @param passwordHash mật khẩu đã được băm
     */
    public Bidder(String username, String email, String passwordHash) {
        super(username, email, passwordHash);
        this.joinedAuctionIds = new ArrayList<>();
        this.wonAuctionIds = new ArrayList<>();
        this.totalBids = 0;
    }

    /**
     * Trả về vai trò của user.
     *
     * @return "BIDDER"
     */
    @Override
    public String getRole() {
        return "BIDDER";
    }

    /**
     * Thêm một auction vào danh sách auction mà bidder đã tham gia.
     *
     * @param auctionId id của phiên đấu giá
     */
    public void joinAuction(String auctionId) {
        if (auctionId == null || auctionId.isBlank()) {
            throw new IllegalArgumentException("AUCTION ID MUST NOT BE EMPTY");
        }

        // Tránh thêm trùng nếu bidder đã tham gia auction này rồi
        if (!joinedAuctionIds.contains(auctionId)) {
            joinedAuctionIds.add(auctionId);
        }
    }

    /**
     * Ghi nhận bidder đã thắng một phiên đấu giá.
     *
     * @param auctionId id của phiên đấu giá thắng
     */
    public void addWonAuction(String auctionId) {
        if (auctionId == null || auctionId.isBlank()) {
            throw new IllegalArgumentException("AUCTION ID MUST NOT BE EMPTY");
        }

        if (!wonAuctionIds.contains(auctionId)) {
            wonAuctionIds.add(auctionId);
        }
    }

    /**
     * Tăng số lần đặt giá của bidder lên 1.
     * Method này thường được gọi sau mỗi lần bid thành công.
     */
    public void increaseTotalBids() {
        this.totalBids++;
    }

    /**
     * Trả về danh sách auction mà bidder đã tham gia.
     * Trả về bản sao để bảo vệ dữ liệu bên trong object.
     *
     * @return danh sách id auction đã tham gia
     */
    public List<String> getJoinedAuctionIds() {
        return new ArrayList<>(joinedAuctionIds);
    }

    /**
     * Trả về danh sách auction mà bidder đã thắng.
     * Trả về bản sao để tránh bị sửa trực tiếp từ bên ngoài.
     *
     * @return danh sách id auction đã thắng
     */
    public List<String> getWonAuctionIds() {
        return new ArrayList<>(wonAuctionIds);
    }

    /**
     * Lấy tổng số lần bidder đã đặt giá.
     *
     * @return số lần bid
     */
    public int getTotalBids() {
        return totalBids;
    }

    /**
     * Kiểm tra bidder đã tham gia auction này chưa.
     *
     * @param auctionId id của phiên đấu giá
     * @return true nếu đã tham gia, ngược lại false
     */
    public boolean hasJoinedAuction(String auctionId) {
        if (auctionId == null || auctionId.isBlank()) {
            return false;
        }
        return joinedAuctionIds.contains(auctionId);
    }

    /**
     * Kiểm tra bidder đã thắng auction này chưa.
     *
     * @param auctionId id của phiên đấu giá
     * @return true nếu đã thắng, ngược lại false
     */
    public boolean hasWonAuction(String auctionId) {
        if (auctionId == null || auctionId.isBlank()) {
            return false;
        }
        return wonAuctionIds.contains(auctionId);
    }

    /**
     * In thông tin bidder.
     * Dùng để hiển thị nhanh trong console hoặc debug.
     */
    @Override
    public void printInfo() {
        System.out.println("=== BIDDER INFO ===");
        System.out.println("ID              : " + getId());
        System.out.println("Username        : " + getUsername());
        System.out.println("Email           : " + getEmail());
        System.out.println("Balance         : " + getBalance() + " VND");
        System.out.println("Active          : " + isActive());
        System.out.println("Joined Auctions : " + joinedAuctionIds.size());
        System.out.println("Won Auctions    : " + wonAuctionIds.size());
        System.out.println("Total Bids      : " + totalBids);
    }
}