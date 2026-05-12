package com.auction.controller;

import com.auction.model.Auction;
import com.auction.model.user.User;
import com.auction.service.AuctionService;
import com.auction.util.UserSession;
import javafx.application.Platform;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Controller cho màn hình "Phiên đấu giá".
 *
 * Màn hình này tập trung vào trạng thái thời gian của từng phiên:
 * - Sắp diễn ra
 * - Đang diễn ra
 * - Đã kết thúc
 *
 * Ngoài ra, phần tìm kiếm và bộ lọc đều chạy trên dữ liệu thật từ DB.
 */
public class SessionCatalogController {

    private static final DecimalFormat PRICE_FORMAT = createPriceFormat();
    private static final ExecutorService REFRESH_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "session-catalog-refresh");
        thread.setDaemon(true);
        return thread;
    });

    private final AuctionService auctionService = AuctionService.getInstance();
    private final AtomicBoolean refreshInProgress = new AtomicBoolean(false);
    private Timeline refreshTimeline;

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

        updateLoginState();

        searchField.textProperty().addListener((observable, oldValue, newValue) -> renderSessions());
        statusFilter.valueProperty().addListener((observable, oldValue, newValue) -> renderSessions());

        startRefreshLoop();
        refreshSessionsAsync();
    }

    private void updateLoginState() {
        if (UserSession.isLoggedIn()) {
            User user = UserSession.getLoggedInUser();
            if (loginButton != null) {
                loginButton.setText("Đăng xuất (" + user.getUsername() + ")");
                loginButton.setOnAction(this::handleLogout);
            }
        } else {
            if (loginButton != null) {
                loginButton.setText("Đăng nhập");
                loginButton.setOnAction(this::goToLogin);
            }
        }
    }

    @FXML
    public void handleLogout(ActionEvent event) {
        UserSession.logout();
        updateLoginState();
        try {
            goToHome(event);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleSearch(ActionEvent event) {
        refreshSessionsAsync();
    }

    private void startRefreshLoop() {
        refreshTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> refreshSessionsAsync()));
        refreshTimeline.setCycleCount(Animation.INDEFINITE);
        refreshTimeline.play();
    }

    private void renderSessions() {
        List<Auction> filteredAuctions = filterAuctions(auctionService.getAllAuctions());
        List<Auction> running = filteredAuctions.stream().filter(auction -> "Đang diễn ra".equals(resolveStatusLabel(auction))).collect(Collectors.toList());
        List<Auction> upcoming = filteredAuctions.stream().filter(auction -> "Sắp diễn ra".equals(resolveStatusLabel(auction))).collect(Collectors.toList());
        List<Auction> finished = filteredAuctions.stream().filter(auction -> "Đã kết thúc".equals(resolveStatusLabel(auction))).collect(Collectors.toList());

        renderSection(runningSessionsContainer, running, "Không có phiên đang diễn ra.");
        renderSection(upcomingSessionsContainer, upcoming, "Không có phiên sắp diễn ra.");
        renderSection(finishedSessionsContainer, finished, "Không có phiên đã kết thúc.");
    }

    private void refreshSessionsAsync() {
        if (!refreshInProgress.compareAndSet(false, true)) {
            return;
        }

        REFRESH_EXECUTOR.execute(() -> {
            try {
                auctionService.refreshAuctions();
                Platform.runLater(this::renderSessions);
            } finally {
                refreshInProgress.set(false);
            }
        });
    }

    private List<Auction> filterAuctions(List<Auction> auctions) {
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        String status = statusFilter.getValue();

        return auctions.stream()
                .filter(auction -> matchesKeyword(auction, keyword))
                .filter(auction -> status == null || "Tất cả".equals(status) || resolveStatusLabel(auction).equals(status))
                .collect(Collectors.toList());
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
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/product-detail.fxml"));
            Parent root = loader.load();

            AuctionDetailController controller = loader.getController();
            controller.setItemData(auction.getItem());

            Stage stage = (Stage) searchField.getScene().getWindow();
            Scene scene = stage.getScene();
            if (scene == null) {
                stage.setScene(new Scene(root, 1380, 920));
            } else {
                scene.setRoot(root);
            }
        } catch (IOException e) {
            showError("Không thể mở chi tiết phiên đấu giá.");
        }
    }

    private String formatPrice(BigDecimal amount) {
        if (amount == null) {
            return "0 VND";
        }
        return PRICE_FORMAT.format(amount) + " VND";
    }

    private static DecimalFormat createPriceFormat() {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance();
        symbols.setGroupingSeparator(',');
        DecimalFormat format = new DecimalFormat("#,##0", symbols);
        format.setGroupingUsed(true);
        return format;
    }

    @FXML
    public void goToHome(ActionEvent event) {
        switchScene(event, "giaodien.fxml");
    }

    @FXML
    public void goToAuctionList(ActionEvent event) {
        switchScene(event, "auction-detail.fxml");
    }

    @FXML
    public void goToSessions(ActionEvent event) {
        switchScene(event, "sessions.fxml");
    }

    @FXML
    public void goToNews(ActionEvent event) {
        switchScene(event, "news.fxml");
    }

    @FXML
    public void goToContact(ActionEvent event) {
        switchScene(event, "contact.fxml");
    }

    @FXML
    public void goToLogin(ActionEvent event) {
        switchScene(event, "login.fxml");
    }

    private void switchScene(ActionEvent event, String fxmlFile) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/" + fxmlFile));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene currentScene = stage.getScene();
            if (currentScene == null) {
                stage.setScene(new Scene(root, 1380, 920));
            } else {
                currentScene.setRoot(root);
            }
        } catch (IOException e) {
            showError("Không thể tải giao diện: " + fxmlFile);
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
