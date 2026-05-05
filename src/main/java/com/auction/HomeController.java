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

/**
 * HomeController - Bộ điều khiển chính cho giao diện người dùng.
 * Quản lý các tương tác trên trang chủ, đăng nhập, đăng ký, tìm kiếm và điều hướng giữa các màn hình.
 */
public class HomeController {

    // --- Các thành phần UI được ánh xạ từ FXML ---
    
    @FXML
    private Label clockLabel; // Nhãn hiển thị đồng hồ thời gian thực

    @FXML
    private TextField searchField; // Ô nhập liệu tìm kiếm tài sản

    @FXML
    private Button loginButton; // Nút đăng nhập/đăng xuất trên thanh điều hướng

    @FXML
    private Button mainLoginButton; // Nút chào mừng người dùng (hiển thị khi đã đăng nhập)

    @FXML
    private javafx.scene.layout.VBox loginPanel; // Bảng điều khiển đăng nhập (bên trái giao diện chính)
    
    @FXML
    private javafx.scene.layout.VBox userPanel; // Bảng thông tin người dùng (hiển thị sau khi đăng nhập)
    
    @FXML
    private Label userNameLabel; // Nhãn hiển thị tên người dùng
    
    @FXML
    private Label userRoleLabel; // Nhãn hiển thị vai trò (Người dùng/Admin)
    
    @FXML
    private Label userBalanceLabel; // Nhãn hiển thị số dư tài khoản

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
    private TextField idCardField;
    @FXML
    private TextField addressField;
    @FXML
    private PasswordField regPasswordField;
    @FXML
    private PasswordField confirmPasswordField;

    // --- Các thành phần cho Chi tiết tài sản và Đặt giá ---
    @FXML
    private TextField bidAmountField; // Ô nhập số tiền đặt giá
    @FXML
    private Label detailItemNameLabel; // Nhãn tên tài sản đang xem chi tiết
    @FXML
    private Label dbStatusLabel; // Nhãn hiển thị trạng thái kết nối Database

    // Dịch vụ mạng để giao tiếp với Server
    private final NetworkService networkService = NetworkService.getInstance();

    /**
     * Phương thức khởi tạo tự động được gọi bởi JavaFX sau khi FXML được tải.
     */
    @FXML
    public void initialize() {
        // Cập nhật trạng thái đăng nhập (Hiển thị thông tin người dùng nếu đã đăng nhập)
        updateLoginState();
        
        // Khởi tạo đồng hồ nếu thành phần clockLabel tồn tại
        if (clockLabel != null) {
            initClock();
        }
        
        // Kiểm tra và hiển thị trạng thái kết nối cơ sở dữ liệu
        refreshDatabaseStatus();
    }

