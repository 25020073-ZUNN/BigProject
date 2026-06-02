package com.auction.controller;

import com.auction.network.client.NetworkService;
import com.auction.util.AlertHelper;
import com.auction.util.FxAsync;
import com.auction.util.LoginStateHelper;
import com.auction.util.PriceFormatter;
import com.auction.util.SceneNavigator;
import com.auction.util.UserSession;
import com.auction.util.ThemeManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Lớp điều khiển (Controller) cho trang tổng quan của quản trị viên (Admin Dashboard).
 * Xử lý các tương tác người dùng và hiển thị dữ liệu quản lý người dùng, đấu giá và sản phẩm.
 */
public class AdminDashboardController {

    // Các thành phần UI được tiêm (injected) từ file FXML
    @FXML private Label adminNameLabel; // Nhãn hiển thị tên quản trị viên
    @FXML private Label totalUsersLabel; // Nhãn hiển thị tổng số người dùng
    @FXML private Label activeUsersLabel; // Nhãn hiển thị số người dùng đang hoạt động
    @FXML private Label totalAuctionsLabel; // Nhãn hiển thị tổng số phiên đấu giá
    @FXML private Label runningAuctionsLabel; // Nhãn hiển thị số phiên đấu giá đang diễn ra
    
    // Bảng quản lý người dùng
    @FXML private TableView<Map<String, Object>> usersTable; // Bảng hiển thị danh sách người dùng
    @FXML private TableColumn<Map<String, Object>, String> usernameColumn; // Cột tên đăng nhập
    @FXML private TableColumn<Map<String, Object>, String> fullNameColumn; // Cột họ và tên
    @FXML private TableColumn<Map<String, Object>, String> emailColumn; // Cột email
    @FXML private TableColumn<Map<String, Object>, String> roleColumn; // Cột vai trò (ADMIN, BIDDER, SELLER)
    @FXML private TableColumn<Map<String, Object>, String> statusColumn; // Cột trạng thái (Đang hoạt động/Đã khóa)
    @FXML private TextField searchUserField; // Trường nhập để tìm kiếm người dùng

    // Bảng quản lý đấu giá & sản phẩm
    @FXML private TableView<Map<String, Object>> auctionsTable; // Bảng hiển thị danh sách phiên đấu giá
    @FXML private TableColumn<Map<String, Object>, String> auctionIdColumn; // Cột ID phiên đấu giá
    @FXML private TableColumn<Map<String, Object>, String> productNameColumn; // Cột tên sản phẩm
    @FXML private TableColumn<Map<String, Object>, String> categoryColumn; // Cột danh mục sản phẩm
    @FXML private TableColumn<Map<String, Object>, String> sellerColumn; // Cột tên người bán
    @FXML private TableColumn<Map<String, Object>, String> currentPriceColumn; // Cột giá hiện tại
    @FXML private TableColumn<Map<String, Object>, String> auctionStatusColumn; // Cột trạng thái đấu giá (Đang diễn ra/Đã kết thúc/Sắp diễn ra)
    @FXML private TextField searchAuctionField; // Trường nhập để tìm kiếm phiên đấu giá

    @FXML private Label notificationLabel; // Nhãn hiển thị thông báo
    @FXML private Button themeToggleBtn; // Nút chuyển đổi giao diện (sáng/tối)
    @FXML private Button loginButton; // Nút đăng nhập/đăng xuất

    // Các dịch vụ và danh sách dữ liệu
    private final NetworkService networkService = NetworkService.getInstance(); // Dịch vụ mạng để giao tiếp với server
    private final ObservableList<Map<String, Object>> users = FXCollections.observableArrayList(); // Danh sách người dùng quan sát được
    private final ObservableList<Map<String, Object>> auctions = FXCollections.observableArrayList(); // Danh sách phiên đấu giá quan sát được

