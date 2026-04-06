package com.uet.bidding.model.user;

import java.util.ArrayList;
import java.util.List;

/**
 * Lớp Admin đại diện cho quản trị viên trong hệ thống đấu giá.
 *
 * Admin kế thừa từ User nên có đầy đủ các thuộc tính chung như:
 * - id
 * - username
 * - email
 * - passwordHash
 * - balance
 * - active
 *
 * Ngoài ra Admin có thêm quyền quản lý người dùng và phiên đấu giá.
 */
public class Admin extends User {

    // Danh sách id các user mà admin đã xử lý / quản lý
    private List<String> managedUserIds;

    // Danh sách id các phiên đấu giá mà admin đã can thiệp / quản lý
    private List<String> managedAuctionIds;

    // Cấp độ quản trị, ví dụ: SUPER_ADMIN, MODERATOR, SUPPORT_ADMIN
    private String adminLevel;

    /**
     * Constructor khởi tạo đối tượng Admin.
     *
     * @param username tên đăng nhập
     * @param email email của admin
     * @param passwordHash mật khẩu đã được băm
     * @param adminLevel cấp độ quản trị
     */
    public Admin(String username, String email, String passwordHash, String adminLevel) {
        super(username, email, passwordHash);

        if (adminLevel == null || adminLevel.isBlank()) {
            throw new IllegalArgumentException("ADMIN LEVEL MUST NOT BE EMPTY");
        }

        this.managedUserIds = new ArrayList<>();
        this.managedAuctionIds = new ArrayList<>();
        this.adminLevel = adminLevel;
    }

    /**
     * Trả về vai trò của user.
     */
    @Override
    public String getRole() {
        return "ADMIN";
    }

    /**
     * Thêm id của một user vào danh sách user mà admin quản lý.
     *
     * @param userId id của user
     */
    public void addManagedUser(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("USER ID MUST NOT BE EMPTY");
        }

        managedUserIds.add(userId);
    }

    /**
     * Thêm id của một phiên đấu giá vào danh sách auction mà admin quản lý.
     *
     * @param auctionId id của auction
     */
    public void addManagedAuction(String auctionId) {
        if (auctionId == null || auctionId.isBlank()) {
            throw new IllegalArgumentException("AUCTION ID MUST NOT BE EMPTY");
        }

        managedAuctionIds.add(auctionId);
    }

    /**
     * Trả về danh sách user id mà admin đã quản lý.
     * Trả về bản sao để tránh bị sửa trực tiếp từ bên ngoài.
     *
     * @return danh sách id user
     */
    public List<String> getManagedUserIds() {
        return new ArrayList<>(managedUserIds);
    }

    /**
     * Trả về danh sách auction id mà admin đã quản lý.
     * Trả về bản sao để tránh bị sửa trực tiếp từ bên ngoài.
     *
     * @return danh sách id auction
     */
    public List<String> getManagedAuctionIds() {
        return new ArrayList<>(managedAuctionIds);
    }

    /**
     * Lấy cấp độ quản trị của admin.
     *
     * @return cấp độ admin
     */
    public String getAdminLevel() {
        return adminLevel;
    }

    /**
     * Cập nhật cấp độ quản trị.
     *
     * @param adminLevel cấp độ mới
     */
    public void setAdminLevel(String adminLevel) {
        if (adminLevel == null || adminLevel.isBlank()) {
            throw new IllegalArgumentException("ADMIN LEVEL MUST NOT BE EMPTY");
        }

        this.adminLevel = adminLevel;
    }

    /**
     * Kiểm tra admin có đang quản lý user này hay không.
     *
     * @param userId id của user cần kiểm tra
     * @return true nếu có quản lý, ngược lại false
     */
    public boolean managesUser(String userId) {
        if (userId == null || userId.isBlank()) {
            return false;
        }
        return managedUserIds.contains(userId);
    }

    /**
     * Kiểm tra admin có đang quản lý auction này hay không.
     *
     * @param auctionId id của auction cần kiểm tra
     * @return true nếu có quản lý, ngược lại false
     */
    public boolean managesAuction(String auctionId) {
        if (auctionId == null || auctionId.isBlank()) {
            return false;
        }
        return managedAuctionIds.contains(auctionId);
    }

    /**
     * Vô hiệu hóa một user.
     * Method này gọi tới setActive(false) của đối tượng user.
     *
     * @param user user cần khóa
     */
    public void deactivateUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("USER MUST NOT BE NULL");
        }

        user.setActive(false);
        addManagedUser(user.getId());
    }

    /**
     * Kích hoạt lại một user.
     *
     * @param user user cần mở khóa
     */
    public void activateUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("USER MUST NOT BE NULL");
        }

        user.setActive(true);
        addManagedUser(user.getId());
    }

    /**
     * In thông tin admin.
     * Có thể dùng để debug hoặc hiển thị nhanh thông tin trong console.
     */
    @Override
    public void printInfo() {
        System.out.println("=== ADMIN INFO ===");
        System.out.println("ID               : " + getId());
        System.out.println("Username         : " + getUsername());
        System.out.println("Email            : " + getEmail());
        System.out.println("Balance          : " + getBalance() + " VND");
        System.out.println("Active           : " + isActive());
        System.out.println("Admin Level      : " + adminLevel);
        System.out.println("Managed Users    : " + managedUserIds.size());
        System.out.println("Managed Auctions : " + managedAuctionIds.size());
    }
}