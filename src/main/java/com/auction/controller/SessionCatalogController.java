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
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
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

public class SessionCatalogController {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final NetworkService networkService = NetworkService.getInstance();
    private final AuctionUpdateListener auctionUpdateListener = auctionData -> Platform.runLater(this::renderSessions);

    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> statusFilter;
    @FXML
    private FlowPane runningSessionsContainer;
    @FXML
    private FlowPane upcomingSessionsContainer;
    @FXML
    private FlowPane finishedSessionsContainer;
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
    public void handleLogout(ActionEvent event) {
        LoginStateHelper.handleLogout(event);
    }

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
        List<Auction> running = filteredAuctions.stream()
                .filter(auction -> "Đang diễn ra".equals(resolveStatusLabel(auction)))
                .sorted(Comparator.comparing(auction -> auction.getItem().getEndTime()))
                .collect(Collectors.toList());
        List<Auction> upcoming = filteredAuctions.stream()
                .filter(auction -> "Sắp diễn ra".equals(resolveStatusLabel(auction)))
                .sorted(Comparator.comparing(auction -> auction.getItem().getStartTime()))
                .collect(Collectors.toList());
        List<Auction> finished = filteredAuctions.stream()
                .filter(auction -> "Đã kết thúc".equals(resolveStatusLabel(auction)))
                .sorted(Comparator.comparing((Auction auction) -> auction.getItem().getEndTime()).reversed())
                .collect(Collectors.toList());

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

    private VBox createSessionCard(Auction auction) {
        VBox card = new VBox(12);
        card.getStyleClass().add("catalog-product-card");

        Label imageBox = new Label(resolveImageText(auction));
        imageBox.getStyleClass().add("catalog-product-image");
        imageBox.setMaxWidth(Double.MAX_VALUE);

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

    private String resolveStatusBadgeClass(Auction auction) {
        String status = resolveStatusLabel(auction);
        return switch (status) {
            case "Đang diễn ra" -> "badge-live";
            case "Sắp diễn ra" -> "badge-new";
            default -> "badge-hot";
        };
    }

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