    /**
     * Phương thức khởi tạo được gọi tự động sau khi FXML được tải.
     * Thiết lập trạng thái ban đầu của giao diện người dùng và tải dữ liệu.
     */
    @FXML
    public void initialize() {
        LoginStateHelper.updateLoginButton(loginButton); // Cập nhật trạng thái nút đăng nhập
        if (!isAdminLoggedIn()) { // Kiểm tra xem quản trị viên đã đăng nhập chưa
            AlertHelper.showError("Không có quyền", "Bạn cần đăng nhập bằng tài khoản quản trị."); // Hiển thị thông báo lỗi
            return; // Dừng xử lý nếu không phải quản trị viên
        }

        // Hiển thị tên quản trị viên hiện tại
        adminNameLabel.setText(UserSession.getLoggedInUser().getUsername());
        
        configureUsersTable(); // Cấu hình các cột cho bảng người dùng
        configureAuctionsTable(); // Cấu hình các cột cho bảng đấu giá
        
        setupFiltering(); // Thiết lập chức năng lọc dữ liệu trực tiếp cho cả hai bảng
        
        loadDashboardData(); // Tải dữ liệu ban đầu cho trang tổng quan
        updateThemeButton(); // Cập nhật biểu tượng nút chuyển đổi giao diện
    }

    /**
     * Cấu hình cách dữ liệu được hiển thị trong bảng người dùng.
     * Liên kết các cột với các thuộc tính tương ứng từ đối tượng người dùng.
     */
    private void configureUsersTable() {
        usernameColumn.setCellValueFactory(data -> text(data.getValue().get("username"))); // Lấy username
        fullNameColumn.setCellValueFactory(data -> text(data.getValue().get("fullName"))); // Lấy fullName
        emailColumn.setCellValueFactory(data -> text(data.getValue().get("email"))); // Lấy email
        roleColumn.setCellValueFactory(data -> text(data.getValue().get("role"))); // Lấy role
        // Hiển thị trạng thái "Đang hoạt động" hoặc "Đã khóa" dựa trên giá trị boolean "active"
        statusColumn.setCellValueFactory(data -> text(Boolean.parseBoolean(String.valueOf(data.getValue().get("active"))) ? "Đang hoạt động" : "Đã khóa"));
    }

    /**
     * Cấu hình cách dữ liệu được hiển thị trong bảng đấu giá.
     * Liên kết các cột với các thuộc tính tương ứng từ đối tượng đấu giá và đối tượng Item/Seller lồng nhau.
     */
    @SuppressWarnings("unchecked")
    private void configureAuctionsTable() {
        auctionIdColumn.setCellValueFactory(data -> text(data.getValue().get("auctionId"))); // Lấy auctionId
        
        // Lấy tên sản phẩm từ đối tượng item lồng nhau
        productNameColumn.setCellValueFactory(data -> {
            Map<String, Object> item = (Map<String, Object>) data.getValue().get("item");
            return text(item != null ? item.get("name") : "");
        });
        
        // Lấy danh mục sản phẩm từ đối tượng item lồng nhau
        categoryColumn.setCellValueFactory(data -> {
            Map<String, Object> item = (Map<String, Object>) data.getValue().get("item");
            return text(item != null ? item.get("category") : "");
        });
        
        // Lấy tên người bán từ đối tượng seller lồng nhau
        sellerColumn.setCellValueFactory(data -> {
            Map<String, Object> seller = (Map<String, Object>) data.getValue().get("seller");
            return text(seller != null ? seller.get("username") : "");
        });
        
        // Định dạng và hiển thị giá hiện tại của phiên đấu giá
        currentPriceColumn.setCellValueFactory(data -> {
            Object currentPrice = data.getValue().get("currentPrice");
            if (currentPrice == null) {
                currentPrice = data.getValue().get("startingPrice"); // Nếu chưa có giá hiện tại, lấy giá khởi điểm
            }
            if (currentPrice != null) {
                try {
                    BigDecimal val = new BigDecimal(String.valueOf(currentPrice));
                    return text(PriceFormatter.formatPrice(val)); // Định dạng giá theo tiền tệ
                } catch (Exception e) {
                    return text(currentPrice + " VNĐ"); // Trường hợp lỗi, hiển thị giá kèm "VNĐ"
                }
            }
            return text("0 VNĐ"); // Mặc định là "0 VNĐ"
        });
        
        // Xác định và hiển thị trạng thái của phiên đấu giá
        auctionStatusColumn.setCellValueFactory(data -> {
            boolean finished = Boolean.parseBoolean(String.valueOf(data.getValue().get("finished")));
            if (finished) {
                return text("Đã kết thúc"); // Nếu cờ "finished" là true
            }
            // Kiểm tra thời gian thực: nếu đã quá end_time thì cũng coi là kết thúc
            Object endTimeObj = data.getValue().get("endTime");
            Object startTimeObj = data.getValue().get("startTime");
            if (endTimeObj != null) {
                try {
                    java.time.LocalDateTime endTime = java.time.LocalDateTime.parse(String.valueOf(endTimeObj));
                    java.time.LocalDateTime now = java.time.LocalDateTime.now();
                    if (now.isAfter(endTime)) { // Nếu thời gian hiện tại sau thời gian kết thúc
                        return text("Đã kết thúc");
                    }
                    if (startTimeObj != null) {
                        java.time.LocalDateTime startTime = java.time.LocalDateTime.parse(String.valueOf(startTimeObj));
                        if (now.isBefore(startTime)) { // Nếu thời gian hiện tại trước thời gian bắt đầu
                            return text("Sắp diễn ra");
                        }
                    }
                } catch (Exception ignored) {
                    // Bỏ qua lỗi parsing ngày giờ, tiếp tục với trạng thái mặc định
                }
            }
            return text("Đang diễn ra"); // Mặc định là "Đang diễn ra"
        });
    }

