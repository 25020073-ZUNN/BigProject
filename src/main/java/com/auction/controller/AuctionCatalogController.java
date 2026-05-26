package com.auction.controller;

import com.auction.model.Auction;
import com.auction.model.item.Item;
import com.auction.network.client.AuctionPayloadMapper;
import com.auction.network.client.AuctionUpdateListener;
import com.auction.network.client.NetworkService;
import com.auction.util.SceneNavigator;
import com.auction.util.LoginStateHelper;
import com.auction.util.PriceFormatter;
import com.auction.util.AuctionImageLoader;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.application.Platform;
//****Thêm
import javafx.concurrent.Task;
import javafx.animation.PauseTransition;
import javafx.util.Duration;

import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.Node;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.Priority;
import javafx.geometry.Pos;
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
 * Controller cho màn hình "Tài sản đấu giá" (Auction Catalog).
 *
 * Mục tiêu của màn hình này:
 * - Hiển thị danh sách các tài sản (phiên đấu giá) đang có trên hệ thống.
 * - Luôn đọc danh sách tài sản thật từ Server snapshot thay vì sử dụng dữ liệu tĩnh (hard-code).
 * - Cung cấp tính năng tìm kiếm và lọc theo danh mục, trạng thái dựa trên dữ liệu thực.
 */
public class AuctionCatalogController {

    // Định dạng ngày giờ hiển thị trên giao diện
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    // Dịch vụ mạng để giao tiếp với server và lấy dữ liệu
    private final NetworkService networkService = NetworkService.getInstance();
    
    // Listener lắng nghe sự kiện cập nhật dữ liệu từ server và cập nhật UI trên JavaFX Application Thread
    private final AuctionUpdateListener auctionUpdateListener = auctionData -> Platform.runLater(this::renderAuctions);

    // ***Thêm
    private final PauseTransition searchDebounce = new PauseTransition(Duration.millis(300));

    // --- Các thành phần giao diện FXML ---
    @FXML private TextField searchField;            // Ô nhập từ khóa tìm kiếm
    @FXML private ComboBox<String> categoryFilter;  // Dropdown lọc theo danh mục sản phẩm
    @FXML private ComboBox<String> statusFilter;    // Dropdown lọc theo trạng thái phiên đấu giá
    @FXML private ComboBox<String> sortFilter;      // Dropdown sắp xếp kết quả hiển thị
    @FXML private Label resultCountLabel;           // Nhãn hiển thị số lượng kết quả tìm được
    @FXML private FlowPane auctionListContainer;    // Vùng chứa các thẻ (card) hiển thị phiên đấu giá
    @FXML private Button loginButton;               // Nút Đăng nhập / Đăng xuất

