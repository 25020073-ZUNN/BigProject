package com.auction.controller;

import com.auction.model.user.User;
import com.auction.network.client.NetworkService;
import com.auction.util.AlertHelper;
import com.auction.util.LoginStateHelper;
import com.auction.util.PriceFormatter;
import com.auction.util.SceneNavigator;
import com.auction.util.UserSession;
import com.auction.util.FxAsync;
import com.auction.util.ThemeManager;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.IOException;

/**
 * Controller cho màn hình "Thông tin cá nhân" (User Profile).
 * Nhiệm vụ chính:
 * - Hiển thị thông tin chi tiết của người dùng đang đăng nhập (Tên, vai trò, số dư).
 * - Cho phép người dùng cập nhật Họ tên và Email.
 * - Cung cấp chức năng xóa tài khoản vĩnh viễn khỏi hệ thống.
 * - Điều hướng giữa các phân đoạn khác của ứng dụng.
 */
public class UserProfileController {

    // Dịch vụ mạng để tương tác với Server
    private final NetworkService networkService = NetworkService.getInstance();

    // --- Các thành phần hiển thị thông tin (Label) ---
    @FXML private Label lblUsername;    // Hiển thị tên đăng nhập (không cho sửa)
    @FXML private Label lblRole;        // Hiển thị vai trò người dùng (Bidder/Seller)
    @FXML private Label lblBalance;     // Hiển thị số dư tài khoản (đã định dạng tiền tệ)
    
    // --- Các trường nhập liệu để chỉnh sửa (TextField) ---
    @FXML private TextField txtUsername; // Tên đăng nhập (ở phần chỉnh sửa)
    @FXML private TextField txtFullName; // Họ và tên đầy đủ
    @FXML private TextField txtEmail;    // Địa chỉ email
    
    @FXML private Label lblNotification; // Nhãn hiển thị thông báo kết quả thao tác (Thành công/Lỗi)
    @FXML private Button themeToggleBtn; // Nút chuyển đổi giao diện sáng/tối
    @FXML private Button loginButton;    // Nút Đăng nhập/Đăng xuất trên thanh menu

    /**
     * Phương thức khởi tạo tự động của JavaFX.
     * Thiết lập trạng thái ban đầu cho các nút và tải dữ liệu người dùng.
     */
    @FXML
    public void initialize() {
        // Cập nhật trạng thái nút Login/Logout dựa trên session
        LoginStateHelper.updateLoginButton(loginButton);
        // Tải thông tin người dùng lên giao diện
        loadUserProfile();
        refreshLoggedInUser();
        updateThemeButton();
    }

    /**
     * Lấy thông tin người dùng từ session hiện tại và điền vào các trường trên giao diện.
     * Nếu chưa đăng nhập, sẽ yêu cầu người dùng đăng nhập lại.
     */
    private void loadUserProfile() {
        if (!UserSession.isLoggedIn()) {
            Platform.runLater(() -> {
                AlertHelper.showError("Lỗi", "Vui lòng đăng nhập để xem thông tin.");
                SceneNavigator.goToLogin(new ActionEvent(loginButton, null));
            });
            return;
        }

        // Lấy đối tượng User từ session
        User user = UserSession.getLoggedInUser();
        
        // Cập nhật các nhãn thông tin tóm tắt
        lblUsername.setText(user.getUsername());
        lblRole.setText("Vai trò: " + user.getRole());
        lblBalance.setText(PriceFormatter.formatPrice(java.math.BigDecimal.valueOf(user.getBalance())));

        // Điền dữ liệu vào form chỉnh sửa
        txtUsername.setText(user.getUsername());
        txtFullName.setText(user.getFullname());
        txtEmail.setText(user.getEmail());
    }

    private void refreshLoggedInUser() {
        if (!UserSession.isLoggedIn()) {
            return;
        }

        String username = UserSession.getLoggedInUser().getUsername();
        FxAsync.run(
                () -> networkService.getCurrentUser(username),
                refreshedUser -> {
                    UserSession.login(refreshedUser);
                    loadUserProfile();
                    LoginStateHelper.updateLoginButton(loginButton);
                },
                ignored -> {});
    }

