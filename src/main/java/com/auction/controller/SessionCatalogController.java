package com.auction.controller;

import com.auction.model.Auction;
import com.auction.network.client.AuctionPayloadMapper;
import com.auction.network.client.AuctionUpdateListener;
import com.auction.network.client.NetworkService;
import com.auction.util.LoginStateHelper;
import com.auction.util.PriceFormatter;
import com.auction.util.SceneNavigator;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.Node;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Controller cho màn hình "Phiên đấu giá" (Session Catalog).
 * Màn hình này phân loại các phiên đấu giá thành 3 nhóm: Đang diễn ra, Sắp diễn ra, và Đã kết thúc.
 */
public class SessionCatalogController {

    // Định dạng hiển thị ngày giờ rút gọn
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Dịch vụ mạng để đồng bộ dữ liệu với server
    private final NetworkService networkService = NetworkService.getInstance();
    
    // Listener lắng nghe cập nhật từ server để tự động render lại danh sách các phiên
    private final AuctionUpdateListener auctionUpdateListener = auctionData -> Platform.runLater(this::renderSessions);

    // --- Các thành phần giao diện FXML ---
    @FXML private TextField searchField;            // Ô tìm kiếm theo từ khóa
    @FXML private ComboBox<String> statusFilter;    // Lọc theo trạng thái
    @FXML private FlowPane runningSessionsContainer;  // Vùng chứa các phiên Đang diễn ra
    @FXML private FlowPane upcomingSessionsContainer; // Vùng chứa các phiên Sắp diễn ra
    @FXML private FlowPane finishedSessionsContainer; // Vùng chứa các phiên Đã kết thúc
    @FXML private Button loginButton;               // Nút Đăng nhập/Đăng xuất

    /**
     * Khởi tạo giao diện, thiết lập các bộ lọc và đăng ký nhận dữ liệu từ server.
     */
    @FXML
    public void initialize() {
        // Thiết lập các tùy chọn lọc trạng thái
        statusFilter.setItems(FXCollections.observableArrayList("Tất cả", "Đang diễn ra", "Sắp diễn ra", "Đã kết thúc"));
        statusFilter.setValue("Tất cả");

        // Cập nhật hiển thị nút đăng nhập theo session hiện tại
        LoginStateHelper.updateLoginButton(loginButton);

        // Lắng nghe thay đổi trên ô tìm kiếm và bộ lọc để cập nhật danh sách ngay lập tức
        searchField.textProperty().addListener((observable, oldValue, newValue) -> Platform.runLater(this::renderSessions));
        statusFilter.valueProperty().addListener((observable, oldValue, newValue) -> Platform.runLater(this::renderSessions));

        // Đăng ký lifecycle để hủy listener khi chuyển màn hình
        registerObserverLifecycle();
        // Đăng ký nhận cập nhật từ NetworkService
        networkService.addAuctionUpdateListener(auctionUpdateListener);
    }

    /**
     * Xử lý sự kiện đăng xuất.
     */
    @FXML
    public void handleLogout(ActionEvent event) {
        LoginStateHelper.handleLogout(event);
    }

    /**
     * Xử lý tìm kiếm khi người dùng nhấn nút hoặc Enter.
     */
    @FXML
    public void handleSearch(ActionEvent event) {
        renderSessions();
    }