    /**
     * Thiết lập chức năng lọc dữ liệu trực tiếp cho bảng người dùng và bảng đấu giá
     * dựa trên nội dung nhập vào các trường tìm kiếm.
     */
    private void setupFiltering() {
        // Lọc trực tiếp cho bảng người dùng
        FilteredList<Map<String, Object>> filteredUsers = new FilteredList<>(users, p -> true); // Tạo danh sách lọc ban đầu
        searchUserField.textProperty().addListener((observable, oldValue, newValue) -> { // Lắng nghe sự thay đổi của trường tìm kiếm
            filteredUsers.setPredicate(user -> { // Thiết lập bộ lọc
                if (newValue == null || newValue.isEmpty()) {
                    return true; // Hiển thị tất cả nếu trường tìm kiếm rỗng
                }
                String lowerCaseFilter = newValue.toLowerCase().trim(); // Chuyển đổi từ khóa tìm kiếm thành chữ thường
                // Lấy các thuộc tính của người dùng và chuyển đổi thành chữ thường để so sánh
                String username = String.valueOf(user.getOrDefault("username", "")).toLowerCase();
                String fullName = String.valueOf(user.getOrDefault("fullName", "")).toLowerCase();
                String email = String.valueOf(user.getOrDefault("email", "")).toLowerCase();
                String role = String.valueOf(user.getOrDefault("role", "")).toLowerCase();
                // Trả về true nếu bất kỳ thuộc tính nào chứa từ khóa tìm kiếm
                return username.contains(lowerCaseFilter)
                        || fullName.contains(lowerCaseFilter)
                        || email.contains(lowerCaseFilter)
                        || role.contains(lowerCaseFilter);
            });
        });
        usersTable.setItems(filteredUsers); // Đặt danh sách lọc làm nguồn dữ liệu cho bảng người dùng

        // Lọc trực tiếp cho bảng đấu giá/sản phẩm
        FilteredList<Map<String, Object>> filteredAuctions = new FilteredList<>(auctions, p -> true); // Tạo danh sách lọc ban đầu
        searchAuctionField.textProperty().addListener((observable, oldValue, newValue) -> { // Lắng nghe sự thay đổi của trường tìm kiếm
            filteredAuctions.setPredicate(auction -> { // Thiết lập bộ lọc
                if (newValue == null || newValue.isEmpty()) {
                    return true; // Hiển thị tất cả nếu trường tìm kiếm rỗng
                }
                String lowerCaseFilter = newValue.toLowerCase().trim(); // Chuyển đổi từ khóa tìm kiếm thành chữ thường
                String auctionId = String.valueOf(auction.getOrDefault("auctionId", "")).toLowerCase(); // Lấy auctionId

                // Lấy thông tin sản phẩm từ đối tượng item lồng nhau
                @SuppressWarnings("unchecked")
                Map<String, Object> item = (Map<String, Object>) auction.get("item");
                String productName = "";
                String category = "";
                if (item != null) {
                    productName = String.valueOf(item.getOrDefault("name", "")).toLowerCase();
                    category = String.valueOf(item.getOrDefault("category", "")).toLowerCase();
                }
                
                // Lấy thông tin người bán từ đối tượng seller lồng nhau
                @SuppressWarnings("unchecked")
                Map<String, Object> seller = (Map<String, Object>) auction.get("seller");
                String sellerName = "";
                if (seller != null) {
                    sellerName = String.valueOf(seller.getOrDefault("username", "")).toLowerCase();
                }
                
                // Trả về true nếu bất kỳ thuộc tính nào chứa từ khóa tìm kiếm
                return auctionId.contains(lowerCaseFilter)
                        || productName.contains(lowerCaseFilter)
                        || category.contains(lowerCaseFilter)
                        || sellerName.contains(lowerCaseFilter);
            });
        });
        auctionsTable.setItems(filteredAuctions); // Đặt danh sách lọc làm nguồn dữ liệu cho bảng đấu giá
    }