    /**
     * Xử lý khi người dùng nhấn nút "Cập nhật thông tin".
     * Thực hiện gửi yêu cầu cập nhật lên server thông qua NetworkService.
     */
    @FXML
    public void handleUpdateProfile() {
        if (!UserSession.isLoggedIn()) return;
        
        String username = UserSession.getLoggedInUser().getUsername();
        String newFullName = txtFullName.getText().trim();
        String newEmail = txtEmail.getText().trim();

        // Kiểm tra tính hợp lệ cơ bản của dữ liệu
        if (newFullName.isEmpty() || newEmail.isEmpty()) {
            lblNotification.setText("Họ tên và Email không được để trống!");
            lblNotification.setStyle("-fx-text-fill: #e74c3c;"); // Màu đỏ thông báo lỗi
            return;
        }

        // Thực hiện cập nhật bất đồng bộ
        FxAsync.run(
            () -> networkService.updateProfile(username, newFullName, newEmail),
            updatedUser -> {
                // Cập nhật lại thông tin mới vào session sau khi server phản hồi thành công
                UserSession.login(updatedUser); 
                lblNotification.setText("Cập nhật thông tin thành công!");
                lblNotification.setStyle("-fx-text-fill: #2ecc71;"); // Màu xanh thông báo thành công
                loadUserProfile(); // Vẽ lại giao diện với thông tin mới
            },
            errorMsg -> {
                lblNotification.setText("Lỗi: " + errorMsg);
                lblNotification.setStyle("-fx-text-fill: #e74c3c;");
            }
        );
    }

    /**
     * Xử lý khi người dùng yêu cầu xóa tài khoản.
     * Cần xác nhận qua hộp thoại trước khi thực hiện xóa vĩnh viễn.
     */
    @FXML
    public void handleDeleteAccount(ActionEvent event) {
        if (!UserSession.isLoggedIn()) return;
        
        // Hiển thị hộp thoại xác nhận (Confirmation Dialog)
        boolean confirmed = AlertHelper.showConfirmation("Xác nhận xóa tài khoản", 
            "Hành động này không thể hoàn tác. Bạn có chắc chắn muốn xóa tài khoản này không?");
            
        if (!confirmed) return;

        String username = UserSession.getLoggedInUser().getUsername();
        
        // Thực hiện gửi yêu cầu xóa lên server
        FxAsync.run(
            () -> {
                networkService.deleteAccount(username);
                return true;
            },
            success -> {
                AlertHelper.showInformation("Thành công", "Tài khoản của bạn đã được xóa khỏi hệ thống.");
                UserSession.logout(); // Đăng xuất sau khi xóa thành công
                SceneNavigator.goToHome(event); // Chuyển về trang chủ
            },
            errorMsg -> {
                AlertHelper.showError("Lỗi", "Không thể xóa tài khoản: " + errorMsg);
            }
        );
    }

    /**
     * Xử lý đăng xuất (Sử dụng LoginStateHelper).
     */
    @FXML public void handleLogout(ActionEvent event) { LoginStateHelper.handleLogout(event); }

    @FXML
    public void toggleTheme(ActionEvent event) {
        javafx.scene.Scene scene = ((Node) event.getSource()).getScene();
        ThemeManager.getInstance().toggleTheme(scene);
        updateThemeButton();
    }

    /*private void updateThemeButton() {
        if (themeToggleBtn != null) {
            themeToggleBtn.setText(ThemeManager.getInstance().isDarkMode() ? "☀️" : "🌙");
        }
    }*/
    /*sua thành */
    private void updateThemeButton() {
        if (themeToggleBtn != null) {
            themeToggleBtn.setText(
                    ThemeManager.getInstance().isDarkMode()
                            ? "☀"
                            : "\uD83C\uDF19"
            );
        }
    }

    // --- Các phương thức điều hướng Sidebar ---
    @FXML public void goToHome(ActionEvent event) { SceneNavigator.goToHome(event); }
    @FXML public void goToAuctionList(ActionEvent event) { SceneNavigator.goToAuctionList(event); }
    @FXML public void goToSessions(ActionEvent event) { SceneNavigator.goToSessions(event); }
    @FXML public void goToNews(ActionEvent event) { SceneNavigator.goToNews(event); }
    @FXML public void goToContact(ActionEvent event) { SceneNavigator.goToContact(event); }
}
