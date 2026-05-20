package com.auction.controller;

import com.auction.network.client.NetworkService;
import com.auction.util.AlertHelper;
import com.auction.util.FxAsync;
import com.auction.util.LoginStateHelper;
import com.auction.util.PriceFormatter;
import com.auction.util.SceneNavigator;
import com.auction.util.UserSession;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class AdminDashboardController {

    @FXML private Label adminNameLabel;
    @FXML private Label totalUsersLabel;
    @FXML private Label activeUsersLabel;
    @FXML private Label totalAuctionsLabel;
    @FXML private Label runningAuctionsLabel;
    
    // User Management Table
    @FXML private TableView<Map<String, Object>> usersTable;
    @FXML private TableColumn<Map<String, Object>, String> usernameColumn;
    @FXML private TableColumn<Map<String, Object>, String> fullNameColumn;
    @FXML private TableColumn<Map<String, Object>, String> emailColumn;
    @FXML private TableColumn<Map<String, Object>, String> roleColumn;
    @FXML private TableColumn<Map<String, Object>, String> statusColumn;
    @FXML private TextField searchUserField;

    // Auction & Product Management Table
    @FXML private TableView<Map<String, Object>> auctionsTable;
    @FXML private TableColumn<Map<String, Object>, String> auctionIdColumn;
    @FXML private TableColumn<Map<String, Object>, String> productNameColumn;
    @FXML private TableColumn<Map<String, Object>, String> categoryColumn;
    @FXML private TableColumn<Map<String, Object>, String> sellerColumn;
    @FXML private TableColumn<Map<String, Object>, String> currentPriceColumn;
    @FXML private TableColumn<Map<String, Object>, String> auctionStatusColumn;
    @FXML private TextField searchAuctionField;

    @FXML private Label notificationLabel;
    @FXML private Button loginButton;

    private final NetworkService networkService = NetworkService.getInstance();
    private final ObservableList<Map<String, Object>> users = FXCollections.observableArrayList();
    private final ObservableList<Map<String, Object>> auctions = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        LoginStateHelper.updateLoginButton(loginButton);
        if (!isAdminLoggedIn()) {
            AlertHelper.showError("Không có quyền", "Bạn cần đăng nhập bằng tài khoản quản trị.");
            return;
        }

        adminNameLabel.setText(UserSession.getLoggedInUser().getUsername());
        
        configureUsersTable();
        configureAuctionsTable();
        
        setupFiltering();
        
        loadDashboardData();
    }

    private void configureUsersTable() {
        usernameColumn.setCellValueFactory(data -> text(data.getValue().get("username")));
        fullNameColumn.setCellValueFactory(data -> text(data.getValue().get("fullName")));
        emailColumn.setCellValueFactory(data -> text(data.getValue().get("email")));
        roleColumn.setCellValueFactory(data -> text(data.getValue().get("role")));
        statusColumn.setCellValueFactory(data -> text(Boolean.parseBoolean(String.valueOf(data.getValue().get("active"))) ? "Đang hoạt động" : "Đã khóa"));
    }

    @SuppressWarnings("unchecked")
    private void configureAuctionsTable() {
        auctionIdColumn.setCellValueFactory(data -> text(data.getValue().get("auctionId")));
        
        productNameColumn.setCellValueFactory(data -> {
            Map<String, Object> item = (Map<String, Object>) data.getValue().get("item");
            return text(item != null ? item.get("name") : "");
        });
        
        categoryColumn.setCellValueFactory(data -> {
            Map<String, Object> item = (Map<String, Object>) data.getValue().get("item");
            return text(item != null ? item.get("category") : "");
        });
        
        sellerColumn.setCellValueFactory(data -> {
            Map<String, Object> seller = (Map<String, Object>) data.getValue().get("seller");
            return text(seller != null ? seller.get("username") : "");
        });
        
        currentPriceColumn.setCellValueFactory(data -> {
            Object currentPrice = data.getValue().get("currentPrice");
            if (currentPrice == null) {
                currentPrice = data.getValue().get("startingPrice");
            }
            if (currentPrice != null) {
                try {
                    BigDecimal val = new BigDecimal(String.valueOf(currentPrice));
                    return text(PriceFormatter.formatPrice(val));
                } catch (Exception e) {
                    return text(currentPrice + " VNĐ");
                }
            }
            return text("0 VNĐ");
        });
        
        auctionStatusColumn.setCellValueFactory(data -> {
            boolean finished = Boolean.parseBoolean(String.valueOf(data.getValue().get("finished")));
            if (finished) {
                return text("Đã kết thúc");
            }
            // Kiểm tra thời gian thực: nếu đã quá end_time thì cũng coi là kết thúc
            Object endTimeObj = data.getValue().get("endTime");
            Object startTimeObj = data.getValue().get("startTime");
            if (endTimeObj != null) {
                try {
                    java.time.LocalDateTime endTime = java.time.LocalDateTime.parse(String.valueOf(endTimeObj));
                    java.time.LocalDateTime now = java.time.LocalDateTime.now();
                    if (now.isAfter(endTime)) {
                        return text("Đã kết thúc");
                    }
                    if (startTimeObj != null) {
                        java.time.LocalDateTime startTime = java.time.LocalDateTime.parse(String.valueOf(startTimeObj));
                        if (now.isBefore(startTime)) {
                            return text("Sắp diễn ra");
                        }
                    }
                } catch (Exception ignored) {}
            }
            return text("Đang diễn ra");
        });
    }

    private void setupFiltering() {
        // Users Live Filtering
        FilteredList<Map<String, Object>> filteredUsers = new FilteredList<>(users, p -> true);
        searchUserField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredUsers.setPredicate(user -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newValue.toLowerCase().trim();
                String username = String.valueOf(user.getOrDefault("username", "")).toLowerCase();
                String fullName = String.valueOf(user.getOrDefault("fullName", "")).toLowerCase();
                String email = String.valueOf(user.getOrDefault("email", "")).toLowerCase();
                String role = String.valueOf(user.getOrDefault("role", "")).toLowerCase();
                return username.contains(lowerCaseFilter)
                        || fullName.contains(lowerCaseFilter)
                        || email.contains(lowerCaseFilter)
                        || role.contains(lowerCaseFilter);
            });
        });
        usersTable.setItems(filteredUsers);

        // Auctions/Products Live Filtering
        FilteredList<Map<String, Object>> filteredAuctions = new FilteredList<>(auctions, p -> true);
        searchAuctionField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredAuctions.setPredicate(auction -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newValue.toLowerCase().trim();
                String auctionId = String.valueOf(auction.getOrDefault("auctionId", "")).toLowerCase();
                
                @SuppressWarnings("unchecked")
                Map<String, Object> item = (Map<String, Object>) auction.get("item");
                String productName = "";
                String category = "";
                if (item != null) {
                    productName = String.valueOf(item.getOrDefault("name", "")).toLowerCase();
                    category = String.valueOf(item.getOrDefault("category", "")).toLowerCase();
                }
                
                @SuppressWarnings("unchecked")
                Map<String, Object> seller = (Map<String, Object>) auction.get("seller");
                String sellerName = "";
                if (seller != null) {
                    sellerName = String.valueOf(seller.getOrDefault("username", "")).toLowerCase();
                }
                
                return auctionId.contains(lowerCaseFilter)
                        || productName.contains(lowerCaseFilter)
                        || category.contains(lowerCaseFilter)
                        || sellerName.contains(lowerCaseFilter);
            });
        });
        auctionsTable.setItems(filteredAuctions);
    }

    @FXML
    public void handleRefresh() {
        loadDashboardData();
    }

    @FXML
    public void handleToggleSelectedUser() {
        Map<String, Object> selectedUser = usersTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            notificationLabel.setText("Hãy chọn một tài khoản trước.");
            return;
        }

        String targetUsername = String.valueOf(selectedUser.get("username"));
        boolean currentActive = Boolean.parseBoolean(String.valueOf(selectedUser.get("active")));
        boolean nextActive = !currentActive;
        String adminUsername = UserSession.getLoggedInUser().getUsername();

        FxAsync.run(
                () -> networkService.setUserActive(adminUsername, targetUsername, nextActive),
                () -> {
                    notificationLabel.setText((nextActive ? "Đã mở khóa " : "Đã khóa ") + targetUsername);
                    loadDashboardData();
                },
                errorMsg -> AlertHelper.showError("Lỗi quản trị", errorMsg)
        );
    }

    @FXML
    public void handleDeleteSelectedAuction() {
        Map<String, Object> selectedAuction = auctionsTable.getSelectionModel().getSelectedItem();
        if (selectedAuction == null) {
            notificationLabel.setText("Hãy chọn một phiên đấu giá để xóa.");
            return;
        }

        String auctionId = String.valueOf(selectedAuction.get("auctionId"));
        if (!isAuctionFinished(selectedAuction)) {
            AlertHelper.showError("Lỗi xóa phiên", "Chỉ được phép xóa những phiên đấu giá đã kết thúc.");
            return;
        }

        String adminUsername = UserSession.getLoggedInUser().getUsername();
        boolean confirm = AlertHelper.showConfirmation("Xác nhận xóa", "Bạn có chắc chắn muốn xóa phiên đấu giá này không? Dữ liệu đấu giá và thầu liên quan cũng sẽ bị xóa vĩnh viễn.");
        if (!confirm) {
            return;
        }

        notificationLabel.setText("Đang xóa phiên " + auctionId + "...");
        FxAsync.run(
                () -> {
                    networkService.deleteAuction(adminUsername, auctionId);
                    return null;
                },
                result -> {
                    notificationLabel.setText("Đã xóa thành công phiên " + auctionId);
                    loadDashboardData();
                },
                errorMsg -> AlertHelper.showError("Lỗi xóa phiên", errorMsg)
        );
    }

    @FXML
    public void goToCreateAuction(ActionEvent event) {
        SceneNavigator.goToCreateAuction(event);
    }

    @FXML
    public void goToHome(ActionEvent event) {
        SceneNavigator.goToHome(event);
    }

    @FXML
    public void handleLogout(ActionEvent event) {
        LoginStateHelper.handleLogout(event);
    }

    private void loadDashboardData() {
        String adminUsername = UserSession.getLoggedInUser().getUsername();
        notificationLabel.setText("Đang tải dữ liệu...");
        FxAsync.run(
                () -> Map.of(
                        "users", networkService.getUsers(adminUsername),
                        "auctions", networkService.getAuctions()
                ),
                this::renderDashboard,
                errorMsg -> AlertHelper.showError("Lỗi tải dữ liệu", errorMsg)
        );
    }

    @SuppressWarnings("unchecked")
    private void renderDashboard(Map<String, Object> data) {
        List<Map<String, Object>> loadedUsers = (List<Map<String, Object>>) data.get("users");
        List<Map<String, Object>> loadedAuctions = (List<Map<String, Object>>) data.get("auctions");

        users.setAll(loadedUsers);
        auctions.setAll(loadedAuctions);
        
        long activeUsers = loadedUsers.stream()
                .filter(user -> Boolean.parseBoolean(String.valueOf(user.get("active"))))
                .count();
        long runningAuctions = loadedAuctions.stream()
                .filter(auction -> {
                    if (Boolean.parseBoolean(String.valueOf(auction.get("finished")))) {
                        return false;
                    }
                    Object endTimeObj = auction.get("endTime");
                    if (endTimeObj != null) {
                        try {
                            java.time.LocalDateTime endTime = java.time.LocalDateTime.parse(String.valueOf(endTimeObj));
                            if (java.time.LocalDateTime.now().isAfter(endTime)) {
                                return false;
                            }
                        } catch (Exception ignored) {}
                    }
                    return true;
                })
                .count();

        totalUsersLabel.setText(String.valueOf(loadedUsers.size()));
        activeUsersLabel.setText(String.valueOf(activeUsers));
        totalAuctionsLabel.setText(String.valueOf(loadedAuctions.size()));
        runningAuctionsLabel.setText(String.valueOf(runningAuctions));
        notificationLabel.setText("Dữ liệu đã cập nhật.");
    }

    private boolean isAdminLoggedIn() {
        return UserSession.isLoggedIn()
                && "ADMIN".equalsIgnoreCase(UserSession.getLoggedInUser().getRole());
    }

    private boolean isAuctionFinished(Map<String, Object> auction) {
        if (auction == null) {
            return false;
        }
        if (Boolean.parseBoolean(String.valueOf(auction.get("finished")))) {
            return true;
        }

        Object endTimeObj = auction.get("endTime");
        if (endTimeObj == null) {
            return false;
        }

        try {
            java.time.LocalDateTime endTime = java.time.LocalDateTime.parse(String.valueOf(endTimeObj));
            return !java.time.LocalDateTime.now().isBefore(endTime);
        } catch (Exception ignored) {
            return false;
        }
    }

    private SimpleStringProperty text(Object value) {
        return new SimpleStringProperty(value == null ? "" : String.valueOf(value));
    }
}
