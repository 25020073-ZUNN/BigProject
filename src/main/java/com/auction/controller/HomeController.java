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
 * HomeController - Bộ điều khiển chính cho giao diện người dùng (Trang chủ).
 * Quản lý các tương tác cốt lõi như đăng nhập, đăng ký, hiển thị thông tin người dùng,
 * đồng hồ hệ thống, tìm kiếm và điều hướng giữa các phân đoạn của ứng dụng.
 */
public class HomeController {

    // --- Các thành phần UI được ánh xạ từ FXML ---

    @FXML
    private Label clockLabel;               // Nhãn hiển thị đồng hồ thời gian thực
    @FXML
    private TextField searchField;          // Ô nhập liệu tìm kiếm tài sản
    @FXML
    private Button loginButton;             // Nút Đăng nhập/Đăng xuất trên thanh điều hướng
    @FXML
    private Button mainLoginButton;         // Nút chào mừng người dùng sau khi đăng nhập thành công
    @FXML
    private javafx.scene.layout.VBox loginPanel; // Bảng chứa form đăng nhập (ẩn khi đã đăng nhập)
    @FXML
    private javafx.scene.layout.VBox userPanel;  // Bảng chứa thông tin cá nhân (hiện khi đã đăng nhập)
    @FXML
    private Label userNameLabel;            // Nhãn hiển thị tên người dùng
    @FXML
    private Label userRoleLabel;            // Nhãn hiển thị vai trò (Bidder/Seller/Admin)
    @FXML
    private Label userBalanceLabel;         // Nhãn hiển thị số dư ví tiền

    // --- Các trường dữ liệu cho chức năng Đăng nhập ---
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private TextField visiblePasswordField; // Trường hiển thị mật khẩu dạng văn bản thuần
    @FXML
    private Button togglePasswordButton;    // Nút ẩn/hiện mật khẩu

    // --- Các trường dữ liệu cho chức năng Đăng ký ---
    @FXML
    private TextField fullNameField;
    @FXML
    private TextField regUsernameField;
    @FXML
    private TextField phoneField;
    @FXML
    private TextField emailField;
    @FXML
    private PasswordField regPasswordField;
    @FXML
    private TextField visibleRegPasswordField;
    @FXML
    private Button toggleRegPasswordButton;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private TextField visibleConfirmPasswordField;
    @FXML
    private Button toggleConfirmPasswordButton;

    // --- Các thành phần hỗ trợ chức năng nhanh (như đặt giá nhanh từ trang chủ) ---
    @FXML
    private TextField bidAmountField;
    @FXML
    private Label detailItemNameLabel;

    // --- Khởi tạo các dịch vụ ---
    private final NetworkService networkService = NetworkService.getInstance();
    private final List<Map<String, Object>> latestAuctions = new ArrayList<>();
    
    // Bộ lắng nghe cập nhật từ Server: Cập nhật danh sách tài sản đấu giá mới nhất vào bộ nhớ tạm
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

    /**
     * Phương thức khởi tạo tự động của JavaFX.
     * Thiết lập trạng thái ban đầu, đồng hồ, sự kiện ẩn/hiện mật khẩu và đăng ký nhận dữ liệu từ Server.
     */
    @FXML
    public void initialize() {
        updateLoginState(); // Kiểm tra session và cập nhật UI
        if (clockLabel != null)
            initClock();    // Chạy đồng hồ hệ thống
        setupPasswordToggles(); // Cấu hình nút xem mật khẩu
        
        // Đăng ký nhận thông báo thay đổi dữ liệu đấu giá
        networkService.addAuctionUpdateListener(auctionUpdateListener);
        registerListenerLifecycle(); // Tự động hủy đăng ký khi scene bị đóng
    }

    /**
     * Thiết lập cơ chế ẩn/hiện mật khẩu cho tất cả các form (Đăng nhập & Đăng ký).
     */
    private void setupPasswordToggles() {
        setupPasswordToggle(passwordField, visiblePasswordField, togglePasswordButton);
        setupPasswordToggle(regPasswordField, visibleRegPasswordField, toggleRegPasswordButton);
        setupPasswordToggle(confirmPasswordField, visibleConfirmPasswordField, toggleConfirmPasswordButton);
    }

