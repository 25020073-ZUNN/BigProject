package com.auction.controller;

import com.auction.model.Auction;
import com.auction.model.BidTransaction;
import com.auction.model.user.User;
import com.auction.network.client.AuctionPayloadMapper;
import com.auction.network.client.NetworkService;
import com.auction.util.AlertHelper;
import com.auction.util.LoginStateHelper;
import com.auction.util.PriceFormatter;
import com.auction.util.SceneNavigator;
import com.auction.util.UserSession;
import com.auction.util.FxAsync;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Controller cho màn hình "Lịch sử hoạt động" (Auction History Page).
 * Quản lý lịch sử Bid và lịch sử đăng bán sản phẩm của người dùng hiện tại.
 */
public class AuctionHistoryController {

    private final NetworkService networkService = NetworkService.getInstance();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    // --- Tab Lịch sử đặt giá (Bid) - Metrics ---
    @FXML private Label lblTotalBids;
    @FXML private Label lblActiveBids;
    @FXML private Label lblWonBids;
    @FXML private Label lblTotalBidAmount;

    // --- Tab Lịch sử đặt giá (Bid) - Table ---
    @FXML private TableView<MyBidRow> tableBids;
    @FXML private TableColumn<MyBidRow, String> colBidAssetName;
    @FXML private TableColumn<MyBidRow, String> colBidCategory;
    @FXML private TableColumn<MyBidRow, BigDecimal> colBidAmount;
    @FXML private TableColumn<MyBidRow, BigDecimal> colBidCurrentPrice;
    @FXML private TableColumn<MyBidRow, LocalDateTime> colBidTime;
    @FXML private TableColumn<MyBidRow, String> colBidStatus;
    @FXML private TableColumn<MyBidRow, String> colBidResult;
    @FXML private TableColumn<MyBidRow, Void> colBidAction;

    // --- Tab Tài sản đăng bán (Sale) - Metrics ---
    @FXML private Label lblTotalSales;
    @FXML private Label lblActiveSales;
    @FXML private Label lblSoldSales;
    @FXML private Label lblTotalRevenue;

    // --- Tab Tài sản đăng bán (Sale) - Table ---
    @FXML private TableView<MySaleRow> tableSales;
    @FXML private TableColumn<MySaleRow, String> colSaleAssetName;
    @FXML private TableColumn<MySaleRow, String> colSaleCategory;
    @FXML private TableColumn<MySaleRow, BigDecimal> colSaleStartingPrice;
    @FXML private TableColumn<MySaleRow, BigDecimal> colSaleCurrentPrice;
    @FXML private TableColumn<MySaleRow, String> colSaleHighestBidder;
    @FXML private TableColumn<MySaleRow, LocalDateTime> colSaleEndTime;
    @FXML private TableColumn<MySaleRow, String> colSaleStatus;
    @FXML private TableColumn<MySaleRow, Void> colSaleAction;

    @FXML private Button loginButton;

    @FXML
    public void initialize() {
        // Cập nhật trạng thái nút Login/Logout trên Topbar
        LoginStateHelper.updateLoginButton(loginButton);

        // Kiểm tra quyền truy cập: bắt buộc phải đăng nhập
        if (!UserSession.isLoggedIn()) {
            Platform.runLater(() -> {
                AlertHelper.showError("Chưa đăng nhập", "Vui lòng đăng nhập để xem lịch sử hoạt động.");
                SceneNavigator.goToLogin(new ActionEvent(loginButton, null));
            });
            return;
        }

        setupBidTable();
        setupSaleTable();
        loadHistoryData();
    }

