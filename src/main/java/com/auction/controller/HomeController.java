package com.auction.controller;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javafx.scene.control.PasswordField;
import javafx.scene.control.ComboBox;
import javafx.application.Platform;
import com.auction.network.client.NetworkService;
import com.auction.network.client.AuctionUpdateListener;
import com.auction.model.user.User;
import com.auction.util.UserSession;
import com.auction.util.ValidationUtil;
import com.auction.util.FxAsync;
import com.auction.util.SceneNavigator;
import com.auction.util.AlertHelper;
import com.auction.util.LoginStateHelper;
import com.auction.util.PriceFormatter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.io.IOException;

/**
 * HomeController - Bộ điều khiển chính cho giao diện người dùng.
 * Quản lý các tương tác trên trang chủ, đăng nhập, đăng ký, tìm kiếm và điều
 * hướng giữa các màn hình.
 */
public class HomeController {

    // --- Các thành phần UI được ánh xạ từ FXML ---

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

    // --- Các trường dữ liệu cho Đăng nhập ---
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;

    // --- Các trường dữ liệu cho Đăng ký ---
    @FXML
    private TextField fullNameField;
    @FXML
    private TextField regUsernameField;
    @FXML
    private TextField phoneField;
    @FXML
    private TextField emailField;
    @FXML
    private TextField addressField;
    @FXML
    private ComboBox<String> accountTypeComboBox;
    @FXML
    private PasswordField regPasswordField;
    @FXML
    private PasswordField confirmPasswordField;

    // --- Các thành phần cho Chi tiết tài sản và Đặt giá ---
    @FXML
    private TextField bidAmountField;
    @FXML
    private Label detailItemNameLabel;

    // Dịch vụ mạng để giao tiếp với Server
    private final NetworkService networkService = NetworkService.getInstance();
    private final List<Map<String, Object>> latestAuctions = new ArrayList<>();
    private final AuctionUpdateListener auctionUpdateListener = auctionData -> Platform.runLater(() -> {
        synchronized (latestAuctions) {
            int existingIndex = findAuctionIndex(String.valueOf(auctionData.get("auctionId")));
            if (existingIndex >= 0) {
                latestAuctions.set(existingIndex, auctionData);
            } else {
                latestAuctions.add(auctionData);
            }
        }
    });

    @FXML
    public void initialize() {
        updateLoginState();
        if (clockLabel != null)
            initClock();
        if (accountTypeComboBox != null) {
            accountTypeComboBox.getItems().setAll("BIDDER", "SELLER");
            accountTypeComboBox.setValue("BIDDER");
        }
        networkService.addAuctionUpdateListener(auctionUpdateListener);
        registerListenerLifecycle();
    }

