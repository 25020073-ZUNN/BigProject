package com.auction;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javafx.scene.control.PasswordField;
import javafx.scene.control.ComboBox;
import com.uet.service.UserService;
import com.uet.bidding.model.user.Bidder;
import com.uet.bidding.model.user.Seller;
import com.uet.bidding.model.user.User;

public class HomeController {

    @FXML
    private Label clockLabel;

    @FXML
    private TextField searchField;

    @FXML
    private Button loginButton;

    @FXML
    private Button mainLoginButton;

    @FXML
    private javafx.scene.layout.VBox loginPanel;
    @FXML
    private javafx.scene.layout.VBox userPanel;
    @FXML
    private Label userNameLabel;
    @FXML
    private Label userRoleLabel;
    @FXML
    private Label userBalanceLabel;

    // Login fields
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;

    // Register fields
    @FXML
    private TextField fullNameField;
    @FXML
    private TextField regUsernameField;
    @FXML
    private TextField phoneField;
    @FXML
    private TextField emailField;
    @FXML
    private ComboBox<String> accountTypeComboBox;
    @FXML
    private TextField idCardField;
    @FXML
    private TextField addressField;
    @FXML
    private PasswordField regPasswordField;
    @FXML
    private PasswordField confirmPasswordField;

    private final UserService userService = UserService.getInstance();

    @FXML
    public void initialize() {
        updateLoginState();
        if (clockLabel != null) {
            initClock();
        }
        if (accountTypeComboBox != null) {
            accountTypeComboBox.getItems().addAll("Người mua (Bidder)", "Người bán (Seller)");
            accountTypeComboBox.getSelectionModel().selectFirst();
        }
    }

    private void updateLoginState() {
        if (UserSession.isLoggedIn()) {
            User user = UserSession.getLoggedInUser();
            if (loginButton != null) {
                loginButton.setText("Đăng xuất (" + user.getUsername() + ")");
                loginButton.setOnAction(this::handleLogout);
            }
            if (mainLoginButton != null) {
                mainLoginButton.setText("Chào mừng, " + user.getUsername());
                mainLoginButton.setOnAction(e -> showInformation("Hồ sơ", "Chào mừng " + user.getUsername() + " đến với hệ thống!"));
            }

            // Update side panel if it exists (only in giaodien.fxml)
            if (loginPanel != null && userPanel != null) {
                loginPanel.setVisible(false);
                loginPanel.setManaged(false);
                userPanel.setVisible(true);
                userPanel.setManaged(true);
                
                if (userNameLabel != null) userNameLabel.setText(user.getUsername());
                if (userRoleLabel != null) userRoleLabel.setText(user.getRole());
                if (userBalanceLabel != null) {
                    java.text.NumberFormat formatter = java.text.NumberFormat.getInstance(new java.util.Locale("vi", "VN"));
                    userBalanceLabel.setText(formatter.format(user.getBalance()) + " VND");
                }
            }
        } else {
            // Reset to default state if not logged in
            if (loginButton != null) {
                loginButton.setText("Đăng nhập");
                loginButton.setOnAction(this::goToLogin);
            }
            if (loginPanel != null && userPanel != null) {
                loginPanel.setVisible(true);
                loginPanel.setManaged(true);
                userPanel.setVisible(false);
                userPanel.setManaged(false);
            }
        }
    }

