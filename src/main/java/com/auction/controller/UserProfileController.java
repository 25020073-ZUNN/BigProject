package com.auction.controller;

import com.auction.model.user.User;
import com.auction.network.client.NetworkService;
import com.auction.util.AlertHelper;
import com.auction.util.LoginStateHelper;
import com.auction.util.PriceFormatter;
import com.auction.util.SceneNavigator;
import com.auction.util.UserSession;
import com.auction.util.FxAsync;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.IOException;

public class UserProfileController {

    private final NetworkService networkService = NetworkService.getInstance();

    @FXML private Label lblUsername;
    @FXML private Label lblRole;
    @FXML private Label lblBalance;
    
    @FXML private TextField txtUsername;
    @FXML private TextField txtFullName;
    @FXML private TextField txtEmail;
    
    @FXML private Label lblNotification;
    @FXML private Button loginButton;

    @FXML
    public void initialize() {
        LoginStateHelper.updateLoginButton(loginButton);
        loadUserProfile();
    }

    private void loadUserProfile() {
        if (!UserSession.isLoggedIn()) {
            Platform.runLater(() -> {
                AlertHelper.showError("Lỗi", "Vui lòng đăng nhập để xem thông tin.");
                SceneNavigator.goToLogin(new ActionEvent(loginButton, null));
            });
            return;
        }

        User user = UserSession.getLoggedInUser();
        lblUsername.setText(user.getUsername());
        lblRole.setText("Vai trò: " + user.getRole());
        lblBalance.setText(PriceFormatter.formatPrice(java.math.BigDecimal.valueOf(user.getBalance())));

        txtUsername.setText(user.getUsername());
        txtFullName.setText(user.getFullname());
        txtEmail.setText(user.getEmail());
    }

    @FXML
    public void handleUpdateProfile() {
        if (!UserSession.isLoggedIn()) return;
        
        String username = UserSession.getLoggedInUser().getUsername();
        String newFullName = txtFullName.getText().trim();
        String newEmail = txtEmail.getText().trim();

        if (newFullName.isEmpty() || newEmail.isEmpty()) {
            lblNotification.setText("Họ tên và Email không được để trống!");
            return;
        }

        FxAsync.run(
            () -> networkService.updateProfile(username, newFullName, newEmail),
            updatedUser -> {
                UserSession.login(updatedUser); // Cập nhật lại thông tin trong session
                lblNotification.setText("Cập nhật thông tin thành công!");
                lblNotification.setStyle("-fx-text-fill: #2ecc71;");
                loadUserProfile(); // Cập nhật lại UI
            },
            errorMsg -> {
                lblNotification.setText("Lỗi: " + errorMsg);
                lblNotification.setStyle("-fx-text-fill: #e74c3c;");
            }
        );
    }

    @FXML
    public void handleDeleteAccount(ActionEvent event) {
        if (!UserSession.isLoggedIn()) return;
        
        boolean confirmed = AlertHelper.showConfirmation("Xác nhận xóa tài khoản", 
            "Hành động này không thể hoàn tác. Bạn có chắc chắn muốn xóa tài khoản này không?");
            
        if (!confirmed) return;

        String username = UserSession.getLoggedInUser().getUsername();
        
        FxAsync.run(
            () -> {
                networkService.deleteAccount(username);
                return true;
            },
            success -> {
                AlertHelper.showInformation("Thành công", "Tài khoản của bạn đã được xóa khỏi hệ thống.");
                UserSession.logout();
                SceneNavigator.goToHome(event);
            },
            errorMsg -> {
                AlertHelper.showError("Lỗi", "Không thể xóa tài khoản: " + errorMsg);
            }
        );
    }

    @FXML public void handleLogout(ActionEvent event) { LoginStateHelper.handleLogout(event); }
    @FXML public void goToHome(ActionEvent event) { SceneNavigator.goToHome(event); }
    @FXML public void goToAuctionList(ActionEvent event) { SceneNavigator.goToAuctionList(event); }
    @FXML public void goToSessions(ActionEvent event) { SceneNavigator.goToSessions(event); }
    @FXML public void goToNews(ActionEvent event) { SceneNavigator.goToNews(event); }
    @FXML public void goToContact(ActionEvent event) { SceneNavigator.goToContact(event); }
}