    /**
     * Cập nhật giao diện dựa trên trạng thái đăng nhập của người dùng.
     */
    private void updateLoginState() {
        LoginStateHelper.updateLoginButton(loginButton);

        if (UserSession.isLoggedIn()) {
            User user = UserSession.getLoggedInUser();
            if (mainLoginButton != null) {
                mainLoginButton.setText("Chào mừng, " + user.getUsername());
                mainLoginButton.setOnAction(e -> AlertHelper.showInformation("Hồ sơ",
                        "Chào mừng " + user.getUsername() + " đến với hệ thống!"));
            }
            if (loginPanel != null && userPanel != null) {
                loginPanel.setVisible(false);
                loginPanel.setManaged(false);
                userPanel.setVisible(true);
                userPanel.setManaged(true);
                if (userNameLabel != null)
                    userNameLabel.setText(user.getUsername());
                if (userRoleLabel != null)
                    userRoleLabel.setText(user.getRole());
                if (userBalanceLabel != null) {
                    java.text.NumberFormat formatter = java.text.NumberFormat
                            .getInstance(new java.util.Locale("vi", "VN"));
                    userBalanceLabel.setText(formatter.format(user.getBalance()) + " VND");
                }
            }
        } else {
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
        LoginStateHelper.handleLogout(event);
    }

    @FXML
    public void handleLogin(ActionEvent event) {
        String username = usernameField != null ? usernameField.getText() : "";
        String password = passwordField != null ? passwordField.getText() : "";

        if (username.isEmpty() || password.isEmpty()) {
            AlertHelper.showError("Lỗi đăng nhập", "Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu.");
            return;
        }

        if (!ValidationUtil.isUsernameValid(username)) {
            AlertHelper.showError("Lỗi đăng nhập", "Tên đăng nhập không hợp lệ (3-16 ký tự, chỉ chữ và số).");
            return;
        }

        FxAsync.run(
                () -> networkService.login(username, password),
                user -> {
                    UserSession.login(user);
                    AlertHelper.showInformation("Đăng nhập thành công",
                            "Chào mừng " + user.getUsername() + " quay trở lại!");
                    SceneNavigator.goToHome(event);
                },
                errorMsg -> AlertHelper.showError("Lỗi đăng nhập", "Không thể đăng nhập: " + errorMsg));
    }

    @FXML
    public void handleRegister(ActionEvent event) {
        String fullName = fullNameField.getText();
        String username = regUsernameField.getText();
        String email = emailField.getText();
        String role = accountTypeComboBox != null ? accountTypeComboBox.getValue() : "BIDDER";
        String password = regPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (fullName.isEmpty() || username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            AlertHelper.showError("Lỗi đăng ký", "Vui lòng điền đầy đủ các thông tin bắt buộc.");
            return;
        }
        if (role == null || role.isBlank()) {
            AlertHelper.showError("Lỗi đăng ký", "Vui lòng chọn loại tài khoản.");
            return;
        }
        if (!password.equals(confirmPassword)) {
            AlertHelper.showError("Lỗi đăng ký", "Mật khẩu xác nhận không khớp.");
            return;
        }

        if (!ValidationUtil.isUsernameValid(username)) {
            AlertHelper.showError("Lỗi đăng ký", "Tên đăng nhập không hợp lệ (3-16 ký tự, chỉ chữ và số).");
            return;
        }

        if (!ValidationUtil.isEmailValid(email)) {
            AlertHelper.showError("Lỗi đăng ký", "Định dạng email không hợp lệ.");
            return;
        }

        if (!ValidationUtil.isPasswordValid(password)) {
            AlertHelper.showError("Lỗi đăng ký",
                    "Mật khẩu quá yếu (yêu cầu ít nhất 8 ký tự, có chữ hoa, chữ thường và số).");
            return;
        }

        FxAsync.run(
                () -> networkService.register(username, fullName, email, password, role),
                user -> {
                    AlertHelper.showInformation("Đăng ký thành công", "Tài khoản đã được tạo. Vui lòng đăng nhập.");
                    SceneNavigator.goToLogin(event);
                },
                errorMsg -> AlertHelper.showError("Lỗi đăng ký", "Không thể đăng ký: " + errorMsg));
    }

    @FXML
    public void handleBid(ActionEvent event) {
        if (!UserSession.isLoggedIn()) {
            AlertHelper.showError("Chưa đăng nhập", "Bạn cần đăng nhập trước khi đặt giá.");
            return;
        }

        FxAsync.run(
                () -> {
                    List<Map<String, Object>> auctions = getKnownAuctions();
                    Map<String, Object> targetAuction = resolveTargetAuction(auctions);
                    if (targetAuction == null)
                        throw new RuntimeException("Không xác định được tài sản.");
                    String amount = resolveBidAmount(targetAuction);
                    return networkService.placeBid(
                            String.valueOf(targetAuction.get("itemId")),
                            UserSession.getLoggedInUser().getUsername(),
                            amount);
                },
                result -> AlertHelper.showInformation("Đặt giá thành công",
                        "Bạn đã đặt giá cho " + result.get("itemName")
                                + "\nGiá hiện tại mới: "
                                + PriceFormatter.formatCurrency(String.valueOf(result.get("currentPrice"))) + " VND"),
                errorMsg -> AlertHelper.showError("Đặt giá thất bại", errorMsg));
    }

    @FXML
    public void handleSearch(ActionEvent event) {
        String query = searchField != null ? searchField.getText().trim().toLowerCase() : "";

        FxAsync.run(
                () -> {
                    List<Map<String, Object>> auctions = getKnownAuctions();
                    return auctions.stream()
                            .filter(auction -> query.isBlank()
                                    || String.valueOf(auction.getOrDefault("itemName", "")).toLowerCase()
                                            .contains(query)
                                    || String.valueOf(auction.getOrDefault("category", "")).toLowerCase()
                                            .contains(query))
                            .toList();
                },
                filtered -> {
                    String summary = filtered.isEmpty()
                            ? "Không có tài sản nào khớp."
                            : filtered.stream()
                                    .limit(5)
                                    .map(a -> "- " + a.get("itemName") + " | "
                                            + PriceFormatter.formatCurrency(String.valueOf(a.get("currentPrice")))
                                            + " VND")
                                    .reduce((a, b) -> a + "\n" + b)
                                    .orElse("");
                    AlertHelper.showInformation("Kết quả tìm kiếm",
                            "Tìm thấy " + filtered.size() + " tài sản.\n" + summary);
                },
                errorMsg -> AlertHelper.showError("Lỗi tìm kiếm", "Không thể lấy dữ liệu: " + errorMsg));
    }

    @FXML
    public void handleFollow(ActionEvent event) {
        AlertHelper.showInformation("Theo dõi", "Tài sản này đã được thêm vào danh sách quan tâm.");
    }

    @FXML
    public void handleComingSoon(ActionEvent event) {
        AlertHelper.showInformation("Tính năng sắp ra mắt", "Tính năng này hiện đang được hoàn thiện.");
    }

    @FXML
    public void handleSubscribe(ActionEvent event) {
        AlertHelper.showInformation("Đăng ký thành công", "Chúng tôi sẽ gửi bản tin qua email.");
    }

    // --- Logic nội bộ ---

    private Map<String, Object> resolveTargetAuction(List<Map<String, Object>> auctions) {
        if (auctions.isEmpty())
            return null;
        if (detailItemNameLabel != null && detailItemNameLabel.getText() != null
                && !detailItemNameLabel.getText().isBlank()) {
            String detailName = detailItemNameLabel.getText().trim().toLowerCase();
            Map<String, Object> matched = auctions.stream()
                    .filter(a -> String.valueOf(a.getOrDefault("itemName", "")).trim().toLowerCase()
                            .contains(detailName)
                            || detailName.contains(String.valueOf(a.getOrDefault("itemName", "")).trim().toLowerCase()))
                    .findFirst().orElse(null);
            if (matched != null)
                return matched;
        }
        if (searchField != null && searchField.getText() != null && !searchField.getText().isBlank()) {
            String query = searchField.getText().trim().toLowerCase();
            Map<String, Object> matched = auctions.stream()
                    .filter(a -> String.valueOf(a.getOrDefault("itemName", "")).toLowerCase().contains(query))
                    .findFirst().orElse(null);
            if (matched != null)
                return matched;
        }
        return auctions.get(0);
    }

    private String resolveBidAmount(Map<String, Object> auction) {
        if (bidAmountField != null && bidAmountField.getText() != null && !bidAmountField.getText().isBlank()) {
            String normalized = bidAmountField.getText().replaceAll("[^\\d]", "");
            if (normalized.isBlank())
                throw new IllegalArgumentException("Số tiền đặt giá không hợp lệ.");
            return normalized;
        }
        BigDecimal currentPrice = new BigDecimal(String.valueOf(auction.get("currentPrice")));
        return currentPrice.add(new BigDecimal("50000000")).toPlainString();
    }

    private List<Map<String, Object>> getKnownAuctions() throws IOException {
        synchronized (latestAuctions) {
            if (!latestAuctions.isEmpty())
                return new ArrayList<>(latestAuctions);
        }
        List<Map<String, Object>> auctions = networkService.getAuctions();
        synchronized (latestAuctions) {
            latestAuctions.clear();
            latestAuctions.addAll(auctions);
            return new ArrayList<>(latestAuctions);
        }
    }

    private int findAuctionIndex(String auctionId) {
        for (int i = 0; i < latestAuctions.size(); i++) {
            if (auctionId.equals(String.valueOf(latestAuctions.get(i).get("auctionId"))))
                return i;
        }
        return -1;
    }

    private void registerListenerLifecycle() {
        if (loginButton == null)
            return;
        loginButton.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (oldScene != null && newScene == null)
                networkService.removeAuctionUpdateListener(auctionUpdateListener);
        });
    }

    private void initClock() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss\ndd/MM/yyyy");
        Timeline clock = new Timeline(
                new KeyFrame(Duration.seconds(1), e -> clockLabel.setText(LocalDateTime.now().format(formatter))));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
        clockLabel.setText(LocalDateTime.now().format(formatter));
    }

    // --- Điều hướng (delegate tới SceneNavigator) ---
    @FXML
    public void goToHome(ActionEvent event) {
        SceneNavigator.goToHome(event);
    }

    @FXML
    public void goToAuctionList(ActionEvent event) {
        SceneNavigator.goToAuctionList(event);
    }

    @FXML
    public void goToLogin(ActionEvent event) {
        SceneNavigator.goToLogin(event);
    }

    @FXML
    public void goToRegister(ActionEvent event) {
        SceneNavigator.goToRegister(event);
    }

    @FXML
    public void goToProductDetail(ActionEvent event) {
        SceneNavigator.goToProductDetail(event);
    }

    @FXML
    public void goToSessions(ActionEvent event) {
        SceneNavigator.goToSessions(event);
    }

    @FXML
    public void goToNews(ActionEvent event) {
        SceneNavigator.goToNews(event);
    }

    @FXML
    public void goToContact(ActionEvent event) {
        SceneNavigator.goToContact(event);
    }

    @FXML
    public void goToCreateAuction(ActionEvent event) {
        SceneNavigator.goToCreateAuction(event);
    }
}