    @FXML
    public void handleLogout(ActionEvent event) {
        UserSession.logout();
        showInformation("Đăng xuất thành công", "Hẹn gặp lại bạn!");
        
        // Refresh the current scene to update UI
        try {
            // Get current FXML by checking the scene's root if possible, 
            // but since we usually switch by name, let's just go back to home 
            // or stay on page by reloading giaodien if we are there.
            // For simplicity and to avoid crash, go to Home.
            goToHome(event);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showInformation(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    public void handleLogin(ActionEvent event) {
        String username = usernameField != null ? usernameField.getText() : "";
        String password = passwordField != null ? passwordField.getText() : "";

        if (username.isEmpty() || password.isEmpty()) {
            showError("Lỗi đăng nhập", "Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu.");
            return;
        }

        userService.login(username, password).ifPresentOrElse(
            user -> {
                UserSession.login(user);
                showInformation("Đăng nhập thành công", "Chào mừng " + user.getUsername() + " quay trở lại!");
                goToHome(event);
            },
            () -> {
                showError("Lỗi đăng nhập", "Tên đăng nhập hoặc mật khẩu không chính xác.");
            }
        );
    }

    @FXML
    public void handleRegister(ActionEvent event) {
        String fullName = fullNameField.getText();
        String username = regUsernameField.getText();
        String phone = phoneField.getText();
        String email = emailField.getText();
        String accountType = accountTypeComboBox.getValue();
        String idCard = idCardField.getText();
        String address = addressField.getText();
        String password = regPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (fullName.isEmpty() || username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showError("Lỗi đăng ký", "Vui lòng điền đầy đủ các thông tin bắt buộc (Họ tên, Tên đăng nhập, Email, Mật khẩu).");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError("Lỗi đăng ký", "Mật khẩu xác nhận không khớp.");
            return;
        }

        // Hash password (User.verifyPassword expects String.valueOf(password.hashCode()))
        String passwordHash = String.valueOf(password.hashCode());

        User newUser;
        if (accountType != null && accountType.contains("Seller")) {
            newUser = new Seller(username, email, passwordHash);
        } else {
            newUser = new Bidder(username, email, passwordHash);
        }

        if (userService.register(newUser)) {
            showInformation("Đăng ký thành công", "Tài khoản của bạn đã được tạo. Vui lòng đăng nhập.");
            goToLogin(event);
        } else {
            showError("Lỗi đăng ký", "Tên đăng nhập hoặc email đã tồn tại.");
        }
    }
    @FXML
    public void handleBid(ActionEvent event) {
        showInformation("Đặt giá thành công", "Bạn đã đặt giá thành công cho tài sản này!\nChúng tôi sẽ thông báo cho bạn nếu có người đặt giá cao hơn.");
    }

    @FXML
    public void handleSearch(ActionEvent event) {
        String query = (searchField != null) ? searchField.getText() : "";
        showInformation("Tìm kiếm tài sản", "Đang lọc danh sách tài sản với từ khóa: " + (query.isEmpty() ? "Tất cả" : query));
    }

    @FXML
    public void handleFollow(ActionEvent event) {
        showInformation("Theo dõi", "Tài sản này đã được thêm vào danh sách quan tâm của bạn.");
    }

    @FXML
    public void handleComingSoon(ActionEvent event) {
        showInformation("Tính năng sắp ra mắt", "Cảm ơn bạn quan tâm! Tính năng này hiện đang được hoàn thiện.");
    }

    @FXML
    public void handleSubscribe(ActionEvent event) {
        showInformation("Đăng ký thành công", "Chúng tôi sẽ gửi các bản tin đấu giá mới nhất qua email của bạn.");
    }

    private void initClock() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss\ndd/MM/yyyy");
        Timeline clock = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            clockLabel.setText(LocalDateTime.now().format(formatter));
        }));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();

        // Cập nhật ngay lập tức khi khởi tạo
        clockLabel.setText(LocalDateTime.now().format(formatter));
    }

    private void switchScene(ActionEvent event, String fxmlFile) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/" + fxmlFile));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void goToHome(ActionEvent event) {
        switchScene(event, "giaodien.fxml");
    }

    @FXML
    public void goToAuctionList(ActionEvent event) {
        switchScene(event, "auction-detail.fxml");
    }

    @FXML
    public void goToLogin(ActionEvent event) {
        switchScene(event, "login.fxml");
    }

    @FXML
    public void goToRegister(ActionEvent event) {
        switchScene(event, "register.fxml");
    }

    @FXML
    public void goToProductDetail(ActionEvent event) {
        switchScene(event, "product-detail.fxml");
    }

    @FXML
    public void goToSessions(ActionEvent event) {
        switchScene(event, "sessions.fxml");
    }

    @FXML
    public void goToNews(ActionEvent event) {
        switchScene(event, "news.fxml");
    }

    @FXML
    public void goToContact(ActionEvent event) {
        switchScene(event, "contact.fxml");
    }
}