    /**
     * Xử lý sự kiện khi người dùng nhấn nút "Làm mới" (Refresh).
     * Tải lại toàn bộ dữ liệu trang tổng quan.
     */
    @FXML
    public void handleRefresh() {
        loadDashboardData(); // Tải lại dữ liệu
    }

    /**
     * Xử lý sự kiện khi người dùng nhấn nút "Thay đổi trạng thái người dùng đã chọn" (Toggle User).
     * Kích hoạt hoặc khóa tài khoản người dùng được chọn.
     */
    @FXML
    public void handleToggleSelectedUser() {
        Map<String, Object> selectedUser = usersTable.getSelectionModel().getSelectedItem(); // Lấy người dùng đang được chọn
        if (selectedUser == null) {
            notificationLabel.setText("Hãy chọn một tài khoản trước."); // Thông báo nếu chưa chọn người dùng
            return;
        }

        String targetUsername = String.valueOf(selectedUser.get("username")); // Lấy username của người dùng được chọn
        boolean currentActive = Boolean.parseBoolean(String.valueOf(selectedUser.get("active"))); // Lấy trạng thái hiện tại
        boolean nextActive = !currentActive; // Trạng thái mới (ngược lại)
        String adminUsername = UserSession.getLoggedInUser().getUsername(); // Lấy username của admin hiện tại

        // Thực hiện thay đổi trạng thái người dùng bất đồng bộ
        FxAsync.run(
                () -> networkService.setUserActive(adminUsername, targetUsername, nextActive), // Gọi API để thay đổi trạng thái
                () -> {
                    notificationLabel.setText((nextActive ? "Đã mở khóa " : "Đã khóa ") + targetUsername); // Cập nhật thông báo
                    loadDashboardData(); // Tải lại dữ liệu dashboard để cập nhật trạng thái
                },
                errorMsg -> AlertHelper.showError("Lỗi quản trị", errorMsg) // Xử lý lỗi
        );
    }

    /**
     * Xử lý sự kiện khi người dùng nhấn nút "Xóa phiên đấu giá đã chọn".
     * Xóa phiên đấu giá được chọn sau khi xác nhận.
     */
    @FXML
    public void handleDeleteSelectedAuction() {
        Map<String, Object> selectedAuction = auctionsTable.getSelectionModel().getSelectedItem(); // Lấy phiên đấu giá đang được chọn
        if (selectedAuction == null) {
            notificationLabel.setText("Hãy chọn một phiên đấu giá để xóa."); // Thông báo nếu chưa chọn phiên đấu giá
            return;
        }

        String auctionId = String.valueOf(selectedAuction.get("auctionId")); // Lấy ID phiên đấu giá

        String adminUsername = UserSession.getLoggedInUser().getUsername(); // Lấy username của admin hiện tại
        // Hiển thị hộp thoại xác nhận trước khi xóa
        boolean confirm = AlertHelper.showConfirmation("Xác nhận xóa", "Bạn có chắc chắn muốn xóa phiên đấu giá này không? Dữ liệu đấu giá và thầu liên quan cũng sẽ bị xóa vĩnh viễn.");
        if (!confirm) {
            return; // Nếu không xác nhận, dừng lại
        }

        notificationLabel.setText("Đang xóa phiên " + auctionId + "..."); // Hiển thị thông báo đang xóa
        // Thực hiện xóa phiên đấu giá bất đồng bộ
        FxAsync.run(
                () -> {
                    networkService.deleteAuction(adminUsername, auctionId); // Gọi API để xóa phiên đấu giá
                    return null; // Không cần trả về giá trị cụ thể
                },
                result -> {
                    notificationLabel.setText("Đã xóa thành công phiên " + auctionId); // Cập nhật thông báo
                    loadDashboardData(); // Tải lại dữ liệu dashboard để cập nhật danh sách
                },
                errorMsg -> AlertHelper.showError("Lỗi xóa phiên", errorMsg) // Xử lý lỗi
        );
    }

