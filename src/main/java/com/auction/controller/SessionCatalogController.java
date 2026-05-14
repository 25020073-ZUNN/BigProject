package com.auction.controller;

import com.auction.model.Auction;
import com.auction.network.client.AuctionUpdateListener;
import com.auction.network.client.NetworkService;
import com.auction.util.UserSession;
import com.auction.util.SceneNavigator;
import com.auction.util.AlertHelper;
import com.auction.util.LoginStateHelper;
import com.auction.util.PriceFormatter;
import com.auction.network.client.AuctionPayloadMapper;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Controller cho màn hình "Phiên đấu giá".
 *
 * Màn hình này tập trung vào trạng thái thời gian của từng phiên:
 * - Sắp diễn ra
 * - Đang diễn ra
 * - Đã kết thúc
 *
 * Ngoài ra, phần tìm kiếm và bộ lọc đều chạy trên snapshot thật từ server.
 */
public class SessionCatalogController {

    private final NetworkService networkService = NetworkService.getInstance();
    private final AuctionUpdateListener auctionUpdateListener = auctionData -> Platform.runLater(this::renderSessions);

    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> statusFilter;
    @FXML
    private VBox runningSessionsContainer;
    @FXML
    private VBox upcomingSessionsContainer;
    @FXML
    private VBox finishedSessionsContainer;
    @FXML
    private Button loginButton;

    @FXML
    public void initialize() {
        statusFilter.setItems(FXCollections.observableArrayList("Tất cả", "Đang diễn ra", "Sắp diễn ra", "Đã kết thúc"));
        statusFilter.setValue("Tất cả");

        LoginStateHelper.updateLoginButton(loginButton);

        searchField.textProperty().addListener((observable, oldValue, newValue) -> Platform.runLater(this::renderSessions));
        statusFilter.valueProperty().addListener((observable, oldValue, newValue) -> Platform.runLater(this::renderSessions));

        registerObserverLifecycle();
        networkService.addAuctionUpdateListener(auctionUpdateListener);
    }

    @FXML
    public void handleLogout(ActionEvent event) { LoginStateHelper.handleLogout(event); }

    @FXML
    public void handleSearch(ActionEvent event) {
        renderSessions();
    }

    private void registerObserverLifecycle() {
        searchField.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (oldScene != null && newScene == null) {
                networkService.removeAuctionUpdateListener(auctionUpdateListener);
            }
        });
    }

    private void renderSessions() {
        List<Auction> filteredAuctions = filterAuctions(loadAuctionsFromServer());
        List<Auction> running = filteredAuctions.stream().filter(auction -> "Đang diễn ra".equals(resolveStatusLabel(auction))).collect(Collectors.toList());
        List<Auction> upcoming = filteredAuctions.stream().filter(auction -> "Sắp diễn ra".equals(resolveStatusLabel(auction))).collect(Collectors.toList());
        List<Auction> finished = filteredAuctions.stream().filter(auction -> "Đã kết thúc".equals(resolveStatusLabel(auction))).collect(Collectors.toList());

        renderSection(runningSessionsContainer, running, "Không có phiên đang diễn ra.");
        renderSection(upcomingSessionsContainer, upcoming, "Không có phiên sắp diễn ra.");
        renderSection(finishedSessionsContainer, finished, "Không có phiên đã kết thúc.");
    }

    private List<Auction> filterAuctions(List<Auction> auctions) {
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        String status = statusFilter.getValue();

        return auctions.stream()
                .filter(auction -> matchesKeyword(auction, keyword))
                .filter(auction -> status == null || "Tất cả".equals(status) || resolveStatusLabel(auction).equals(status))
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
        return auction.getItem().getName().toLowerCase(Locale.ROOT).contains(keyword)
                || auction.getId().toLowerCase(Locale.ROOT).contains(keyword)
                || auction.getItem().getId().toLowerCase(Locale.ROOT).contains(keyword)
                || auction.getSeller().getUsername().toLowerCase(Locale.ROOT).contains(keyword);
    }

    private void renderSection(VBox container, List<Auction> auctions, String emptyMessage) {
        container.getChildren().clear();
        if (auctions.isEmpty()) {
            container.getChildren().add(createEmptyState(emptyMessage));
            return;
        }

        for (Auction auction : auctions) {
            container.getChildren().add(createSessionRow(auction));
        }
    }

    private VBox createSessionRow(Auction auction) {
        VBox row = new VBox(10);
        row.getStyleClass().add("session-data-card");

        Label codeLabel = new Label("Mã phiên: " + auction.getId());
        codeLabel.getStyleClass().add("hero-small");

        Label titleLabel = new Label(auction.getItem().getName());
        titleLabel.getStyleClass().add("news-title");
        titleLabel.setWrapText(true);

        Label sellerLabel = new Label("Người bán: " + auction.getSeller().getUsername());
        sellerLabel.getStyleClass().add("muted-text");

        Label statusLabel = new Label(resolveStatusLabel(auction) + " | " + buildTimeMessage(auction));
        statusLabel.getStyleClass().add("partner-text-strong");
        statusLabel.setWrapText(true);

        Label priceLabel = new Label("Giá hiện tại: " + formatPrice(auction.getCurrentPrice()));
        priceLabel.getStyleClass().add("mini-info-number");

        Label stepLabel = new Label("Bước giá: " + formatPrice(auction.getMinimumBidStep()));
        stepLabel.getStyleClass().add("partner-text");

        javafx.scene.control.Button detailButton = new javafx.scene.control.Button("Mở phiên");
        detailButton.getStyleClass().add("small-btn");
        detailButton.setOnAction(event -> openAuctionDetail(auction));

        HBox footer = new HBox(12);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        footer.getChildren().addAll(priceLabel, stepLabel, spacer, detailButton);

        row.getChildren().addAll(codeLabel, titleLabel, sellerLabel, statusLabel, footer);
        return row;
    }

    private VBox createEmptyState(String message) {
        VBox box = new VBox(8);
        box.getStyleClass().add("content-card");
        Label title = new Label(message);
        title.getStyleClass().add("partner-title");
        box.getChildren().add(title);
        return box;
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

    private String buildTimeMessage(Auction auction) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = auction.getItem().getStartTime();
        LocalDateTime end = auction.getItem().getEndTime();

        /*
         * Ghi chú quan trọng:
         * Màn hình phiên đấu giá phải giải thích thời gian theo ngữ nghĩa trạng thái,
         * không chỉ hiển thị timestamp thô. Người dùng cần nhìn vào là biết còn bao lâu,
         * sắp mở khi nào, hay đã kết thúc từ lúc nào.
         */
        if (now.isBefore(start)) {
            return "Mở sau " + formatDuration(now, start);
        }
        if (now.isBefore(end) && !auction.isFinished()) {
            return "Còn lại " + formatDuration(now, end);
        }
        return "Đã kết thúc";
    }

    private String formatDuration(LocalDateTime from, LocalDateTime to) {
        long days = from.until(to, ChronoUnit.DAYS);
        from = from.plusDays(days);
        long hours = from.until(to, ChronoUnit.HOURS);
        from = from.plusHours(hours);
        long minutes = from.until(to, ChronoUnit.MINUTES);
        return days + " ngày " + String.format("%02d:%02d", hours, minutes);
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
}
