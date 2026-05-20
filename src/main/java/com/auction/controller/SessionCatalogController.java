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
 * Controller cho màn hình "Danh sách phiên đấu giá" (Session Catalog).
 * Nhiệm vụ chính:
 * - Hiển thị các phiên đấu giá được phân loại theo trạng thái: Đang diễn ra, Sắp diễn ra, và Đã kết thúc.
 * - Cho phép tìm kiếm và lọc phiên đấu giá theo từ khóa và trạng thái.
 * - Tự động cập nhật giao diện khi có dữ liệu mới từ server thông qua NetworkService.
 */
public class SessionCatalogController {

    // Định dạng hiển thị ngày giờ ngắn gọn trên các thẻ (card)
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Dịch vụ mạng để giao tiếp với Server
    private final NetworkService networkService = NetworkService.getInstance();
    
    // Listener lắng nghe cập nhật dữ liệu từ server: khi có dữ liệu mới sẽ yêu cầu vẽ lại giao diện
    private final AuctionUpdateListener auctionUpdateListener = auctionData -> Platform.runLater(this::renderSessions);

    // --- Các thành phần giao diện FXML ---
    @FXML private TextField searchField;            // Ô nhập từ khóa tìm kiếm
    @FXML private ComboBox<String> statusFilter;    // Dropdown lọc theo trạng thái phiên
    @FXML private FlowPane runningSessionsContainer;  // Vùng chứa các phiên Đang diễn ra
    @FXML private FlowPane upcomingSessionsContainer; // Vùng chứa các phiên Sắp diễn ra
    @FXML private FlowPane finishedSessionsContainer; // Vùng chứa các phiên Đã kết thúc
    @FXML private Button loginButton;               // Nút Đăng nhập/Đăng xuất trên thanh menu

    /**
     * Khởi tạo giao diện khi scene được load.
     */
    @FXML
    public void initialize() {
        // Thiết lập các lựa chọn cho bộ lọc trạng thái
        statusFilter.setItems(FXCollections.observableArrayList("Tất cả", "Đang diễn ra", "Sắp diễn ra", "Đã kết thúc"));
        statusFilter.setValue("Tất cả");

        // Cập nhật nhãn và hành động cho nút Login dựa trên trạng thái session
        LoginStateHelper.updateLoginButton(loginButton);

        // Đăng ký sự kiện: khi người dùng nhập text hoặc đổi bộ lọc, danh sách sẽ tự động cập nhật
        searchField.textProperty().addListener((observable, oldValue, newValue) -> Platform.runLater(this::renderSessions));
        statusFilter.valueProperty().addListener((observable, oldValue, newValue) -> Platform.runLater(this::renderSessions));

        // Đăng ký nhận thông báo từ NetworkService và quản lý vòng đời (Lifecycle)
        registerObserverLifecycle();
        networkService.addAuctionUpdateListener(auctionUpdateListener);
    }

    /**
     * Xử lý khi nhấn nút Đăng xuất.
     */
    @FXML
    public void handleLogout(ActionEvent event) {
        LoginStateHelper.handleLogout(event);
    }

    /**
     * Xử lý khi người dùng thực hiện thao tác tìm kiếm (nhấn nút hoặc Enter).
     */
    @FXML
    public void handleSearch(ActionEvent event) {
        renderSessions();
    }