    /**
     * Cấu hình TableView hiển thị lịch sử đặt giá
     */
    private void setupBidTable() {
        colBidAssetName.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getAssetName()));
        colBidCategory.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCategory()));
        colBidAmount.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getBidAmount()));
        colBidCurrentPrice.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getCurrentPrice()));
        colBidTime.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getBidTime()));
        colBidStatus.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStatus()));
        colBidResult.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getResult()));

        // Định dạng tiền tệ
        colBidAmount.setCellFactory(col -> new TableCell<MyBidRow, BigDecimal>() {
            @Override
            protected void updateItem(BigDecimal price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                } else {
                    setText(PriceFormatter.formatPrice(price));
                }
            }
        });

        colBidCurrentPrice.setCellFactory(col -> new TableCell<MyBidRow, BigDecimal>() {
            @Override
            protected void updateItem(BigDecimal price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                } else {
                    setText(PriceFormatter.formatPrice(price));
                }
            }
        });

        // Định dạng thời gian đặt giá
        colBidTime.setCellFactory(col -> new TableCell<MyBidRow, LocalDateTime>() {
            @Override
            protected void updateItem(LocalDateTime time, boolean empty) {
                super.updateItem(time, empty);
                if (empty || time == null) {
                    setText(null);
                } else {
                    setText(time.format(DATE_FORMATTER));
                }
            }
        });

        // Cột Trạng thái phiên đấu giá (badge màu)
        colBidStatus.setCellFactory(col -> new TableCell<MyBidRow, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label badge = new Label(status);
                    badge.getStyleClass().clear();
                    if ("Đang diễn ra".equals(status)) {
                        badge.getStyleClass().addAll("badge-live");
                    } else if ("Đã kết thúc".equals(status)) {
                        badge.getStyleClass().addAll("badge-hot");
                    } else {
                        badge.getStyleClass().addAll("badge-soft");
                    }
                    setGraphic(badge);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        // Cột kết quả đấu giá của User
        colBidResult.setCellFactory(col -> new TableCell<MyBidRow, String>() {
            @Override
            protected void updateItem(String result, boolean empty) {
                super.updateItem(result, empty);
                if (empty || result == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label badge = new Label(result);
                    badge.getStyleClass().clear();
                    if ("Thắng cuộc 🏆".equals(result)) {
                        badge.getStyleClass().addAll("badge-vip");
                    } else if ("Đang dẫn đầu 🟢".equals(result)) {
                        badge.getStyleClass().addAll("badge-live");
                    } else if ("Bị vượt giá 🔴".equals(result)) {
                        badge.getStyleClass().addAll("badge-hot");
                    } else {
                        badge.getStyleClass().addAll("badge-soft");
                    }
                    setGraphic(badge);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        // Cột Action nút "Chi tiết"
        colBidAction.setCellFactory(col -> new TableCell<MyBidRow, Void>() {
            private final Button btn = new Button("Chi tiết  →");
            {
                btn.getStyleClass().addAll("secondary-btn-small");
                btn.setOnAction(e -> {
                    MyBidRow row = getTableView().getItems().get(getIndex());
                    Stage stage = (Stage) getScene().getWindow();
                    SceneNavigator.navigateToAssetDetail(stage, row.getAuction());
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btn);
                    setAlignment(Pos.CENTER);
                }
            }
        });
    }

    /**
     * Cấu hình TableView hiển thị lịch sử đăng bán
     */
    private void setupSaleTable() {
        colSaleAssetName.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getAssetName()));
        colSaleCategory.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCategory()));
        colSaleStartingPrice.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getStartingPrice()));
        colSaleCurrentPrice.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getCurrentPrice()));
        colSaleHighestBidder.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getHighestBidder()));
        colSaleEndTime.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getEndTime()));
        colSaleStatus.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStatus()));

        // Định dạng giá
        colSaleStartingPrice.setCellFactory(col -> new TableCell<MySaleRow, BigDecimal>() {
            @Override
            protected void updateItem(BigDecimal price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                } else {
                    setText(PriceFormatter.formatPrice(price));
                }
            }
        });

        colSaleCurrentPrice.setCellFactory(col -> new TableCell<MySaleRow, BigDecimal>() {
            @Override
            protected void updateItem(BigDecimal price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                } else {
                    setText(PriceFormatter.formatPrice(price));
                }
            }
        });

        // Định dạng thời gian kết thúc
        colSaleEndTime.setCellFactory(col -> new TableCell<MySaleRow, LocalDateTime>() {
            @Override
            protected void updateItem(LocalDateTime time, boolean empty) {
                super.updateItem(time, empty);
                if (empty || time == null) {
                    setText(null);
                } else {
                    setText(time.format(DATE_FORMATTER));
                }
            }
        });

        // Cột Trạng thái của sản phẩm đăng bán
        colSaleStatus.setCellFactory(col -> new TableCell<MySaleRow, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label badge = new Label(status);
                    badge.getStyleClass().clear();
                    if ("Đang diễn ra".equals(status)) {
                        badge.getStyleClass().addAll("badge-live");
                    } else if ("Đã kết thúc".equals(status)) {
                        badge.getStyleClass().addAll("badge-hot");
                    } else {
                        badge.getStyleClass().addAll("badge-soft");
                    }
                    setGraphic(badge);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        // Cột Action nút "Chi tiết"
        colSaleAction.setCellFactory(col -> new TableCell<MySaleRow, Void>() {
            private final Button btn = new Button("Chi tiết  →");
            {
                btn.getStyleClass().addAll("secondary-btn-small");
                btn.setOnAction(e -> {
                    MySaleRow row = getTableView().getItems().get(getIndex());
                    Stage stage = (Stage) getScene().getWindow();
                    SceneNavigator.navigateToAssetDetail(stage, row.getAuction());
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btn);
                    setAlignment(Pos.CENTER);
                }
            }
        });
    }

    /**
     * Tải dữ liệu đấu giá từ Server bất đồng bộ và lọc theo user đang đăng nhập
     */
    private void loadHistoryData() {
        if (!UserSession.isLoggedIn()) return;
        User currentUser = UserSession.getLoggedInUser();

        FxAsync.run(
                networkService::getAuctions,
                rawAuctions -> {
                    List<Auction> allAuctions = AuctionPayloadMapper.toAuctions(rawAuctions);
                    processBids(allAuctions, currentUser);
                    processSales(allAuctions, currentUser);
                },
                error -> {
                    System.err.println("Lỗi tải lịch sử hoạt động: " + error);
                    AlertHelper.showError("Lỗi kết nối", "Không thể tải dữ liệu từ máy chủ: " + error);
                }
        );
    }

    /**
     * Xử lý hiển thị danh sách Bid của user hiện tại
     */
    private void processBids(List<Auction> allAuctions, User currentUser) {
        List<MyBidRow> bidRows = new ArrayList<>();
        int totalBids = 0;
        Set<String> activeAuctionIds = new HashSet<>();
        Set<String> wonAuctionIds = new HashSet<>();
        Map<String, BigDecimal> maxBidsByAuction = new HashMap<>();

        for (Auction auction : allAuctions) {
            boolean isFinished = auction.isFinished();
            boolean isUpcoming = LocalDateTime.now().isBefore(auction.getItem().getStartTime());
            boolean isRunning = !isFinished && !isUpcoming;

            String statusStr = "Sắp diễn ra";
            if (isRunning) statusStr = "Đang diễn ra";
            else if (isFinished) statusStr = "Đã kết thúc";

            for (BidTransaction transaction : auction.getBidHistory()) {
                if (transaction.getBidder().getUsername().equals(currentUser.getUsername())) {
                    totalBids++;

                    // Xác định vị thế tại lượt đặt giá này
                    String resultStr = "Không thắng";
                    if (isFinished) {
                        if (auction.getHighestBidder() != null &&
                                auction.getHighestBidder().getUsername().equals(currentUser.getUsername())) {
                            resultStr = "Thắng cuộc 🏆";
                            wonAuctionIds.add(auction.getId());
                        }
                    } else if (isRunning) {
                        if (auction.getHighestBidder() != null &&
                                auction.getHighestBidder().getUsername().equals(currentUser.getUsername())) {
                            resultStr = "Đang dẫn đầu 🟢";
                        } else {
                            resultStr = "Bị vượt giá 🔴";
                        }
                        activeAuctionIds.add(auction.getId());
                    } else {
                        resultStr = "Chờ mở";
                    }

                    bidRows.add(new MyBidRow(
                            auction.getItem().getName(),
                            auction.getItem().getCategory(),
                            transaction.getBidAmount(),
                            auction.getCurrentPrice(),
                            transaction.getBidTime(),
                            statusStr,
                            resultStr,
                            auction
                    ));

                    // Tính tổng số tiền bid cao nhất của user tại mỗi phiên
                    BigDecimal currentMax = maxBidsByAuction.getOrDefault(auction.getId(), BigDecimal.ZERO);
                    if (transaction.getBidAmount().compareTo(currentMax) > 0) {
                        maxBidsByAuction.put(auction.getId(), transaction.getBidAmount());
                    }
                }
            }
        }

        // Đổ dữ liệu vào Table
        // Sắp xếp lượt đặt giá mới nhất lên đầu
        bidRows.sort((a, b) -> b.getBidTime().compareTo(a.getBidTime()));
        tableBids.setItems(FXCollections.observableArrayList(bidRows));

        // Cập nhật Metrics
        lblTotalBids.setText(String.valueOf(totalBids));
        lblActiveBids.setText(String.valueOf(activeAuctionIds.size()));
        lblWonBids.setText(String.valueOf(wonAuctionIds.size()));

        BigDecimal totalBidAmountSum = maxBidsByAuction.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        lblTotalBidAmount.setText(PriceFormatter.formatPrice(totalBidAmountSum));
    }

    /**
     * Xử lý hiển thị danh sách sản phẩm đăng bán của user hiện tại
     */
    private void processSales(List<Auction> allAuctions, User currentUser) {
        List<MySaleRow> saleRows = new ArrayList<>();
        int totalSales = 0;
        int activeSales = 0;
        int soldSales = 0;
        BigDecimal totalRevenueSum = BigDecimal.ZERO;

        for (Auction auction : allAuctions) {
            if (auction.getSeller().getUsername().equals(currentUser.getUsername())) {
                totalSales++;

                boolean isFinished = auction.isFinished();
                boolean isUpcoming = LocalDateTime.now().isBefore(auction.getItem().getStartTime());
                boolean isRunning = !isFinished && !isUpcoming;

                String statusStr = "Sắp diễn ra";
                if (isRunning) {
                    statusStr = "Đang diễn ra";
                    activeSales++;
                } else if (isFinished) {
                    statusStr = "Đã kết thúc";
                    if (auction.hasWinner()) {
                        soldSales++;
                        totalRevenueSum = totalRevenueSum.add(auction.getCurrentPrice());
                    }
                }

                String highestBidderStr = "Chưa có";
                if (auction.getHighestBidder() != null) {
                    highestBidderStr = auction.getHighestBidder().getUsername();
                    if (isFinished) {
                        highestBidderStr += " 🏆";
                    }
                }

                saleRows.add(new MySaleRow(
                        auction.getItem().getName(),
                        auction.getItem().getCategory(),
                        auction.getStartingPrice(),
                        auction.getCurrentPrice(),
                        highestBidderStr,
                        auction.getItem().getEndTime(),
                        statusStr,
                        auction
                ));
            }
        }

        // Đổ dữ liệu vào Table
        // Sắp xếp các phiên kết thúc sau hoặc mới tạo lên đầu
        saleRows.sort((a, b) -> b.getEndTime().compareTo(a.getEndTime()));
        tableSales.setItems(FXCollections.observableArrayList(saleRows));

        // Cập nhật Metrics
        lblTotalSales.setText(String.valueOf(totalSales));
        lblActiveSales.setText(String.valueOf(activeSales));
        lblSoldSales.setText(String.valueOf(soldSales));
        lblTotalRevenue.setText(PriceFormatter.formatPrice(totalRevenueSum));
    }

    @FXML public void handleLogout(ActionEvent event) { LoginStateHelper.handleLogout(event); }

    // --- Điều hướng Header ---
    @FXML public void goToHome(ActionEvent event) { SceneNavigator.goToHome(event); }
    @FXML public void goToAuctionList(ActionEvent event) { SceneNavigator.goToAuctionList(event); }
    @FXML public void goToSessions(ActionEvent event) { SceneNavigator.goToSessions(event); }
    @FXML public void goToNews(ActionEvent event) { SceneNavigator.goToNews(event); }
    @FXML public void goToContact(ActionEvent event) { SceneNavigator.goToContact(event); }

    // ================= HELPER MODEL CLASSES =================

    public static class MyBidRow {
        private final String assetName;
        private final String category;
        private final BigDecimal bidAmount;
        private final BigDecimal currentPrice;
        private final LocalDateTime bidTime;
        private final String status;
        private final String result;
        private final Auction auction;

        public MyBidRow(String assetName, String category, BigDecimal bidAmount, BigDecimal currentPrice,
                        LocalDateTime bidTime, String status, String result, Auction auction) {
            this.assetName = assetName;
            this.category = category;
            this.bidAmount = bidAmount;
            this.currentPrice = currentPrice;
            this.bidTime = bidTime;
            this.status = status;
            this.result = result;
            this.auction = auction;
        }

        public String getAssetName() { return assetName; }
        public String getCategory() { return category; }
        public BigDecimal getBidAmount() { return bidAmount; }
        public BigDecimal getCurrentPrice() { return currentPrice; }
        public LocalDateTime getBidTime() { return bidTime; }
        public String getStatus() { return status; }
        public String getResult() { return result; }
        public Auction getAuction() { return auction; }
    }

    public static class MySaleRow {
        private final String assetName;
        private final String category;
        private final BigDecimal startingPrice;
        private final BigDecimal currentPrice;
        private final String highestBidder;
        private final LocalDateTime endTime;
        private final String status;
        private final Auction auction;

        public MySaleRow(String assetName, String category, BigDecimal startingPrice, BigDecimal currentPrice,
                         String highestBidder, LocalDateTime endTime, String status, Auction auction) {
            this.assetName = assetName;
            this.category = category;
            this.startingPrice = startingPrice;
            this.currentPrice = currentPrice;
            this.highestBidder = highestBidder;
            this.endTime = endTime;
            this.status = status;
            this.auction = auction;
        }

        public String getAssetName() { return assetName; }
        public String getCategory() { return category; }
        public BigDecimal getStartingPrice() { return startingPrice; }
        public BigDecimal getCurrentPrice() { return currentPrice; }
        public String getHighestBidder() { return highestBidder; }
        public LocalDateTime getEndTime() { return endTime; }
        public String getStatus() { return status; }
        public Auction getAuction() { return auction; }
    }
}
