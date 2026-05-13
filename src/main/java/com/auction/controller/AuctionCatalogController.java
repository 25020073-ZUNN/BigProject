package com.auction.controller;

import com.auction.model.Auction;
import com.auction.model.item.Item;
import com.auction.network.client.AuctionPayloadMapper;
import com.auction.network.client.AuctionUpdateListener;
import com.auction.network.client.NetworkService;
import com.auction.util.UserSession;
import com.auction.util.SceneNavigator;
import com.auction.util.AlertHelper;
import com.auction.util.LoginStateHelper;
import com.auction.util.PriceFormatter;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private Label resultCountLabel;
    @FXML
    private VBox auctionListContainer;
    @FXML
    private Button loginButton;

    @FXML
    public void initialize() {
        categoryFilter.setItems(FXCollections.observableArrayList("Tất cả", "Electronics", "Vehicle", "Art"));
        categoryFilter.setValue("Tất cả");

        LoginStateHelper.updateLoginButton(loginButton);

        statusFilter.setItems(FXCollections.observableArrayList("Tất cả", "Sắp diễn ra", "Đang diễn ra", "Đã kết thúc"));
        statusFilter.setValue("Tất cả");

        searchField.textProperty().addListener((observable, oldValue, newValue) -> Platform.runLater(this::renderAuctions));
        categoryFilter.valueProperty().addListener((observable, oldValue, newValue) -> Platform.runLater(this::renderAuctions));
        statusFilter.valueProperty().addListener((observable, oldValue, newValue) -> Platform.runLater(this::renderAuctions));

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
        List<Auction> filteredAuctions = filterAuctions(loadAuctionsFromServer());

        resultCountLabel.setText(filteredAuctions.size() + " tài sản phù hợp");
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
        card.getStyleClass().add("auction-data-card");

        Label statusBadge = new Label(resolveStatusLabel(auction));
        statusBadge.getStyleClass().add(resolveStatusBadgeClass(auction));

        Label titleLabel = new Label(auction.getItem().getName());
        titleLabel.getStyleClass().add("news-title");
        titleLabel.setWrapText(true);

        Label descriptionLabel = new Label(auction.getItem().getDescription());
        descriptionLabel.getStyleClass().add("partner-text");
        descriptionLabel.setWrapText(true);

        Label categoryLabel = new Label("Danh mục: " + auction.getItem().getCategory());
        categoryLabel.getStyleClass().add("partner-text-strong");

        Label sellerLabel = new Label("Người bán: " + auction.getSeller().getUsername());
        sellerLabel.getStyleClass().add("muted-text");

        Label timeLabel = new Label(
                "Bắt đầu: " + auction.getItem().getStartTime().format(DATE_TIME_FORMATTER)
                        + " | Kết thúc: " + auction.getItem().getEndTime().format(DATE_TIME_FORMATTER)
        );
        timeLabel.getStyleClass().add("muted-text");
        timeLabel.setWrapText(true);

        Label priceLabel = new Label("Giá hiện tại: " + formatPrice(auction.getCurrentPrice()));
        priceLabel.getStyleClass().add("mini-info-number");

        Label stepLabel = new Label("Bước giá: " + formatPrice(auction.getMinimumBidStep()));
        stepLabel.getStyleClass().add("partner-text");

        Button detailButton = new Button("Xem chi tiết");
        detailButton.getStyleClass().add("small-btn");
        detailButton.setOnAction(event -> openAuctionDetail(auction));

        HBox footer = new HBox(12);
        footer.setFillHeight(true);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        footer.getChildren().addAll(priceLabel, spacer, detailButton);

        card.getChildren().addAll(statusBadge, titleLabel, categoryLabel, descriptionLabel, sellerLabel, timeLabel, stepLabel, footer);
        return card;
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
        if (!now.isAfter(auction.getItem().getStartTime()) && now.isBefore(auction.getItem().getStartTime())) {
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
        SceneNavigator.navigateToAuctionDetailOrSummary(stage, auction);
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
