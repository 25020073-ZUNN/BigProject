package com.auction.model.user;

import java.util.ArrayList;
import java.util.List;

/**
 * Lớp Admin đại diện cho quản trị viên.
 * Admin kế thừa từ User nên có đầy đủ các tính năng cơ bản, 
 * cộng thêm các quyền quản lý.
 */
public class Admin extends User {

    private List<String> managedUserIds = new ArrayList<>();
    private List<String> managedAuctionIds = new ArrayList<>();
    private String adminLevel;

    public Admin(String username, String email, String passwordHash, String adminLevel) {
        super(username, email, passwordHash);
        this.adminLevel = adminLevel;
    }

    @Override
    public String getRole() {
        return "ADMIN";
    }

    public void addManagedUser(String userId) {
        if (userId != null) managedUserIds.add(userId);
    }

    public void addManagedAuction(String auctionId) {
        if (auctionId != null) managedAuctionIds.add(auctionId);
    }

    public String getAdminLevel() { return adminLevel; }
    public void setAdminLevel(String adminLevel) { this.adminLevel = adminLevel; }

    public void deactivateUser(User user) {
        if (user != null) {
            user.setActive(false);
            addManagedUser(user.getId());
        }
    }

    public void activateUser(User user) {
        if (user != null) {
            user.setActive(true);
            addManagedUser(user.getId());
        }
    }

    @Override
    public void printInfo() {
        super.printInfo();
        System.out.println("Admin Level: " + adminLevel);
    }
}