    /**
     * Đảm bảo hủy đăng ký listener khi giao diện này bị đóng để tránh rò rỉ bộ nhớ.
     */
    private void registerObserverLifecycle() {
        searchField.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (oldScene != null && newScene == null) {
                networkService.removeAuctionUpdateListener(auctionUpdateListener);
            }
        });
    }

    /**
     * Hàm chính để phân loại và hiển thị danh sách các phiên đấu giá lên giao diện.
     */
    private void renderSessions() {
        // Lấy danh sách đã qua bộ lọc từ server
        List<Auction> filteredAuctions = filterAuctions(loadAuctionsFromServer());
        
        // Nhóm 1: Các phiên đang diễn ra (sắp xếp theo thời gian kết thúc sớm nhất)
        List<Auction> running = filteredAuctions.stream()
                .filter(auction -> "Đang diễn ra".equals(resolveStatusLabel(auction)))
                .sorted(Comparator.comparing(auction -> auction.getItem().getEndTime()))
                .collect(Collectors.toList());
                
        // Nhóm 2: Các phiên sắp diễn ra (sắp xếp theo thời gian bắt đầu sớm nhất)
        List<Auction> upcoming = filteredAuctions.stream()
                .filter(auction -> "Sắp diễn ra".equals(resolveStatusLabel(auction)))
                .sorted(Comparator.comparing(auction -> auction.getItem().getStartTime()))
                .collect(Collectors.toList());
                
        // Nhóm 3: Các phiên đã kết thúc (sắp xếp theo thời gian kết thúc mới nhất)
        List<Auction> finished = filteredAuctions.stream()
                .filter(auction -> "Đã kết thúc".equals(resolveStatusLabel(auction)))
                .sorted(Comparator.comparing((Auction auction) -> auction.getItem().getEndTime()).reversed())
                .collect(Collectors.toList());

        // Hiển thị từng nhóm vào các vùng chứa tương ứng trên UI
        renderSection(runningSessionsContainer, running, "Không có phiên đang diễn ra.");
        renderSection(upcomingSessionsContainer, upcoming, "Không có phiên sắp diễn ra.");
        renderSection(finishedSessionsContainer, finished, "Không có phiên đã kết thúc.");
    }

    /**
     * Lọc danh sách phiên đấu giá dựa trên từ khóa tìm kiếm và trạng thái được chọn.
     */
    private List<Auction> filterAuctions(List<Auction> auctions) {
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        String status = statusFilter.getValue();

        return auctions.stream()
                .filter(auction -> matchesKeyword(auction, keyword))
                .filter(auction -> status == null || "Tất cả".equals(status) || resolveStatusLabel(auction).equals(status))
                .collect(Collectors.toList());
    }

    /**
     * Tải bản sao danh sách đấu giá mới nhất từ server.
     */
    private List<Auction> loadAuctionsFromServer() {
        try {
            List<java.util.Map<String, Object>> snapshot = networkService.getLatestAuctionSnapshot();
            if (snapshot.isEmpty()) {
                snapshot = networkService.getAuctions();
            }
            return AuctionPayloadMapper.toAuctions(snapshot);
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * Kiểm tra từ khóa tìm kiếm trên nhiều trường thông tin.
     */
    private boolean matchesKeyword(Auction auction, String keyword) {
        if (keyword.isBlank()) {
            return true;
        }
        return auction.getItem().getName().toLowerCase(Locale.ROOT).contains(keyword)
                || auction.getId().toLowerCase(Locale.ROOT).contains(keyword)
                || auction.getItem().getId().toLowerCase(Locale.ROOT).contains(keyword)
                || auction.getSeller().getUsername().toLowerCase(Locale.ROOT).contains(keyword);
    }

    /**
     * Render một nhóm phiên vào FlowPane cụ thể.
     */
    private void renderSection(FlowPane container, List<Auction> auctions, String emptyMessage) {
        container.getChildren().clear();
        if (auctions.isEmpty()) {
            container.getChildren().add(createEmptyState(emptyMessage));
            return;
        }

        for (Auction auction : auctions) {
            container.getChildren().add(createSessionCard(auction));
        }
    }

    /**
     * Tạo thẻ giao diện (card) cho một phiên đấu giá.
     */
    private VBox createSessionCard(Auction auction) {
        VBox card = new VBox(12);
        card.getStyleClass().add("catalog-product-card");

        // Ảnh sản phẩm
        Node imageBox = createImageNode(auction);

        // Các nhãn Trạng thái và Danh mục
        Label statusBadge = new Label(resolveStatusLabel(auction));
        statusBadge.getStyleClass().add(resolveStatusBadgeClass(auction));

        Label categoryBadge = new Label(auction.getItem().getCategory());
        categoryBadge.getStyleClass().add("badge-soft");
        HBox badgeRow = new HBox(8, statusBadge, categoryBadge);

        // Tên tài sản
        Label titleLabel = new Label(auction.getItem().getName());
        titleLabel.getStyleClass().add("catalog-product-title");
        titleLabel.setWrapText(true);
        titleLabel.setMinHeight(72);

        // Các dòng thông tin: Giá, Bước giá, Thời gian, Trạng thái đếm ngược, Lượt bid
        Label currentPriceRow = createCatalogInfoRow(auction.isFinished() ? "Giá chốt:" : "Giá hiện tại:", formatPrice(auction.getCurrentPrice()));
        Label stepRow = createCatalogInfoRow("Bước giá:", formatPrice(auction.getMinimumBidStep()));
        Label timeRow = createCatalogInfoRow("Thời gian tổ chức:", auction.getItem().getStartTime().format(DATE_TIME_FORMATTER));
        Label stateRow = createCatalogInfoRow("Trạng thái:", buildTimeMessage(auction));
        Label bidRow = createCatalogInfoRow("Lượt đặt giá:", String.valueOf(auction.getBidHistory().size()));

        // Nút hành động (Mở phiên hoặc Xem tổng kết)
        Button detailButton = new Button(auction.isFinished() ? "Xem tổng kết" : "Mở phiên");
        detailButton.getStyleClass().add("catalog-card-btn");
        detailButton.setMaxWidth(Double.MAX_VALUE);
        detailButton.setOnAction(event -> openAuctionDetail(auction));

        card.getChildren().addAll(imageBox, badgeRow, titleLabel, currentPricePriceRow(), stepRow, timeRow, stateRow, bidRow, detailButton);
        return card;
    }

    // Sửa lỗi cú pháp nhẹ ở dòng trên khi ghép code (giả sử là currentPriceRow đã tạo ở trên)
    private VBox createSessionCardFixed(Auction auction) {
        VBox card = new VBox(12);
        card.getStyleClass().add("catalog-product-card");
        Node imageBox = createImageNode(auction);
        Label statusBadge = new Label(resolveStatusLabel(auction));
        statusBadge.getStyleClass().add(resolveStatusBadgeClass(auction));
        Label categoryBadge = new Label(auction.getItem().getCategory());
        categoryBadge.getStyleClass().add("badge-soft");
        HBox badgeRow = new HBox(8, statusBadge, categoryBadge);
        Label titleLabel = new Label(auction.getItem().getName());
        titleLabel.getStyleClass().add("catalog-product-title");
        titleLabel.setWrapText(true);
        titleLabel.setMinHeight(72);
        Label currentPriceRow = createCatalogInfoRow(auction.isFinished() ? "Giá chốt:" : "Giá hiện tại:", formatPrice(auction.getCurrentPrice()));
        Label stepRow = createCatalogInfoRow("Bước giá:", formatPrice(auction.getMinimumBidStep()));
        Label timeRow = createCatalogInfoRow("Thời gian tổ chức:", auction.getItem().getStartTime().format(DATE_TIME_FORMATTER));
        Label stateRow = createCatalogInfoRow("Trạng thái:", buildTimeMessage(auction));
        Label bidRow = createCatalogInfoRow("Lượt đặt giá:", String.valueOf(auction.getBidHistory().size()));
        Button detailButton = new Button(auction.isFinished() ? "Xem tổng kết" : "Mở phiên");
        detailButton.getStyleClass().add("catalog-card-btn");
        detailButton.setMaxWidth(Double.MAX_VALUE);
        detailButton.setOnAction(event -> openAuctionDetail(auction));
        card.getChildren().addAll(imageBox, badgeRow, titleLabel, currentPriceRow, stepRow, timeRow, stateRow, bidRow, detailButton);
        return card;
    }

    /**
     * Tạo khối hiển thị hình ảnh sản phẩm.
     */
    private Node createImageNode(Auction auction) {
        String imageUrl = auction.getItem().getImageUrl();
        if (imageUrl != null && !imageUrl.isBlank()) {
            StackPane imagePane = new StackPane();
            imagePane.getStyleClass().add("catalog-product-image");
            imagePane.setMinHeight(160);
            imagePane.setPrefHeight(160);
            imagePane.setMaxWidth(Double.MAX_VALUE);

            ImageView imageView = new ImageView(new Image(imageUrl, true));
            imageView.setFitWidth(260);
            imageView.setFitHeight(160);
            imageView.setPreserveRatio(true);
            imageView.setSmooth(true);
            imagePane.getChildren().add(imageView);
            return imagePane;
        }

        // Placeholder nếu không có ảnh
        Label imageBox = new Label(resolveImageText(auction));
        imageBox.getStyleClass().add("catalog-product-image");
        imageBox.setMaxWidth(Double.MAX_VALUE);
        return imageBox;
    }

    /**
     * Tạo một dòng thông tin đơn giản cho card.
     */
    private Label createCatalogInfoRow(String label, String value) {
        Label row = new Label(label + "  " + value);
        row.getStyleClass().add("catalog-info-row");
        row.setWrapText(true);
        return row;
    }

    /**
     * Trả về text mặc định dựa trên danh mục khi thiếu ảnh.
     */
    private String resolveImageText(Auction auction) {
        return switch (auction.getItem().getCategory()) {
            case "Vehicle" -> "VEHICLE";
            case "Art" -> "ART";
            default -> "AUREX";
        };
    }

    /**
     * Tạo thông báo trạng thái trống cho một nhóm.
     */
    private VBox createEmptyState(String message) {
        VBox box = new VBox(8);
        box.getStyleClass().add("content-card");
        Label title = new Label(message);
        title.getStyleClass().add("partner-title");
        box.getChildren().add(title);
        return box;
    }

    /**
     * Xác định nhãn trạng thái thân thiện dựa trên thời gian hiện tại.
     */
    private String resolveStatusLabel(Auction auction) {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(auction.getItem().getStartTime())) {
            return "Sắp diễn ra";
        }
        if (now.isBefore(auction.getItem().getEndTime()) && !auction.isFinished()) {
            return "Đang diễn ra";
        }
        return "Đã kết thúc";
    }

    /**
     * Trả về CSS class tương ứng với trạng thái để trang trí.
     */
    private String resolveStatusBadgeClass(Auction auction) {
        String status = resolveStatusLabel(auction);
        return switch (status) {
            case "Đang diễn ra" -> "badge-live";
            case "Sắp diễn ra" -> "badge-new";
            default -> "badge-hot";
        };
    }

    /**
     * Xây dựng chuỗi thông báo thời gian (Đếm ngược hoặc báo kết thúc).
     */
    private String buildTimeMessage(Auction auction) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = auction.getItem().getStartTime();
        LocalDateTime end = auction.getItem().getEndTime();

        if (now.isBefore(start)) {
            return "Mở sau " + formatDuration(now, start);
        }
        if (now.isBefore(end) && !auction.isFinished()) {
            return "Còn lại " + formatDuration(now, end);
        }
        return "Đã kết thúc";
    }

    /**
     * Định dạng khoảng thời gian giữa hai thời điểm thành chuỗi "X ngày HH:mm".
     */
    private String formatDuration(LocalDateTime from, LocalDateTime to) {
        long days = from.until(to, ChronoUnit.DAYS);
        from = from.plusDays(days);
        long hours = from.until(to, ChronoUnit.HOURS);
        from = from.plusHours(hours);
        long minutes = from.until(to, ChronoUnit.MINUTES);
        return days + " ngày " + String.format("%02d:%02d", hours, minutes);
    }

    /**
     * Chuyển hướng sang màn hình chi tiết phiên đấu giá.
     */
    private void openAuctionDetail(Auction auction) {
        Stage stage = (Stage) searchField.getScene().getWindow();
        SceneNavigator.navigateToAssetDetail(stage, auction);
    }

    /**
     * Định dạng tiền tệ để hiển thị.
     */
    private String formatPrice(BigDecimal amount) {
        return PriceFormatter.formatPrice(amount);
    }

    // --- Các phương thức điều hướng sidebar ---
    @FXML public void goToHome(ActionEvent event) { SceneNavigator.goToHome(event); }
    @FXML public void goToAuctionList(ActionEvent event) { SceneNavigator.goToAuctionList(event); }
    @FXML public void goToSessions(ActionEvent event) { SceneNavigator.goToSessions(event); }
    @FXML public void goToNews(ActionEvent event) { SceneNavigator.goToNews(event); }
    @FXML public void goToContact(ActionEvent event) { SceneNavigator.goToContact(event); }
    @FXML public void goToLogin(ActionEvent event) { SceneNavigator.goToLogin(event); }
}
