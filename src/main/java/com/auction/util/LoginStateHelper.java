package com.auction.util;

import com.auction.model.user.User;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;

/**
 * Tiện ích quản lý trạng thái đăng nhập trên thanh điều hướng.
 * Cập nhật nút đăng nhập/đăng xuất cho tất cả các Controller.
 */
public final class LoginStateHelper {

    private LoginStateHelper() {}

    /**
     * Cập nhật nút đăng nhập/đăng xuất dựa trên trạng thái phiên.
     */
    public static void updateLoginButton(Button loginButton) {
        if (loginButton == null) return;

        if (UserSession.isLoggedIn()) {
            User user = UserSession.getLoggedInUser();
            loginButton.setText("Đăng xuất (" + user.getUsername() + ")");
            loginButton.setOnAction(event -> handleLogout(event));
        } else {
            loginButton.setText("Đăng nhập");
            loginButton.setOnAction(SceneNavigator::goToLogin);
        }
    }

    /**
     * Xử lý đăng xuất: xóa phiên và chuyển về trang chủ.
     */
    public static void handleLogout(ActionEvent event) {
        UserSession.logout();
        AlertHelper.showInformation("Đăng xuất thành công", "Hẹn gặp lại bạn!");
        SceneNavigator.goToHome(event);
    }
}