    /**
     * Điều hướng đến màn hình tạo phiên đấu giá.
     * @param event Sự kiện hành động kích hoạt.
     */
    @FXML
    public void goToCreateAuction(ActionEvent event) {
        SceneNavigator.goToCreateAuction(event);
    }

    /**
     * Điều hướng về màn hình chính.
     * @param event Sự kiện hành động kích hoạt.
     */
    @FXML
    public void goToHome(ActionEvent event) {
        SceneNavigator.goToHome(event);
    }

    /**
     * Chuyển đổi giữa chế độ giao diện sáng và tối.
     * @param event Sự kiện hành động kích hoạt.
     */
    @FXML
    public void toggleTheme(ActionEvent event) {
        javafx.scene.Scene scene = ((Node) event.getSource()).getScene(); // Lấy Scene hiện tại
        ThemeManager.getInstance().toggleTheme(scene); // Chuyển đổi giao diện
        updateThemeButton(); // Cập nhật biểu tượng nút chuyển đổi
    }

    /**
     * Cập nhật biểu tượng trên nút chuyển đổi giao diện dựa trên chế độ giao diện hiện tại.
     * Hiển thị mặt trời cho chế độ tối và mặt trăng cho chế độ sáng.
     */
    private void updateThemeButton() {
        if (themeToggleBtn != null) {
            themeToggleBtn.setText(
                    ThemeManager.getInstance().isDarkMode()
                            ? "☀" // Biểu tượng mặt trời cho chế độ tối
                            : "\uD83C\uDF19" // Biểu tượng mặt trăng cho chế độ sáng
            );
        }
    }

    /**
     * Xử lý sự kiện đăng xuất người dùng.
     * @param event Sự kiện hành động kích hoạt.
     */
    @FXML
    public void handleLogout(ActionEvent event) {
        LoginStateHelper.handleLogout(event);
    }

    /**
     * Tải dữ liệu tổng quan cho trang dashboard từ dịch vụ mạng.
     * Bao gồm danh sách người dùng và danh sách phiên đấu giá.
     */
    private void loadDashboardData() {
        String adminUsername = UserSession.getLoggedInUser().getUsername(); // Lấy username của admin
        notificationLabel.setText("Đang tải dữ liệu..."); // Hiển thị thông báo tải dữ liệu
        // Thực hiện tải dữ liệu bất đồng bộ
        FxAsync.run(
                () -> Map.of(
                        "users", networkService.getUsers(adminUsername), // Lấy danh sách người dùng
                        "auctions", networkService.getAuctions() // Lấy danh sách phiên đấu giá
                ),
                this::renderDashboard, // Sau khi tải xong, gọi phương thức renderDashboard để hiển thị
                errorMsg -> AlertHelper.showError("Lỗi tải dữ liệu", errorMsg) // Xử lý lỗi
        );
    }

