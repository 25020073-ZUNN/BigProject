package com.auction.util;

import com.auction.model.user.User;

/**
 * UserSession - Lưu trữ thông tin phiên đăng nhập của người dùng.
 * Sử dụng pattern để truy cập người dùng hiện tại từ bất kỳ đâu trong ứng dụng.
 */
public class UserSession {
    private static User loggedInUser;

    /**
     * Đăng nhập người dùng vào hệ thống.
     * @param user Đối tượng người dùng đã xác thực.
     */
    public static void login(User user) {
        loggedInUser = user;
    }

    /**
     * Đăng xuất người dùng, xóa thông tin phiên.
     */
    public static void logout() {
        loggedInUser = null;
    }

    /**
     * Kiểm tra xem có người dùng nào đang đăng nhập không.
     * @return true nếu đã đăng nhập, ngược lại false.
     */
    public static boolean isLoggedIn() {
        return loggedInUser != null;
    }

    /**
     * Lấy đối tượng người dùng đang đăng nhập.
     * @return Đối tượng User hoặc null nếu chưa đăng nhập.
     */
    public static User getLoggedInUser() {
        return loggedInUser;
    }
}