    /**
     * Cập nhật giao diện dựa trên trạng thái đăng nhập của người dùng.
     * Ẩn/hiện các bảng điều khiển và thay đổi văn bản trên các nút.
     */
    private void updateLoginState() {
        if (UserSession.isLoggedIn()) {
            User user = UserSession.getLoggedInUser();
            
            // Cập nhật nút đăng nhập trên Header thành nút đăng xuất
            if (loginButton != null) {
                loginButton.setText("Đăng xuất (" + user.getUsername() + ")");
                loginButton.setOnAction(this::handleLogout);
            }
            
            // Cập nhật nút chào mừng
            if (mainLoginButton != null) {
                mainLoginButton.setText("Chào mừng, " + user.getUsername());
                mainLoginButton.setOnAction(e -> showInformation("Hồ sơ", "Chào mừng " + user.getUsername() + " đến với hệ thống!"));
            }

            // Cập nhật bảng thông tin người dùng (áp dụng cho file giaodien.fxml)
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
            // Thiết lập trạng thái mặc định khi chưa đăng nhập
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

    /**
     * Xử lý sự kiện đăng xuất.
     */
    @FXML
    public void handleLogout(ActionEvent event) {
        UserSession.logout();
        showInformation("Đăng xuất thành công", "Hẹn gặp lại bạn!");
        
        // Quay về trang chủ sau khi đăng xuất để làm mới giao diện
        try {
            goToHome(event);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Hiển thị thông báo dạng Information.
     */
    private void showInformation(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Hiển thị thông báo lỗi dạng Error.
     */
    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Xử lý sự kiện khi người dùng nhấn nút Đăng nhập.
     */
    @FXML
    public void handleLogin(ActionEvent event) {
        String username = usernameField != null ? usernameField.getText() : "";
        String password = passwordField != null ? passwordField.getText() : "";

        if (username.isEmpty() || password.isEmpty()) {
            showError("Lỗi đăng nhập", "Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu.");
            return;
        }

        try {
            // Gọi NetworkService để xác thực với server
            User user = networkService.login(username, password);
            // Lưu thông tin phiên đăng nhập
            UserSession.login(user);
            showInformation("Đăng nhập thành công", "Chào mừng " + user.getUsername() + " quay trở lại!");
            // Chuyển về trang chủ
            goToHome(event);
        } catch (Exception e) {
            showError("Lỗi đăng nhập", "Không thể đăng nhập qua server: " + e.getMessage());
        }
    }

    /**
     * Xử lý sự kiện khi người dùng nhấn nút Đăng ký.
     */
    @FXML
    public void handleRegister(ActionEvent event) {
        // Lấy dữ liệu từ form
        String fullName = fullNameField.getText();
        String username = regUsernameField.getText();
        String phone = phoneField.getText();
        String email = emailField.getText();
        String idCard = idCardField.getText();
        String address = addressField.getText();
        String password = regPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // Kiểm tra dữ liệu đầu vào
        if (fullName.isEmpty() || username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showError("Lỗi đăng ký", "Vui lòng điền đầy đủ các thông tin bắt buộc (Họ tên, Tên đăng nhập, Email, Mật khẩu).");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError("Lỗi đăng ký", "Mật khẩu xác nhận không khớp.");
            return;
        }

        try {
            // Gửi yêu cầu đăng ký lên server qua NetworkService
            networkService.register(username, fullName, email, password, "USER");
            showInformation("Đăng ký thành công", "Tài khoản của bạn đã được tạo. Vui lòng đăng nhập.");
            // Chuyển sang màn hình đăng nhập
            goToLogin(event);
        } catch (Exception e) {
            showError("Lỗi đăng ký", "Không thể đăng ký qua server: " + e.getMessage());
        }
    }

    /**
     * Xử lý sự kiện đặt giá cho tài sản.
     */
    @FXML
    public void handleBid(ActionEvent event) {
        // Kiểm tra xem đã đăng nhập chưa
        if (!UserSession.isLoggedIn()) {
            showError("Chưa đăng nhập", "Bạn cần đăng nhập trước khi đặt giá.");
            return;
        }

        try {
            // Lấy danh sách các cuộc đấu giá hiện tại từ server
            List<Map<String, Object>> auctions = networkService.getAuctions();
            // Xác định tài sản mục tiêu người dùng đang xem hoặc tìm kiếm
            Map<String, Object> targetAuction = resolveTargetAuction(auctions);
            
            if (targetAuction == null) {
                showError("Không tìm thấy phiên", "Không xác định được tài sản để đặt giá.");
                return;
            }

            // Lấy số tiền người dùng nhập hoặc tính toán giá tiếp theo
            String amount = resolveBidAmount(targetAuction);
            
            // Gửi yêu cầu đặt giá lên server
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

    /**
     * Xử lý tìm kiếm tài sản đấu giá dựa trên từ khóa.
     */
    @FXML
    public void handleSearch(ActionEvent event) {
        String query = searchField != null ? searchField.getText().trim().toLowerCase() : "";

        try {
            // Lấy dữ liệu và lọc theo tên tài sản hoặc danh mục
            List<Map<String, Object>> auctions = networkService.getAuctions();
            List<Map<String, Object>> filtered = auctions.stream()
                    .filter(auction -> query.isBlank()
                            || String.valueOf(auction.getOrDefault("itemName", "")).toLowerCase().contains(query)
                            || String.valueOf(auction.getOrDefault("category", "")).toLowerCase().contains(query))
                    .toList();

            // Tạo chuỗi tóm tắt kết quả tìm kiếm
            String summary = filtered.isEmpty()
                    ? "Không có tài sản nào khớp."
                    : filtered.stream()
                    .limit(5) // Chỉ hiển thị tối đa 5 kết quả đầu tiên trong thông báo
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

    /**
     * Xử lý sự kiện "Theo dõi" tài sản.
     */
    @FXML
    public void handleFollow(ActionEvent event) {
        showInformation("Theo dõi", "Tài sản này đã được thêm vào danh sách quan tâm của bạn.");
    }

    /**
     * Thông báo cho các tính năng chưa hoàn thiện.
     */
    @FXML
    public void handleComingSoon(ActionEvent event) {
        showInformation("Tính năng sắp ra mắt", "Cảm ơn bạn quan tâm! Tính năng này hiện đang được hoàn thiện.");
    }

    /**
     * Xử lý đăng ký nhận bản tin qua email.
     */
    @FXML
    public void handleSubscribe(ActionEvent event) {
        showInformation("Đăng ký thành công", "Chúng tôi sẽ gửi các bản tin đấu giá mới nhất qua email của bạn.");
    }

    /**
     * Kiểm tra và cập nhật trạng thái kết nối tới cơ sở dữ liệu.
     */
    private void refreshDatabaseStatus() {
        if (dbStatusLabel == null) {
            return;
        }

        try {
            Map<String, Object> status = networkService.getDatabaseStatus();
            boolean available = Boolean.parseBoolean(String.valueOf(status.getOrDefault("available", false)));
            
            if (available) {
                dbStatusLabel.setText("Database: Connect");
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

    /**
     * Xác định tài sản mà người dùng đang muốn tương tác (đặt giá).
     * Ưu tiên tài sản đang hiển thị chi tiết hoặc tài sản khớp với từ khóa tìm kiếm.
     */
    private Map<String, Object> resolveTargetAuction(List<Map<String, Object>> auctions) {
        if (auctions.isEmpty()) {
            return null;
        }

        // Kiểm tra xem người dùng có đang xem trang chi tiết một sản phẩm cụ thể không
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

        // Kiểm tra theo từ khóa trong ô tìm kiếm
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

        // Mặc định chọn tài sản đầu tiên nếu không xác định được
        return auctions.get(0);
    }

    /**
     * Lấy số tiền đặt giá hợp lệ.
     */
    private String resolveBidAmount(Map<String, Object> auction) {
        // Nếu người dùng có nhập vào ô số tiền
        if (bidAmountField != null && bidAmountField.getText() != null && !bidAmountField.getText().isBlank()) {
            return normalizeAmount(bidAmountField.getText());
        }

        // Nếu không nhập, mặc định đặt giá bằng Giá hiện tại + 50,000,000 VND
        BigDecimal currentPrice = new BigDecimal(String.valueOf(auction.get("currentPrice")));
        BigDecimal nextPrice = currentPrice.add(new BigDecimal("50000000"));
        return nextPrice.toPlainString();
    }

    /**
     * Chuẩn hóa chuỗi tiền tệ (loại bỏ ký tự không phải số).
     */
    private String normalizeAmount(String text) {
        String normalized = text.replaceAll("[^\\d]", "");
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Số tiền đặt giá không hợp lệ.");
        }
        return normalized;
    }

    /**
     * Định dạng số tiền sang kiểu tiền tệ Việt Nam (VD: 1.000.000).
     */
    private String formatCurrency(String amount) {
        NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
        return formatter.format(new BigDecimal(amount));
    }

    /**
     * Khởi tạo đồng hồ hiển thị trên giao diện, cập nhật mỗi giây.
     */
    private void initClock() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss\ndd/MM/yyyy");
        Timeline clock = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            clockLabel.setText(LocalDateTime.now().format(formatter));
        }));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();

        // Cập nhật giá trị ban đầu
        clockLabel.setText(LocalDateTime.now().format(formatter));
    }

    /**
     * Phương thức tiện ích để chuyển đổi giữa các màn hình (Scenes).
     */
    private void switchScene(ActionEvent event, String fxmlFile) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/" + fxmlFile));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Lỗi chuyển trang", "Không thể tải giao diện: " + fxmlFile);
        }
    }

    // --- Các phương thức điều hướng (Navigation) ---

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