    /**
     * Ràng buộc dữ liệu giữa trường mật khẩu ẩn và trường hiển thị văn bản.
     */
    private void setupPasswordToggle(PasswordField hiddenField, TextField visibleField, Button toggleButton) {
        if (hiddenField == null || visibleField == null || toggleButton == null) {
            return;
        }

        // Ràng buộc 2 chiều: nhập vào ô nào thì ô kia cũng cập nhật theo
        visibleField.textProperty().bindBidirectional(hiddenField.textProperty());
        setPasswordVisible(hiddenField, visibleField, toggleButton, false);
        
        toggleButton.setOnAction(event -> setPasswordVisible(
                hiddenField,
                visibleField,
                toggleButton,
                !visibleField.isVisible()
        ));
    }

    /**
     * Điều khiển việc hiển thị hoặc ẩn mật khẩu bằng cách thay đổi thuộc tính visible/managed.
     */
    private void setPasswordVisible(PasswordField hiddenField, TextField visibleField, Button toggleButton, boolean visible) {
        hiddenField.setVisible(!visible);
        hiddenField.setManaged(!visible);
        visibleField.setVisible(visible);
        visibleField.setManaged(visible);
        toggleButton.setText(visible ? "🙈" : "👁");
    }

    /**
     * Cập nhật giao diện dựa trên trạng thái đăng nhập của người dùng.
     * Ẩn form đăng nhập và hiện bảng thông tin tài khoản nếu đã đăng nhập thành công.
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
            // Hiển thị form đăng nhập nếu chưa có session
            if (loginPanel != null && userPanel != null) {
                loginPanel.setVisible(true);
                loginPanel.setManaged(true);
                userPanel.setVisible(false);
                userPanel.setManaged(false);
            }
        }
    }

    /**
     * Xử lý sự kiện đăng xuất người dùng.
     */
    @FXML
    public void handleLogout(ActionEvent event) {
        LoginStateHelper.handleLogout(event);
    }

    /**
     * Xử lý xác thực đăng nhập: kiểm tra tính hợp lệ và gửi yêu cầu tới server.
     */
    @FXML
    public void handleLogin(ActionEvent event) {
        String username = usernameField != null ? usernameField.getText() : "";
        String password = passwordField != null ? passwordField.getText() : "";

        if (username.isEmpty() || password.isEmpty()) {
            AlertHelper.showError("Lỗi đăng nhập", "Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu.");
            return;
        }

        // Kiểm tra định dạng tên đăng nhập (3-16 ký tự)
        if (!ValidationUtil.isUsernameValid(username)) {
            AlertHelper.showError("Lỗi đăng nhập", "Tên đăng nhập không hợp lệ (3-16 ký tự, chỉ chữ và số).");
            return;
        }

        // Chạy tác vụ mạng bất đồng bộ để tránh làm đơ giao diện
        FxAsync.run(
                () -> networkService.login(username, password),
                user -> {
                    UserSession.login(user); // Lưu thông tin người dùng vào session
                    AlertHelper.showInformation("Đăng nhập thành công",
                            "Chào mừng " + user.getUsername() + " quay trở lại!");
                    SceneNavigator.goToHome(event); // Quay về trang chủ
                },
                errorMsg -> AlertHelper.showError("Lỗi đăng nhập", "Không thể đăng nhập: " + errorMsg));
    }