    /**
     * Tự động hủy đăng ký listener khi giao diện này không còn được hiển thị (tránh rò rỉ bộ nhớ).
     */
    private void registerObserverLifecycle() {
        searchField.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (oldScene != null && newScene == null) {
                networkService.removeAuctionUpdateListener(auctionUpdateListener);
            }
        });
    }

    /**
     * Hàm chính để lấy dữ liệu từ server, phân loại theo trạng thái và hiển thị lên UI.
     */
    private void renderSessions() {
        // Lấy danh sách thô từ server và đi qua bộ lọc từ khóa/trạng thái
        List<Auction> filteredAuctions = filterAuctions(loadAuctionsFromServer());
        
        // Nhóm các phiên Đang diễn ra: Sắp xếp theo thời gian kết thúc sớm nhất lên đầu
        List<Auction> running = filteredAuctions.stream()
                .filter(auction -> "Đang diễn ra".equals(resolveStatusLabel(auction)))
                .sorted(Comparator.comparing(auction -> auction.getItem().getEndTime()))
                .collect(Collectors.toList());
                
        // Nhóm các phiên Sắp diễn ra: Sắp xếp theo thời gian bắt đầu sớm nhất lên đầu
        List<Auction> upcoming = filteredAuctions.stream()
                .filter(auction -> "Sắp diễn ra".equals(resolveStatusLabel(auction)))
                .sorted(Comparator.comparing(auction -> auction.getItem().getStartTime()))
                .collect(Collectors.toList());
                
        // Nhóm các phiên Đã kết thúc: Sắp xếp theo thời gian kết thúc mới nhất lên đầu
        List<Auction> finished = filteredAuctions.stream()
                .filter(auction -> "Đã kết thúc".equals(resolveStatusLabel(auction)))
                .sorted(Comparator.comparing((Auction auction) -> auction.getItem().getEndTime()).reversed())
                .collect(Collectors.toList());

        // Hiển thị dữ liệu vào từng vùng tương ứng trên màn hình
        renderSection(runningSessionsContainer, running, "Không có phiên đang diễn ra.");
        renderSection(upcomingSessionsContainer, upcoming, "Không có phiên sắp diễn ra.");
        renderSection(finishedSessionsContainer, finished, "Không có phiên đã kết thúc.");
    }

    /**
     * Lọc danh sách phiên đấu giá dựa trên dữ liệu người dùng nhập/chọn trên UI.
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
     * Tải dữ liệu phiên đấu giá mới nhất từ NetworkService.
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
     * Kiểm tra xem thông tin phiên đấu giá có chứa từ khóa tìm kiếm hay không.
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
     * Đưa danh sách các phiên vào một vùng chứa FlowPane và xử lý trường hợp danh sách rỗng.
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
     * Tạo thẻ giao diện (card) hiển thị tóm tắt thông tin của một phiên đấu giá.
     */
    private VBox createSessionCard(Auction auction) {
        VBox card = new VBox(12);
        card.getStyleClass().add("catalog-product-card");

        // Thành phần 1: Hình ảnh sản phẩm
        Node imageBox = createImageNode(auction);

        // Thành phần 2: Các nhãn trạng thái và danh mục
        Label statusBadge = new Label(resolveStatusLabel(auction));
        statusBadge.getStyleClass().add(resolveStatusBadgeClass(auction));

        Label categoryBadge = new Label(auction.getItem().getCategory());
        categoryBadge.getStyleClass().add("badge-soft");
        HBox badgeRow = new HBox(8, statusBadge, categoryBadge);

        // Thành phần 3: Tên sản phẩm (giới hạn chiều cao để đồng đều các card)
        Label titleLabel = new Label(auction.getItem().getName());
        titleLabel.getStyleClass().add("catalog-product-title");
        titleLabel.setWrapText(true);
        titleLabel.setMinHeight(72);

        // Thành phần 4: Các dòng thông tin chi tiết (Giá, Bước giá, Thời gian...)
        boolean isEnded = "Đã kết thúc".equals(resolveStatusLabel(auction));
        Label currentPriceRow = createCatalogInfoRow(isEnded ? "Giá chốt:" : "Giá hiện tại:", formatPrice(auction.getCurrentPrice()));
        Label stepRow = createCatalogInfoRow("Bước giá:", formatPrice(auction.getMinimumBidStep()));
        Label timeRow = createCatalogInfoRow("Thời gian tổ chức:", auction.getItem().getStartTime().format(DATE_TIME_FORMATTER));
        Label stateRow = createCatalogInfoRow("Trạng thái:", buildTimeMessage(auction));
        Label bidRow = createCatalogInfoRow("Lượt đặt giá:", String.valueOf(auction.getBidHistory().size()));

        // Thành phần 5: Nút bấm hành động (Xem chi tiết hoặc tham gia phòng đấu giá)
        Button detailButton = new Button(isEnded ? "Xem tổng kết" : "Mở phiên");
        detailButton.getStyleClass().add("catalog-card-btn");
        detailButton.setMaxWidth(Double.MAX_VALUE);
        detailButton.setOnAction(event -> openAuctionDetail(auction));

        card.getChildren().addAll(imageBox, badgeRow, titleLabel, currentPriceRow, stepRow, timeRow, stateRow, bidRow, detailButton);
        return card;
    }

    /**
     * Tạo khối chứa hình ảnh cho sản phẩm. Nếu không có URL ảnh sẽ dùng text placeholder.
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

        Label imageBox = new Label(resolveImageText(auction));
        imageBox.getStyleClass().add("catalog-product-image");
        imageBox.setMaxWidth(Double.MAX_VALUE);
        return imageBox;
    }

    /**
     * Hàm tiện ích tạo một dòng thông tin văn bản trên thẻ.
     */
    private Label createCatalogInfoRow(String label, String value) {
        Label row = new Label(label + "  " + value);
        row.getStyleClass().add("catalog-info-row");
        row.setWrapText(true);
        return row;
    }

    /**
     * Quyết định text hiển thị thay cho ảnh dựa trên danh mục.
     */
    private String resolveImageText(Auction auction) {
        return switch (auction.getItem().getCategory()) {
            case "Vehicle" -> "VEHICLE";
            case "Art" -> "ART";
            default -> "AUREX";
        };
    }

    /**
     * Hiển thị trạng thái rỗng khi một nhóm phiên không có dữ liệu.
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
     * Xác định nhãn trạng thái thân thiện (Tiếng Việt) dựa trên thời gian thực tế.
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
     * Trả về lớp CSS tương ứng để hiển thị màu sắc trạng thái.
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
     * Xây dựng nội dung đếm ngược thời gian hoặc báo đã kết thúc.
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
     * Chuyển đổi khoảng thời gian thành định dạng "X ngày HH:mm".
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
     * Chuyển hướng sang màn hình xem chi tiết tài sản/tham gia đấu giá.
     */
    private void openAuctionDetail(Auction auction) {
        Stage stage = (Stage) searchField.getScene().getWindow();
        SceneNavigator.navigateToAssetDetail(stage, auction);
    }

    /**
     * Định dạng số tiền để hiển thị (ví dụ: 1.000.000 VNĐ).
     */
    private String formatPrice(BigDecimal amount) {
        return PriceFormatter.formatPrice(amount);
    }

    // --- Các phương thức điều hướng Sidebar Menu ---
    @FXML public void goToHome(ActionEvent event) { SceneNavigator.goToHome(event); }
    @FXML public void goToAuctionList(ActionEvent event) { SceneNavigator.goToAuctionList(event); }
    @FXML public void goToSessions(ActionEvent event) { SceneNavigator.goToSessions(event); }
    @FXML public void goToNews(ActionEvent event) { SceneNavigator.goToNews(event); }
    @FXML public void goToContact(ActionEvent event) { SceneNavigator.goToContact(event); }
    @FXML public void goToLogin(ActionEvent event) { SceneNavigator.goToLogin(event); }
}