    /**
     * Hiển thị dữ liệu lên trang dashboard sau khi đã tải.
     * Cập nhật các nhãn thống kê và bảng dữ liệu.
     * @param data Map chứa dữ liệu người dùng và đấu giá đã tải.
     */
    @SuppressWarnings("unchecked")
    private void renderDashboard(Map<String, Object> data) {
        List<Map<String, Object>> loadedUsers = (List<Map<String, Object>>) data.get("users"); // Lấy danh sách người dùng
        List<Map<String, Object>> loadedAuctions = (List<Map<String, Object>>) data.get("auctions"); // Lấy danh sách phiên đấu giá

        users.setAll(loadedUsers); // Cập nhật danh sách người dùng
        auctions.setAll(loadedAuctions); // Cập nhật danh sách phiên đấu giá
        
        // Đếm số người dùng đang hoạt động
        long activeUsers = loadedUsers.stream()
                .filter(user -> Boolean.parseBoolean(String.valueOf(user.get("active"))))
                .count();
        // Đếm số phiên đấu giá đang diễn ra (chưa kết thúc và thời gian chưa qua)
        long runningAuctions = loadedAuctions.stream()
                .filter(auction -> {
                    if (Boolean.parseBoolean(String.valueOf(auction.get("finished")))) {
                        return false; // Nếu đã kết thúc theo cờ, không phải đang diễn ra
                    }
                    Object endTimeObj = auction.get("endTime");
                    if (endTimeObj != null) {
                        try {
                            java.time.LocalDateTime endTime = java.time.LocalDateTime.parse(String.valueOf(endTimeObj));
                            if (java.time.LocalDateTime.now().isAfter(endTime)) {
                                return false; // Nếu thời gian hiện tại đã qua thời gian kết thúc, không phải đang diễn ra
                            }
                        } catch (Exception ignored) {
                            // Bỏ qua lỗi parsing ngày giờ
                        }
                    }
                    return true; // Nếu chưa kết thúc và thời gian chưa qua, thì đang diễn ra
                })
                .count();

        totalUsersLabel.setText(String.valueOf(loadedUsers.size())); // Hiển thị tổng số người dùng
        activeUsersLabel.setText(String.valueOf(activeUsers)); // Hiển thị số người dùng hoạt động
        totalAuctionsLabel.setText(String.valueOf(loadedAuctions.size())); // Hiển thị tổng số phiên đấu giá
        runningAuctionsLabel.setText(String.valueOf(runningAuctions)); // Hiển thị số phiên đấu giá đang diễn ra
        notificationLabel.setText("Dữ liệu đã cập nhật."); // Cập nhật thông báo hoàn tất
    }

    /**
     * Kiểm tra xem người dùng hiện tại có phải là quản trị viên đã đăng nhập hay không.
     * @return true nếu là quản trị viên đã đăng nhập, ngược lại false.
     */
    private boolean isAdminLoggedIn() {
        return UserSession.isLoggedIn() // Kiểm tra đã đăng nhập
                && "ADMIN".equalsIgnoreCase(UserSession.getLoggedInUser().getRole()); // Kiểm tra vai trò là ADMIN
    }

    /**
     * Kiểm tra xem một phiên đấu giá đã kết thúc hay chưa.
     * Dựa trên cờ 'finished' hoặc so sánh thời gian kết thúc với thời gian hiện tại.
     * @param auction Đối tượng Map đại diện cho một phiên đấu giá.
     * @return true nếu phiên đấu giá đã kết thúc, ngược lại false.
     */
    private boolean isAuctionFinished(Map<String, Object> auction) {
        if (auction == null) {
            return false;
        }
        // Kiểm tra cờ 'finished'
        if (Boolean.parseBoolean(String.valueOf(auction.get("finished")))) {
            return true;
        }

        Object endTimeObj = auction.get("endTime");
        if (endTimeObj == null) {
            return false;
        }

        try {
            java.time.LocalDateTime endTime = java.time.LocalDateTime.parse(String.valueOf(endTimeObj));
            return !java.time.LocalDateTime.now().isBefore(endTime); // So sánh thời gian hiện tại với thời gian kết thúc
        } catch (Exception ignored) {
            return false; // Trường hợp lỗi parse ngày giờ
        }
    }

    /**
     * Phương thức tiện ích để tạo SimpleStringProperty từ một đối tượng.
     * Dùng để liên kết dữ liệu với các cột của TableView.
     * @param value Đối tượng cần chuyển đổi.
     * @return SimpleStringProperty chứa giá trị chuỗi.
     */
    private SimpleStringProperty text(Object value) {
        return new SimpleStringProperty(value == null ? "" : String.valueOf(value));
    }
}