    /**
     * Phương thức khởi tạo được gọi tự động khi file FXML được tải.
     * Dùng để thiết lập dữ liệu ban đầu và đăng ký các bộ lắng nghe sự kiện (listeners).
     */
    @FXML
    public void initialize() {
        // Cấu hình dữ liệu cho dropdown lọc danh mục
        categoryFilter.setItems(FXCollections.observableArrayList("Tất cả", "Electronics", "Vehicle", "Art"));
        categoryFilter.setValue("Tất cả");

        // Cập nhật trạng thái hiển thị của nút đăng nhập
        LoginStateHelper.updateLoginButton(loginButton);

        // Cấu hình dữ liệu cho dropdown lọc trạng thái
        statusFilter.setItems(FXCollections.observableArrayList("Tất cả", "Sắp diễn ra", "Đang diễn ra", "Đã kết thúc"));
        statusFilter.setValue("Tất cả");
        
        // Cấu hình dữ liệu cho dropdown sắp xếp
        sortFilter.setItems(FXCollections.observableArrayList(
                "Ưu tiên phiên đang diễn ra",
                "Sắp kết thúc trước",
                "Mới tạo/sắp mở trước",
                "Giá cao trước"
        ));
        sortFilter.setValue("Ưu tiên phiên đang diễn ra");

        // Đăng ký sự kiện thay đổi giá trị bộ lọc: khi có thay đổi, tiến hành render lại danh sách
        //searchField.textProperty().addListener((observable, oldValue, newValue) -> Platform.runLater(this::renderAuctions));

        // ****thay bằng
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            searchDebounce.setOnFinished(event -> renderAuctions());
            searchDebounce.playFromStart();
        });
        //***

        //categoryFilter.valueProperty().addListener((observable, oldValue, newValue) -> Platform.runLater(this::renderAuctions));
        //statusFilter.valueProperty().addListener((observable, oldValue, newValue) -> Platform.runLater(this::renderAuctions));
        //sortFilter.valueProperty().addListener((observable, oldValue, newValue) -> Platform.runLater(this::renderAuctions));

        //****thay bằng
        categoryFilter.valueProperty().addListener((observable, oldValue, newValue) -> renderAuctions());
        statusFilter.valueProperty().addListener((observable, oldValue, newValue) -> renderAuctions());
        sortFilter.valueProperty().addListener((observable, oldValue, newValue) -> renderAuctions());
        //****

        // Quản lý vòng đời của observer để tránh rò rỉ bộ nhớ
        registerObserverLifecycle();
        // Đăng ký nhận bản cập nhật danh sách đấu giá từ server
        networkService.addAuctionUpdateListener(auctionUpdateListener);
    }

    /**
     * Xử lý sự kiện đăng xuất.
     */
    @FXML
    public void handleLogout(ActionEvent event) { LoginStateHelper.handleLogout(event); }

    /**
     * Xử lý sự kiện nhấn nút Tìm kiếm hoặc Enter trong ô tìm kiếm.
     */
    @FXML
    public void handleSearch(ActionEvent event) {
        renderAuctions();
    }

    /**
     * Phương thức chính để lấy dữ liệu, lọc, sắp xếp và hiển thị lên giao diện.
     */
   /* private void renderAuctions() {
        // Lấy danh sách từ server, lọc theo điều kiện và sau đó sắp xếp
        List<Auction> filteredAuctions = sortAuctions(filterAuctions(loadAuctionsFromServer()));

        // Cập nhật số lượng hiển thị
        resultCountLabel.setText("Hiển thị " + filteredAuctions.size() + " sản phẩm");
        
        // Xóa các card cũ
        auctionListContainer.getChildren().clear();

        // Nếu không có kết quả nào, hiển thị thông báo rỗng
        if (filteredAuctions.isEmpty()) {
            auctionListContainer.getChildren().add(createEmptyState());
            return;
        }

        // Tạo thẻ (card) cho từng phiên đấu giá và thêm vào vùng chứa
        //for (Auction auction : filteredAuctions) {
        //***thay bằng
        for (Auction auction : filteredAuctions.stream().limit(20).toList()) {
            auctionListContainer.getChildren().add(createAuctionCard(auction));
        }
    }*/
    //*** Thay bằng
    private void renderAuctions() {

        Task<List<Auction>> task = new Task<>() {
            @Override
            protected List<Auction> call() {
                return sortAuctions(filterAuctions(loadAuctionsFromServer()));
            }
        };

        task.setOnSucceeded(event -> {

            List<Auction> filteredAuctions = task.getValue();

            resultCountLabel.setText("Hiển thị " + filteredAuctions.size() + " sản phẩm");

            auctionListContainer.getChildren().clear();

            if (filteredAuctions.isEmpty()) {
                auctionListContainer.getChildren().add(createEmptyState());
                return;
            }

            for (Auction auction : filteredAuctions.stream().limit(20).toList()) {
                auctionListContainer.getChildren().add(createAuctionCard(auction));
            }
        });

        task.setOnFailed(event -> {
            task.getException().printStackTrace();
        });

        new Thread(task).start();
    }

    /**
     * Quản lý vòng đời đăng ký listener. Hủy đăng ký khi giao diện hiện tại không còn hiển thị (được đổi sang scene khác).
     */
    private void registerObserverLifecycle() {
        searchField.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (oldScene != null && newScene == null) {
                networkService.removeAuctionUpdateListener(auctionUpdateListener);
            }
        });
    }

    /**
     * Lọc danh sách đấu giá theo từ khóa, danh mục và trạng thái do người dùng chọn.
     */
    private List<Auction> filterAuctions(List<Auction> auctions) {
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        String category = categoryFilter.getValue();
        String status = statusFilter.getValue();

        /*
         * Ghi chú quan trọng:
         * Controller này không đọc Database trực tiếp nữa.
         * Nó chỉ lọc dựa trên snapshot (bản sao dữ liệu mới nhất) do server broadcast xuống qua NetworkService.
         */
        return auctions.stream()
                .filter(auction -> matchesKeyword(auction, keyword))
                .filter(auction -> matchesCategory(auction, category))
                .filter(auction -> matchesStatus(auction, status))
                .collect(Collectors.toList());
    }

    /**
     * Sắp xếp danh sách đấu giá theo tiêu chí người dùng đã chọn trong sortFilter.
     */
    private List<Auction> sortAuctions(List<Auction> auctions) {
        String sort = sortFilter == null ? null : sortFilter.getValue();
        Comparator<Auction> comparator = switch (sort == null ? "" : sort) {
            case "Sắp kết thúc trước" -> Comparator
                    .comparing((Auction auction) -> auction.isFinished())
                    .thenComparing(auction -> auction.getItem().getEndTime());
            case "Mới tạo/sắp mở trước" -> Comparator.comparing((Auction auction) -> auction.getItem().getStartTime());
            case "Giá cao trước" -> Comparator.comparing(Auction::getCurrentPrice).reversed();
            default -> Comparator // Mặc định là ưu tiên phiên đang diễn ra
                    .comparingInt(this::statusPriority)
                    .thenComparing(auction -> auction.getItem().getEndTime());
        };
        return auctions.stream().sorted(comparator).collect(Collectors.toList());
    }

    /**
     * Gán trọng số ưu tiên cho các trạng thái để dùng trong việc sắp xếp mặc định.
     */
    private int statusPriority(Auction auction) {
        return switch (resolveStatusLabel(auction)) {
            case "Đang diễn ra" -> 0; // Ưu tiên hiển thị trên cùng
            case "Sắp diễn ra" -> 1;
            default -> 2;           // Đã kết thúc
        };
    }

    /**
     * Lấy danh sách phiên đấu giá từ NetworkService (dữ liệu đang có trên server).
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
     * Kiểm tra xem thông tin của phiên đấu giá có khớp với từ khóa tìm kiếm hay không.
     */
    private boolean matchesKeyword(Auction auction, String keyword) {
        if (keyword.isBlank()) {
            return true;
        }

        Item item = auction.getItem();
        String statusLabel = resolveStatusLabel(auction);
        
        // Tìm kiếm linh hoạt trên nhiều trường (Tên, mô tả, ID, Người bán, Trạng thái...)
        return item.getName().toLowerCase(Locale.ROOT).contains(keyword)
                || item.getDescription().toLowerCase(Locale.ROOT).contains(keyword)
                || item.getId().toLowerCase(Locale.ROOT).contains(keyword)
                || auction.getId().toLowerCase(Locale.ROOT).contains(keyword)
                || auction.getSeller().getUsername().toLowerCase(Locale.ROOT).contains(keyword)
                || statusLabel.toLowerCase(Locale.ROOT).contains(keyword);
    }

    /**
     * Kiểm tra xem danh mục của phiên đấu giá có khớp với lựa chọn danh mục hay không.
     */
    private boolean matchesCategory(Auction auction, String category) {
        return category == null || "Tất cả".equals(category) || auction.getItem().getCategory().equalsIgnoreCase(category);
    }

    /**
     * Kiểm tra xem trạng thái của phiên đấu giá có khớp với lựa chọn trạng thái hay không.
     */
    private boolean matchesStatus(Auction auction, String status) {
        return status == null || "Tất cả".equals(status) || resolveStatusLabel(auction).equals(status);
    }

    /**
     * Tạo một thẻ giao diện (VBox) đại diện cho một phiên đấu giá duy nhất.
     */
    private VBox createAuctionCard(Auction auction) {
        VBox card = new VBox(12);
        card.getStyleClass().add("catalog-product-card");

        // Tiêu đề tài sản đấu giá
        Label titleLabel = new Label(auction.getItem().getName());
        titleLabel.getStyleClass().add("catalog-product-title");
        titleLabel.setWrapText(true);
        titleLabel.setMinHeight(52);
        titleLabel.setPrefHeight(52);
        titleLabel.setMaxHeight(52);

        // Hình ảnh đại diện
        Node imageBox = createImageNode(auction);

        // Các nhãn đánh dấu (Trạng thái và Danh mục)
        Label statusBadge = new Label(resolveStatusLabel(auction));
        statusBadge.getStyleClass().add(resolveStatusBadgeClass(auction));

        Label categoryBadge = new Label(auction.getItem().getCategory());
        categoryBadge.getStyleClass().add("badge-soft");

        HBox badgeRow = new HBox(8, statusBadge, categoryBadge);

        // Thành phần: Giá và các dòng thông tin chi tiết
        boolean isEnded = "Đã kết thúc".equals(resolveStatusLabel(auction));
        boolean isUpcoming = "Sắp diễn ra".equals(resolveStatusLabel(auction));

        // Price VBox
        VBox priceBox = new VBox(2);
        priceBox.getStyleClass().add("catalog-price-container");

        String priceTitle = isEnded ? "GIÁ CHỐT" : (isUpcoming ? "GIÁ KHỞI ĐIỂM" : "GIÁ HIỆN TẠI");
        Label priceLabel = new Label(priceTitle);
        priceLabel.getStyleClass().add("catalog-price-label");

        Label priceValue = new Label(formatPrice(isUpcoming ? auction.getStartingPrice() : auction.getCurrentPrice()));
        priceValue.getStyleClass().add("catalog-price-value");

        priceBox.getChildren().addAll(priceLabel, priceValue);

        // Stats HBox capsule badge
        HBox statsBox = new HBox();
        statsBox.getStyleClass().add("catalog-stats-box");

        // Left column in stats: Countdown / Status time
        String timeText;
        LocalDateTime now = LocalDateTime.now();
        if (isUpcoming) {
            timeText = "📅 " + auction.getItem().getStartTime().format(DateTimeFormatter.ofPattern("dd/MM HH:mm"));
        } else if (isEnded) {
            timeText = "🏁 Đã kết thúc";
        } else {
            timeText = "⏳ " + formatDurationShort(now, auction.getItem().getEndTime());
        }
        Label timeBadge = new Label(timeText);
        timeBadge.getStyleClass().add("catalog-stats-text");

        // Right column in stats: Bids count or Status
        String rightText;
        if (isUpcoming) {
            rightText = "⏳ Chờ mở";
        } else {
            rightText = "🔨 " + auction.getBidHistory().size() + " lượt";
        }
        Label rightBadge = new Label(rightText);
        rightBadge.getStyleClass().add("catalog-stats-text");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        statsBox.getChildren().addAll(timeBadge, spacer, rightBadge);

        // Footer details (Bước giá & Người bán)
        HBox footerBox = new HBox(12);
        footerBox.setAlignment(Pos.CENTER_LEFT);

        Label stepLabel = new Label("Bước giá: " + formatPrice(auction.getMinimumBidStep()));
        stepLabel.getStyleClass().add("catalog-footer-stat");

        Label sellerLabel = new Label("Người bán: " + auction.getSeller().getUsername());
        sellerLabel.getStyleClass().add("catalog-footer-stat");

        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);

        footerBox.getChildren().addAll(stepLabel, footerSpacer, sellerLabel);

        // Nút hành động
        Button detailButton = new Button(isEnded ? "Xem tổng kết" : "Xem chi tiết");
        detailButton.getStyleClass().add("catalog-card-btn");
        detailButton.setMaxWidth(Double.MAX_VALUE);
        detailButton.setOnAction(event -> openAuctionDetail(auction));

        card.getChildren().addAll(imageBox, badgeRow, titleLabel, priceBox, statsBox, footerBox, detailButton);
        return card;
    }

    private String formatDurationShort(LocalDateTime from, LocalDateTime to) {
        if (from.isAfter(to)) {
            return "0 phút";
        }
        long days = from.until(to, ChronoUnit.DAYS);
        LocalDateTime temp = from.plusDays(days);
        long hours = temp.until(to, ChronoUnit.HOURS);
        temp = temp.plusHours(hours);
        long minutes = temp.until(to, ChronoUnit.MINUTES);

        if (days > 0) {
            return days + " ngày " + hours + "h";
        } else if (hours > 0) {
            return hours + " giờ " + minutes + "p";
        } else {
            return minutes + " phút";
        }
    }

    /**
     * Khởi tạo giao diện chứa hình ảnh. Nếu không có URL ảnh, sẽ tạo một khối giả.
     */
    private Node createImageNode(Auction auction) {
        String imageUrl = auction.getItem().getImageUrl();
        if (imageUrl != null && !imageUrl.isBlank()) {
            StackPane imagePane = new StackPane();
            imagePane.getStyleClass().add("catalog-product-image");
            imagePane.setMinHeight(160);
            imagePane.setPrefHeight(160);
            imagePane.setMaxWidth(Double.MAX_VALUE);

            ImageView imageView = new ImageView(AuctionImageLoader.thumbnail(imageUrl));
            imageView.setFitWidth(260);
            imageView.setFitHeight(160);
            imageView.setPreserveRatio(true);
            imageView.setSmooth(true);
            imagePane.getChildren().add(imageView);
            return imagePane;
        }

        // Tạo placeholder cho hình ảnh (text hiển thị mã/chữ thay cho ảnh)
        Label imageBox = new Label(resolveImageText(auction));
        imageBox.getStyleClass().add("catalog-product-image");
        imageBox.setMaxWidth(Double.MAX_VALUE);
        return imageBox;
    }

    /**
     * Tạo một dòng thông tin gồm tiêu đề và giá trị.
     */
    private Label createCatalogInfoRow(String label, String value) {
        Label row = new Label(label + "  " + value);
        row.getStyleClass().add("catalog-info-row");
        row.setWrapText(true);
        return row;
    }

    /**
     * Trả về text mặc định khi không có hình ảnh, dựa vào danh mục.
     */
    private String resolveImageText(Auction auction) {
        return switch (auction.getItem().getCategory()) {
            case "Vehicle" -> "VEHICLE";
            case "Art" -> "ART";
            default -> "UET";
        };
    }

    /**
     * Hàm tiện ích rút gọn ID dài cho hiển thị dễ nhìn.
     */
    private String shortId(String id) {
        if (id == null || id.length() <= 8) {
            return id == null ? "" : id;
        }
        return id.substring(0, 8);
    }

    /**
     * Tạo giao diện thông báo khi danh sách hiển thị rỗng (Không có kết quả lọc/tìm kiếm).
     */
    private VBox createEmptyState() {
        VBox emptyBox = new VBox(10);
        emptyBox.getStyleClass().add("content-card");

        Label title = new Label("Không tìm thấy tài sản phù hợp");
        title.getStyleClass().add("partner-title");

        Label hint = new Label("Hãy đổi từ khóa tìm kiếm hoặc tạo thêm tài sản đấu giá thật từ màn hình tạo phiên.");
        hint.getStyleClass().add("partner-text");
        hint.setWrapText(true);

        Button createButton = new Button("Thêm tài sản đấu giá");
        createButton.getStyleClass().add("primary-btn");
        createButton.setOnAction(this::goToCreateAuction);

        emptyBox.getChildren().addAll(title, hint, createButton);
        return emptyBox;
    }

    /**
     * Phân tích và chuyển đổi logic thời gian thành chuỗi trạng thái thân thiện.
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
     * Trả về lớp CSS tương ứng với trạng thái để trang trí giao diện màu sắc khác nhau.
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
     * Xử lý sự kiện nhấn vào nút "Xem chi tiết", chuyển hướng sang màn hình xem thông tin chi tiết tài sản.
     */
    private void openAuctionDetail(Auction auction) {
        Stage stage = (Stage) searchField.getScene().getWindow();
        // Điều hướng tới trang chi tiết tài sản (Asset Detail) trung gian, nơi người dùng sẽ quyết định tham gia hay không
        SceneNavigator.navigateToAssetDetail(stage, auction);
    }

    /**
     * Định dạng kiểu tiền tệ (ví dụ 1.000.000 VNĐ).
     */
    private String formatPrice(BigDecimal amount) {
        return PriceFormatter.formatPrice(amount);
    }

    // --- Các phương thức điều hướng Sidebar (Menu) ---
    @FXML public void goToHome(ActionEvent event) { SceneNavigator.goToHome(event); }
    @FXML public void goToAuctionList(ActionEvent event) { SceneNavigator.goToAuctionList(event); }
    @FXML public void goToSessions(ActionEvent event) { SceneNavigator.goToSessions(event); }
    @FXML public void goToNews(ActionEvent event) { SceneNavigator.goToNews(event); }
    @FXML public void goToContact(ActionEvent event) { SceneNavigator.goToContact(event); }
    @FXML public void goToLogin(ActionEvent event) { SceneNavigator.goToLogin(event); }
    @FXML public void goToCreateAuction(ActionEvent event) { SceneNavigator.goToCreateAuction(event); }
}
