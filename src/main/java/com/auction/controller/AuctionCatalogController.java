package com.auction.controller;

import com.auction.model.Auction;
import com.auction.model.item.Item;
import com.auction.network.client.AuctionPayloadMapper;
import com.auction.network.client.AuctionUpdateListener;
import com.auction.network.client.NetworkService;
import com.auction.util.SceneNavigator;
import com.auction.util.LoginStateHelper;
import com.auction.util.PriceFormatter;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Controller cho màn hình "Tài sản đấu giá".
 *
 * Mục tiêu của màn hình này:
 * - Không hiển thị card mẫu hard-code nữa.
 * - Luôn đọc danh sách tài sản thật từ Server snapshot.
 * - Cho phép tìm kiếm và lọc theo danh mục/trạng thái ngay trên dữ liệu thật.
 */
public class AuctionCatalogController {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");


    private final NetworkService networkService = NetworkService.getInstance();
    private final AuctionUpdateListener auctionUpdateListener = auctionData -> Platform.runLater(this::renderAuctions);

    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> categoryFilter;
    @FXML
    private ComboBox<String> statusFilter;
    @FXML
    private ComboBox<String> sortFilter;
    @FXML
    private Label resultCountLabel;
    @FXML
    private FlowPane auctionListContainer;
    @FXML
    private Button loginButton;

    @FXML
    public void initialize() {
        categoryFilter.setItems(FXCollections.observableArrayList("Tất cả", "Electronics", "Vehicle", "Art"));
        categoryFilter.setValue("Tất cả");

        LoginStateHelper.updateLoginButton(loginButton);

        statusFilter.setItems(FXCollections.observableArrayList("Tất cả", "Sắp diễn ra", "Đang diễn ra", "Đã kết thúc"));
        statusFilter.setValue("Tất cả");
        sortFilter.setItems(FXCollections.observableArrayList(
                "Ưu tiên phiên đang diễn ra",
                "Sắp kết thúc trước",
                "Mới tạo/sắp mở trước",
                "Giá cao trước"
        ));
        sortFilter.setValue("Ưu tiên phiên đang diễn ra");

        searchField.textProperty().addListener((observable, oldValue, newValue) -> Platform.runLater(this::renderAuctions));
        categoryFilter.valueProperty().addListener((observable, oldValue, newValue) -> Platform.runLater(this::renderAuctions));
        statusFilter.valueProperty().addListener((observable, oldValue, newValue) -> Platform.runLater(this::renderAuctions));
        sortFilter.valueProperty().addListener((observable, oldValue, newValue) -> Platform.runLater(this::renderAuctions));

        registerObserverLifecycle();
        networkService.addAuctionUpdateListener(auctionUpdateListener);
    }

    @FXML
    public void handleLogout(ActionEvent event) { LoginStateHelper.handleLogout(event); }

    @FXML
    public void handleSearch(ActionEvent event) {
        renderAuctions();
    }

    private void renderAuctions() {
        List<Auction> filteredAuctions = sortAuctions(filterAuctions(loadAuctionsFromServer()));

        resultCountLabel.setText("Hiển thị " + filteredAuctions.size() + " sản phẩm");
        auctionListContainer.getChildren().clear();

        if (filteredAuctions.isEmpty()) {
            auctionListContainer.getChildren().add(createEmptyState());
            return;
        }

        for (Auction auction : filteredAuctions) {
            auctionListContainer.getChildren().add(createAuctionCard(auction));
        }
    }

