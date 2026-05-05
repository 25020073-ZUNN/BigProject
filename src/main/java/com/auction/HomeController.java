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
import com.auction.service.NetworkService;
import com.auction.model.user.User;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
    private TextField idCardField;
    @FXML
    private TextField addressField;
    @FXML
    private PasswordField regPasswordField;
    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private TextField bidAmountField;
    @FXML
    private Label detailItemNameLabel;
    @FXML
    private Label dbStatusLabel;

    private final NetworkService networkService = NetworkService.getInstance();

    @FXML
    public void initialize() {
        updateLoginState();
        if (clockLabel != null) {
            initClock();
        }
        refreshDatabaseStatus();
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

        try {
            User user = networkService.login(username, password);
            UserSession.login(user);
            showInformation("Đăng nhập thành công", "Chào mừng " + user.getUsername() + " quay trở lại!");
            goToHome(event);
        } catch (Exception e) {
            showError("Lỗi đăng nhập", "Không thể đăng nhập qua server: " + e.getMessage());
        }
    }

    @FXML
    public void handleRegister(ActionEvent event) {
        String fullName = fullNameField.getText();
        String username = regUsernameField.getText();
        String phone = phoneField.getText();
        String email = emailField.getText();
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

        try {
            networkService.register(username, fullName, email, password, "USER");
            showInformation("Đăng ký thành công", "Tài khoản của bạn đã được tạo. Vui lòng đăng nhập.");
            goToLogin(event);
        } catch (Exception e) {
            showError("Lỗi đăng ký", "Không thể đăng ký qua server: " + e.getMessage());
        }
    }

    @FXML
    public void handleBid(ActionEvent event) {
        if (!UserSession.isLoggedIn()) {
            showError("Chưa đăng nhập", "Bạn cần đăng nhập trước khi đặt giá.");
            return;
        }

        try {
            List<Map<String, Object>> auctions = networkService.getAuctions();
            Map<String, Object> targetAuction = resolveTargetAuction(auctions);
            if (targetAuction == null) {
                showError("Không tìm thấy phiên", "Không xác định được tài sản để đặt giá.");
                return;
            }

            String amount = resolveBidAmount(targetAuction);
            Map<String, Object> result = networkService.placeBid(
                    String.valueOf(targetAuction.get("itemId")),
                    UserSession.getLoggedInUser().getUsername(),
                    amount
            );

            showInformation(
                    "Đặt giá thành công",
                    "Bạn đã đặt giá cho " + result.get("itemName")
                            + "\nGiá hiện tại mới: " + formatCurrency(String.valueOf(result.get("currentPrice"))) + " VND"
            );
        } catch (Exception e) {
            showError("Đặt giá thất bại", e.getMessage());
        }
    }

    @FXML
    public void handleSearch(ActionEvent event) {
        String query = searchField != null ? searchField.getText().trim().toLowerCase() : "";

        try {
            List<Map<String, Object>> auctions = networkService.getAuctions();
            List<Map<String, Object>> filtered = auctions.stream()
                    .filter(auction -> query.isBlank()
                            || String.valueOf(auction.getOrDefault("itemName", "")).toLowerCase().contains(query)
                            || String.valueOf(auction.getOrDefault("category", "")).toLowerCase().contains(query))
                    .toList();

            String summary = filtered.isEmpty()
                    ? "Không có tài sản nào khớp."
                    : filtered.stream()
                    .limit(5)
                    .map(auction -> "- " + auction.get("itemName") + " | " + formatCurrency(String.valueOf(auction.get("currentPrice"))) + " VND")
                    .reduce((a, b) -> a + "\n" + b)
                    .orElse("");

            showInformation(
                    "Kết quả tìm kiếm",
                    "Tìm thấy " + filtered.size() + " tài sản.\n" + summary
            );
        } catch (Exception e) {
            showError("Lỗi tìm kiếm", "Không thể lấy dữ liệu từ server: " + e.getMessage());
        }
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

    private void refreshDatabaseStatus() {
        if (dbStatusLabel == null) {
            return;
        }

        try {
            Map<String, Object> status = networkService.getDatabaseStatus();
            boolean available = Boolean.parseBoolean(String.valueOf(status.getOrDefault("available", false)));
            String dbUser = String.valueOf(status.getOrDefault("dbUser", "unknown"));
            String dbUrl = String.valueOf(status.getOrDefault("dbUrl", ""));
            String shortUrl = dbUrl.replaceFirst("^jdbc:mysql://", "");

            if (available) {
                dbStatusLabel.setText("Database: OK");
                dbStatusLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");
            } else {
                dbStatusLabel.setText("Database: Error");
                dbStatusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            }
        } catch (Exception e) {
            dbStatusLabel.setText("DB: Offline");
            dbStatusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
        }
    }

    private Map<String, Object> resolveTargetAuction(List<Map<String, Object>> auctions) {
        if (auctions.isEmpty()) {
            return null;
        }

        if (detailItemNameLabel != null && detailItemNameLabel.getText() != null && !detailItemNameLabel.getText().isBlank()) {
            String detailName = detailItemNameLabel.getText().trim().toLowerCase();
            Map<String, Object> matched = auctions.stream()
                    .filter(auction -> String.valueOf(auction.getOrDefault("itemName", "")).trim().toLowerCase().contains(detailName)
                            || detailName.contains(String.valueOf(auction.getOrDefault("itemName", "")).trim().toLowerCase()))
                    .findFirst()
                    .orElse(null);
            if (matched != null) {
                return matched;
            }
        }

        if (searchField != null && searchField.getText() != null && !searchField.getText().isBlank()) {
            String query = searchField.getText().trim().toLowerCase();
            Map<String, Object> matched = auctions.stream()
                    .filter(auction -> String.valueOf(auction.getOrDefault("itemName", "")).toLowerCase().contains(query))
                    .findFirst()
                    .orElse(null);
            if (matched != null) {
                return matched;
            }
        }

        return auctions.get(0);
    }

    private String resolveBidAmount(Map<String, Object> auction) {
        if (bidAmountField != null && bidAmountField.getText() != null && !bidAmountField.getText().isBlank()) {
            return normalizeAmount(bidAmountField.getText());
        }

        BigDecimal currentPrice = new BigDecimal(String.valueOf(auction.get("currentPrice")));
        BigDecimal nextPrice = currentPrice.add(new BigDecimal("50000000"));
        return nextPrice.toPlainString();
    }

    private String normalizeAmount(String text) {
        String normalized = text.replaceAll("[^\\d]", "");
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Số tiền đặt giá không hợp lệ.");
        }
        return normalized;
    }

    private String formatCurrency(String amount) {
        NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
        return formatter.format(new BigDecimal(amount));
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
