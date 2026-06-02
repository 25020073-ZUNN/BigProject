package com.auction.util;

import com.auction.model.user.User;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;

/**
 * LoginStateHelper
 *
 * Lớp tiện ích dùng để đồng bộ trạng thái nút
 * Đăng nhập / Đăng xuất trên toàn bộ giao diện.
 *
 * Tránh việc mỗi Controller phải tự viết lại logic:
 * - Kiểm tra đăng nhập
 * - Đổi text nút
 * - Gắn sự kiện Login/Logout
 */
public final class LoginStateHelper {

    /**
     * Utility Class
     *
     * Không cho tạo object:
     * new LoginStateHelper()
     */
    private LoginStateHelper() {}

    /**
     * Cập nhật trạng thái nút Login/Logout.
     *
     * Nếu đã đăng nhập:
     *      Hiển thị:
     *      Đăng xuất (username)
     *
     *      Gắn sự kiện Logout
     *
     * Nếu chưa đăng nhập:
     *      Hiển thị:
     *      Đăng nhập
     *
     *      Điều hướng tới màn hình Login
     */
    public static void updateLoginButton(Button loginButton) {

        // Tránh lỗi NullPointerException
        if (loginButton == null) {
            return;
        }

        /*
         * Kiểm tra xem hiện tại có người dùng
         * đang đăng nhập hay không.
         */
        if (UserSession.isLoggedIn()) {

            // Lấy user hiện tại từ Session
            User user = UserSession.getLoggedInUser();

            // Hiển thị tên người dùng trên nút Logout
            loginButton.setText(
                    "Đăng xuất (" + user.getUsername() + ")"
            );

            // Khi click sẽ thực hiện Logout
            loginButton.setOnAction(
                    event -> handleLogout(event)
            );

        } else {

            // Chưa đăng nhập
            loginButton.setText("Đăng nhập");

            /*
             * Method Reference
             *
             * Tương đương:
             * event -> SceneNavigator.goToLogin(event)
             */
            loginButton.setOnAction(
                    SceneNavigator::goToLogin
            );
        }
    }

    /**
     * Xử lý đăng xuất.
     *
     * Bước 1:
     *      Xóa Session hiện tại
     *
     * Bước 2:
     *      Hiển thị thông báo thành công
     *
     * Bước 3:
     *      Chuyển về trang chủ
     */
    public static void handleLogout(ActionEvent event) {

        // Xóa thông tin user đang đăng nhập
        UserSession.logout();

        // Hiển thị popup thông báo
        AlertHelper.showInformation(
                "Đăng xuất thành công",
                "Hẹn gặp lại bạn!"
        );

        // Điều hướng về Home Screen
        SceneNavigator.goToHome(event);
    }
}