    private void registerObserverLifecycle() {
        searchField.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (oldScene != null && newScene == null) {
                networkService.removeAuctionUpdateListener(auctionUpdateListener);
            }
        });
    }

    private List<Auction> filterAuctions(List<Auction> auctions) {
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        String category = categoryFilter.getValue();
        String status = statusFilter.getValue();

        /*
         * Ghi chú quan trọng:
         * Controller này không đọc DB trực tiếp nữa.
         * Nó chỉ lọc snapshot mới nhất do server broadcast xuống qua NetworkService.
         */
        return auctions.stream()
                .filter(auction -> matchesKeyword(auction, keyword))
                .filter(auction -> matchesCategory(auction, category))
                .filter(auction -> matchesStatus(auction, status))
                .collect(Collectors.toList());
    }

    private List<Auction> sortAuctions(List<Auction> auctions) {
        String sort = sortFilter == null ? null : sortFilter.getValue();
        Comparator<Auction> comparator = switch (sort == null ? "" : sort) {
            case "Sắp kết thúc trước" -> Comparator
                    .comparing((Auction auction) -> auction.isFinished())
                    .thenComparing(auction -> auction.getItem().getEndTime());
            case "Mới tạo/sắp mở trước" -> Comparator.comparing((Auction auction) -> auction.getItem().getStartTime());
            case "Giá cao trước" -> Comparator.comparing(Auction::getCurrentPrice).reversed();
            default -> Comparator
                    .comparingInt(this::statusPriority)
                    .thenComparing(auction -> auction.getItem().getEndTime());
        };
        return auctions.stream().sorted(comparator).collect(Collectors.toList());
    }

    private int statusPriority(Auction auction) {
        return switch (resolveStatusLabel(auction)) {
            case "Đang diễn ra" -> 0;
            case "Sắp diễn ra" -> 1;
            default -> 2;
        };
    }

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

    private boolean matchesKeyword(Auction auction, String keyword) {
        if (keyword.isBlank()) {
            return true;
        }

        Item item = auction.getItem();
        String statusLabel = resolveStatusLabel(auction);
        return item.getName().toLowerCase(Locale.ROOT).contains(keyword)
                || item.getDescription().toLowerCase(Locale.ROOT).contains(keyword)
                || item.getId().toLowerCase(Locale.ROOT).contains(keyword)
                || auction.getId().toLowerCase(Locale.ROOT).contains(keyword)
                || auction.getSeller().getUsername().toLowerCase(Locale.ROOT).contains(keyword)
                || statusLabel.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private boolean matchesCategory(Auction auction, String category) {
        return category == null || "Tất cả".equals(category) || auction.getItem().getCategory().equalsIgnoreCase(category);
    }

    private boolean matchesStatus(Auction auction, String status) {
        return status == null || "Tất cả".equals(status) || resolveStatusLabel(auction).equals(status);
    }

    private VBox createAuctionCard(Auction auction) {
        VBox card = new VBox(12);
        card.getStyleClass().add("catalog-product-card");

        Label titleLabel = new Label(auction.getItem().getName());
        titleLabel.getStyleClass().add("catalog-product-title");
        titleLabel.setWrapText(true);
        titleLabel.setMinHeight(72);

        Label imageBox = new Label(resolveImageText(auction));
        imageBox.getStyleClass().add("catalog-product-image");
        imageBox.setMaxWidth(Double.MAX_VALUE);

        Label statusBadge = new Label(resolveStatusLabel(auction));
        statusBadge.getStyleClass().add(resolveStatusBadgeClass(auction));

        Label categoryBadge = new Label(auction.getItem().getCategory());
        categoryBadge.getStyleClass().add("badge-soft");

        HBox badgeRow = new HBox(8, statusBadge, categoryBadge);

        Label startingPriceRow = createCatalogInfoRow("Giá khởi điểm:", formatPrice(auction.getStartingPrice()));
        Label currentPriceRow = createCatalogInfoRow(auction.isFinished() ? "Giá chốt:" : "Giá hiện tại:", formatPrice(auction.getCurrentPrice()));
        Label timeRow = createCatalogInfoRow("Thời gian tổ chức:", auction.getItem().getStartTime().format(DATE_TIME_FORMATTER));
        Label sellerRow = createCatalogInfoRow("Người bán:", auction.getSeller().getUsername());

        Button detailButton = new Button(auction.isFinished() ? "Xem tổng kết" : "Xem chi tiết");
        detailButton.getStyleClass().add("catalog-card-btn");
        detailButton.setMaxWidth(Double.MAX_VALUE);
        detailButton.setOnAction(event -> openAuctionDetail(auction));

        card.getChildren().addAll(imageBox, badgeRow, titleLabel, startingPriceRow, currentPriceRow, timeRow, sellerRow, detailButton);
        return card;
    }

    private Label createCatalogInfoRow(String label, String value) {
        Label row = new Label(label + "  " + value);
        row.getStyleClass().add("catalog-info-row");
        row.setWrapText(true);
        return row;
    }

    private String resolveImageText(Auction auction) {
        return switch (auction.getItem().getCategory()) {
            case "Vehicle" -> "VEHICLE";
            case "Art" -> "ART";
            default -> "AUREX";
        };
    }

    private String shortId(String id) {
        if (id == null || id.length() <= 8) {
            return id == null ? "" : id;
        }
        return id.substring(0, 8);
    }

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

    private String resolveStatusBadgeClass(Auction auction) {
        String status = resolveStatusLabel(auction);
        return switch (status) {
            case "Đang diễn ra" -> "badge-live";
            case "Sắp diễn ra" -> "badge-new";
            default -> "badge-hot";
        };
    }

    private void openAuctionDetail(Auction auction) {
        Stage stage = (Stage) searchField.getScene().getWindow();
        // Điều hướng tới trang chi tiết tài sản (trung gian) thay vì vào thẳng phòng đấu giá
        SceneNavigator.navigateToAssetDetail(stage, auction);
    }

    private String formatPrice(BigDecimal amount) {
        return PriceFormatter.formatPrice(amount);
    }

    @FXML public void goToHome(ActionEvent event) { SceneNavigator.goToHome(event); }
    @FXML public void goToAuctionList(ActionEvent event) { SceneNavigator.goToAuctionList(event); }
    @FXML public void goToSessions(ActionEvent event) { SceneNavigator.goToSessions(event); }
    @FXML public void goToNews(ActionEvent event) { SceneNavigator.goToNews(event); }
    @FXML public void goToContact(ActionEvent event) { SceneNavigator.goToContact(event); }
    @FXML public void goToLogin(ActionEvent event) { SceneNavigator.goToLogin(event); }
    @FXML public void goToCreateAuction(ActionEvent event) { SceneNavigator.goToCreateAuction(event); }
}