    /**
     * Xử lý đăng ký tài khoản mới: kiểm tra dữ liệu và gửi lên server.
     */
    @FXML
    public void handleRegister(ActionEvent event) {
        String fullName = fullNameField.getText();
        String username = regUsernameField.getText();
        String email = emailField.getText();
        String password = regPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // Kiểm tra các ràng buộc dữ liệu cơ bản
        if (fullName.isEmpty() || username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            AlertHelper.showError("Lỗi đăng ký", "Vui lòng điền đầy đủ các thông tin bắt buộc.");
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

        // Thực hiện gửi yêu cầu đăng ký bất đồng bộ
        FxAsync.run(
                () -> networkService.register(username, fullName, email, password),
                user -> {
                    AlertHelper.showInformation("Đăng ký thành công", "Tài khoản đã được tạo. Vui lòng đăng nhập.");
                    SceneNavigator.goToLogin(event);
                },
                errorMsg -> AlertHelper.showError("Lỗi đăng ký", "Không thể đăng ký: " + errorMsg));
    }

    /**
     * Xử lý đặt giá nhanh từ trang chủ. 
     * Tự động xác định tài sản đang được xem hoặc tìm kiếm để đặt giá.
     */
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

    /**
     * Xử lý tìm kiếm tài sản đấu giá nhanh. 
     * Hiển thị kết quả dưới dạng danh sách rút gọn trong hộp thoại thông báo.
     */
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

    /**
     * Thêm tài sản vào danh sách quan tâm (Tính năng mẫu).
     */
    @FXML
    public void handleFollow(ActionEvent event) {
        AlertHelper.showInformation("Theo dõi", "Tài sản này đã được thêm vào danh sách quan tâm.");
    }

    /**
     * Xử lý cho các tính năng chưa hoàn thiện.
     */
    @FXML
    public void handleComingSoon(ActionEvent event) {
        AlertHelper.showInformation("Đang phát triển", "Tính năng này sẽ ra mắt trong bản cập nhật sau.");
    }

    @FXML
    public void goToUserProfile(ActionEvent event) {
        SceneNavigator.goToUserProfile(event);
    }

    @FXML
    public void handleSubscribe(ActionEvent event) {
        AlertHelper.showInformation("Đăng ký thành công", "Chúng tôi sẽ gửi bản tin qua email.");
    }

    // --- Các phương thức bổ trợ logic nội bộ ---

    /**
     * Tìm tài sản mục tiêu dựa trên tiêu đề hoặc từ khóa tìm kiếm hiện tại.
     */
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

    /**
     * Phân tích số tiền đặt giá từ trường nhập liệu hoặc tính toán mức giá tối thiểu.
     */
    private String resolveBidAmount(Map<String, Object> auction) {
        if (bidAmountField != null && bidAmountField.getText() != null && !bidAmountField.getText().isBlank()) {
            String normalized = bidAmountField.getText().replaceAll("[^\\d]", "");
            if (normalized.isBlank())
                throw new IllegalArgumentException("Số tiền đặt giá không hợp lệ.");
            return normalized;
        }
        // Giá trị mặc định (phục vụ mẫu)
        BigDecimal currentPrice = new BigDecimal(String.valueOf(auction.get("currentPrice")));
        return currentPrice.add(new BigDecimal("50000000")).toPlainString();
    }

    /**
     * Lấy danh sách phiên đấu giá hiện có, sử dụng cache nếu có hoặc tải mới từ server.
     */
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

    /**
     * Tìm chỉ số của một phiên đấu giá trong danh sách thông qua ID.
     */
    private int findAuctionIndex(String auctionId) {
        for (int i = 0; i < latestAuctions.size(); i++) {
            if (auctionId.equals(String.valueOf(latestAuctions.get(i).get("auctionId"))))
                return i;
        }
        return -1;
    }

    /**
     * Quản lý vòng đời của bộ lắng nghe: hủy đăng ký khi scene hiện tại bị hủy để tránh rò rỉ bộ nhớ.
     */
    private void registerListenerLifecycle() {
        if (loginButton == null)
            return;
        loginButton.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (oldScene != null && newScene == null)
                networkService.removeAuctionUpdateListener(auctionUpdateListener);
        });
    }

    /**
     * Khởi tạo đồng hồ hệ thống hiển thị giờ:phút:giây và ngày tháng.
     */
    private void initClock() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss\ndd/MM/yyyy");
        Timeline clock = new Timeline(
                new KeyFrame(Duration.seconds(1), e -> clockLabel.setText(LocalDateTime.now().format(formatter))));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
        clockLabel.setText(LocalDateTime.now().format(formatter));
    }

    // --- Điều hướng giao diện (Ủy thác qua SceneNavigator) ---
    
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
