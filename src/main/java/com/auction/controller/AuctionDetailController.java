package com.auction.controller;

import com.auction.model.Auction;
import com.auction.model.BidTransaction;
import com.auction.model.item.Item;
import com.auction.model.user.Bidder;
import com.auction.model.user.User;
import com.auction.service.AuctionService;
import com.auction.util.UserSession;
import javafx.application.Platform;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.Node;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class AuctionDetailController {

    private static final DecimalFormat PRICE_FORMAT = createPriceFormat();
    private static final BigDecimal MIN_INCREMENT_FLOOR = new BigDecimal("500000");
    private static final ExecutorService REFRESH_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "auction-detail-refresh");
        thread.setDaemon(true);
        return thread;
    });

    private final AuctionService auctionService = AuctionService.getInstance();
    private final User currentUser = UserSession.isLoggedIn()
            ? UserSession.getLoggedInUser()
            : new Bidder("guest_user", "guest@example.com", "guest");

    private final XYChart.Series<Number, Number> priceSeries = new XYChart.Series<>();

    private Auction currentAuction;
    private Timeline countdownTimeline;
    private Timeline refreshTimeline;
    private final AtomicBoolean refreshInProgress = new AtomicBoolean(false);
    private int lastSeenBidCount = -1;
    private int lastRenderedBidCount = -1;
    private BigDecimal lastRenderedPrice;

    private boolean autoBidEnabled;
    private BigDecimal autoBidMaximum;
    private BigDecimal autoBidStep;

    @FXML
    private Label lblName;
    @FXML
    private Label lblStatus;
    @FXML
    private Label lblPrice;
    @FXML
    private Label lblLeader;
    @FXML
    private Label lblSeller;
    @FXML
    private Label lblTime;
    @FXML
    private Label lblMinimumBid;
    @FXML
    private Label lblBidHint;
    @FXML
    private Label lblAutoBidStatus;
    @FXML
    private Label lblNotification;
    @FXML
    private TextField txtBidAmount;
    @FXML
    private TextField txtAutoBidMax;
    @FXML
    private TextField txtAutoBidStep;
    @FXML
    private Button btnBid;
    @FXML
    private Button loginButton;
    @FXML
    private ListView<String> lvBidHistory;
    @FXML
    private LineChart<Number, Number> priceChart;
    @FXML
    private NumberAxis xAxis;
    @FXML
    private NumberAxis yAxis;

    @FXML
    public void initialize() {
        priceChart.getData().add(priceSeries);
        xAxis.setForceZeroInRange(false);
        yAxis.setForceZeroInRange(false);

        updateLoginState();

        if (currentAuction == null && !auctionService.getAllAuctions().isEmpty()) {
            currentAuction = auctionService.getAllAuctions().get(0);
            bindAuction(currentAuction);
        }
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
    public void goToHome(ActionEvent event) {
        switchScene(event, "giaodien.fxml");
    }

    @FXML
    public void goToLogin(ActionEvent event) {
        switchScene(event, "login.fxml");
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

    private void switchScene(ActionEvent event, String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/" + fxmlFile));
            Parent root = loader.load();
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


    public void setItemData(Item item) {
        Auction auction = auctionService.getAuctionByItem(item);
        if (auction != null) {
            bindAuction(auction);
        }
    }

    @FXML
    public void handleBid() {
        if (currentAuction == null) {
            showError("Không có phiên đấu giá để đặt giá.");
            return;
        }

        try {
            BigDecimal bidAmount = parseAmount(txtBidAmount.getText());
            BigDecimal minimumAllowed = currentAuction.getCurrentPrice().add(resolveMinimumIncrement());

            if (bidAmount.compareTo(minimumAllowed) < 0) {
                showError("Giá đặt tối thiểu là " + formatPrice(minimumAllowed) + ".");
                return;
            }

            boolean success = auctionService.placeBid(currentAuction, currentUser, bidAmount);
            if (!success) {
                showError("Đặt giá thất bại. Có thể giá đã thay đổi trước khi gửi yêu cầu.");
                return;
            }

            txtBidAmount.clear();
            publishNotification("Bạn đang dẫn đầu với mức " + formatPrice(bidAmount) + ".");
            refreshAuctionState();
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        } catch (Exception ex) {
            showError("Định dạng số tiền không hợp lệ.");
        }
    }

    @FXML
    public void handleActivateAutoBid() {
        if (currentAuction == null) {
            showError("Không có phiên đấu giá để cấu hình auto-bid.");
            return;
        }

        try {
            autoBidMaximum = parseAmount(txtAutoBidMax.getText());
            autoBidStep = parseAmount(txtAutoBidStep.getText());
            if (autoBidStep.compareTo(resolveMinimumIncrement()) < 0) {
                showError("Bước tăng auto-bid phải từ " + formatPrice(resolveMinimumIncrement()) + " trở lên.");
                return;
            }

            autoBidEnabled = true;
            lblAutoBidStatus.setText(
                    "Đang theo dõi đến " + formatPrice(autoBidMaximum) + " với bước tăng " + formatPrice(autoBidStep) + "."
            );
            publishNotification("Auto-bid đã được kích hoạt.");
            maybeExecuteAutoBid();
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        } catch (Exception ex) {
            showError("Thông tin auto-bid không hợp lệ.");
        }
    }

    @FXML
    public void handleDisableAutoBid() {
        autoBidEnabled = false;
        autoBidMaximum = null;
        autoBidStep = null;
        lblAutoBidStatus.setText("Auto-bid chưa được kích hoạt.");
        publishNotification("Auto-bid đã được tắt.");
    }

    private void bindAuction(Auction auction) {
        currentAuction = auction;
        lastSeenBidCount = -1;
        lastRenderedBidCount = -1;
        lastRenderedPrice = null;
        autoBidEnabled = false;
        autoBidMaximum = null;
        autoBidStep = null;
        lblAutoBidStatus.setText("Auto-bid chưa được kích hoạt.");

        lblName.setText(auction.getItem().getName());
        lblSeller.setText(auction.getSeller().getUsername());
        txtBidAmount.setText(formatInputSuggestion(auction.getCurrentPrice().add(resolveMinimumIncrement())));
        refreshAuctionState();
        startCountdown(auction.getItem().getEndTime());
        startRefreshLoop();
    }

    private void refreshAuctionState() {
        if (currentAuction == null) {
            return;
        }

        Auction latestAuction = auctionService.getAuctionById(currentAuction.getId());
        if (latestAuction != null) {
            currentAuction = latestAuction;
        }

        lblPrice.setText(formatPrice(currentAuction.getCurrentPrice()));
        lblLeader.setText(maskLeader(currentAuction.getHighestBidder()));
        lblStatus.setText(resolveStatusLabel());
        lblMinimumBid.setText(formatPrice(resolveMinimumIncrement()));
        lblBidHint.setText("Giá đặt tiếp theo tối thiểu: "
                + formatPrice(currentAuction.getCurrentPrice().add(resolveMinimumIncrement())) + ".");

        int currentBidCount = currentAuction.getBidHistory().size();
        BigDecimal currentPrice = currentAuction.getCurrentPrice();
        if (currentBidCount != lastRenderedBidCount || !Objects.equals(currentPrice, lastRenderedPrice)) {
            redrawHistory();
            redrawChart();
            lastRenderedBidCount = currentBidCount;
            lastRenderedPrice = currentPrice;
        }
    }

    private void redrawHistory() {
        List<BidTransaction> history = currentAuction.getBidHistory();
        List<String> rows = new ArrayList<>();

        for (int i = history.size() - 1; i >= 0; i--) {
            BidTransaction transaction = history.get(i);
            rows.add(formatPrice(transaction.getBidAmount())
                    + " - "
                    + maskUserId(transaction.getBidder().getId())
                    + " - "
                    + transaction.getBidTime().truncatedTo(ChronoUnit.SECONDS));
        }

        if (rows.isEmpty()) {
            rows.add("Chưa có lượt đặt giá nào.");
        }

        lvBidHistory.setItems(FXCollections.observableArrayList(rows));

        if (history.size() != lastSeenBidCount) {
            handleBidCountChanged(history);
            lastSeenBidCount = history.size();
        }
    }

    private void handleBidCountChanged(List<BidTransaction> history) {
        if (history.isEmpty()) {
            return;
        }

        BidTransaction latestBid = history.get(history.size() - 1);
        User latestBidder = latestBid.getBidder();

        if (currentUser.getId().equals(latestBidder.getId())) {
            return;
        }

        publishNotification("Có bid mới từ " + maskUserId(latestBidder.getId())
                + ": " + formatPrice(latestBid.getBidAmount()) + ".");
        maybeExecuteAutoBid();
    }

    private void redrawChart() {
        priceSeries.getData().clear();

        List<BidTransaction> history = currentAuction.getBidHistory();
        if (history.isEmpty()) {
            priceSeries.getData().add(new XYChart.Data<>(0, currentAuction.getCurrentPrice().doubleValue()));
            return;
        }

        LocalDateTime firstBidTime = history.get(0).getBidTime();
        for (BidTransaction transaction : history) {
            long seconds = ChronoUnit.SECONDS.between(firstBidTime, transaction.getBidTime());
            priceSeries.getData().add(new XYChart.Data<>(seconds, transaction.getBidAmount().doubleValue()));
        }
    }

    private void maybeExecuteAutoBid() {
        if (!autoBidEnabled || currentAuction == null || autoBidMaximum == null || autoBidStep == null) {
            return;
        }

        User leader = currentAuction.getHighestBidder();
        if (leader != null && currentUser.getId().equals(leader.getId())) {
            return;
        }

        BigDecimal nextBid = currentAuction.getCurrentPrice().add(autoBidStep.max(resolveMinimumIncrement()));
        if (nextBid.compareTo(autoBidMaximum) > 0) {
            autoBidEnabled = false;
            lblAutoBidStatus.setText("Auto-bid đã dừng vì giá tiếp theo vượt quá mức tối đa.");
            publishNotification("Auto-bid dừng do chạm trần cấu hình.");
            return;
        }

        boolean success = auctionService.placeBid(currentAuction, currentUser, nextBid);
        if (success) {
            lblAutoBidStatus.setText("Auto-bid vừa nâng lên " + formatPrice(nextBid) + ".");
            publishNotification("Auto-bid đã phản ứng với mức " + formatPrice(nextBid) + ".");
            refreshAuctionState();
        }
    }

    private void startCountdown(LocalDateTime endTime) {
        stopCountdown();
        updateTimeLabel(endTime);

        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            updateTimeLabel(endTime);
            if (LocalDateTime.now().isAfter(endTime) && currentAuction != null && !currentAuction.isFinished()) {
                currentAuction.closeAuction();
                refreshAuctionState();
                stopCountdown();
                stopRefreshLoop();
                publishNotification("Phiên đấu giá đã kết thúc.");
            }
        }));
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
    }

    private void startRefreshLoop() {
        stopRefreshLoop();
        refreshTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            refreshAuctionStateAsync();
        }));
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();
    }

    private void refreshAuctionStateAsync() {
        if (!refreshInProgress.compareAndSet(false, true)) {
            return;
        }

        REFRESH_EXECUTOR.execute(() -> {
            try {
                auctionService.refreshAuctions();
                Platform.runLater(this::refreshAuctionState);
            } finally {
                refreshInProgress.set(false);
            }
        });
    }

    private void stopCountdown() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }
    }

    private void stopRefreshLoop() {
        if (refreshTimeline != null) {
            refreshTimeline.stop();
        }
    }

    private void updateTimeLabel(LocalDateTime endTime) {
        LocalDateTime now = LocalDateTime.now();
        if (!now.isBefore(endTime)) {
            lblTime.setText("0 ngày 00:00:00");
            return;
        }

        /*
         * Ghi chú rất quan trọng:
         * Countdown cũ chỉ hiển thị tổng số giờ, nên với phiên kéo dài nhiều ngày hoặc nhiều tháng
         * giao diện sẽ hiện dạng 2160:15:10, rất khó đọc.
         * Phiên bản mới tách rõ phần ngày và phần giờ/phút/giây để phù hợp với các phiên mẫu dài 3 tháng.
         */
        long days = now.until(endTime, ChronoUnit.DAYS);
        now = now.plusDays(days);
        long hours = now.until(endTime, ChronoUnit.HOURS);
        now = now.plusHours(hours);
        long minutes = now.until(endTime, ChronoUnit.MINUTES);
        now = now.plusMinutes(minutes);
        long seconds = now.until(endTime, ChronoUnit.SECONDS);
        lblTime.setText(String.format("%d ngày %02d:%02d:%02d", days, hours, minutes, seconds));
    }

    private BigDecimal parseAmount(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập số tiền hợp lệ.");
        }
        String normalized = text.trim().replaceAll("[^\\d]", "");
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập số tiền hợp lệ.");
        }
        return new BigDecimal(normalized);
    }

    private BigDecimal resolveMinimumIncrement() {
        if (currentAuction == null) {
            return MIN_INCREMENT_FLOOR;
        }
        if (currentAuction.getMinimumBidStep() != null && currentAuction.getMinimumBidStep().compareTo(BigDecimal.ZERO) > 0) {
            return currentAuction.getMinimumBidStep();
        }
        BigDecimal onePercent = currentAuction.getCurrentPrice()
                .multiply(new BigDecimal("0.01"))
                .setScale(0, RoundingMode.CEILING);
        return onePercent.max(MIN_INCREMENT_FLOOR);
    }

    private String resolveStatusLabel() {
        if (currentAuction.isFinished()) {
            return "FINISHED";
        }
        return currentAuction.isActive() ? "RUNNING" : "PAUSED";
    }

    private String formatInputSuggestion(BigDecimal amount) {
        return PRICE_FORMAT.format(amount);
    }

    private String formatPrice(BigDecimal amount) {
        return PRICE_FORMAT.format(amount) + " VND";
    }

    private String maskLeader(User user) {
        return user == null ? "Chưa có" : maskUserId(user.getId());
    }

    private String maskUserId(String userId) {
        if (userId == null || userId.length() <= 8) {
            return userId == null ? "Ẩn danh" : userId;
        }
        return userId.substring(0, 8) + "...";
    }

    private void publishNotification(String message) {
        lblNotification.setText(message);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static DecimalFormat createPriceFormat() {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance();
        symbols.setGroupingSeparator(',');
        DecimalFormat format = new DecimalFormat("#,##0", symbols);
        format.setGroupingUsed(true);
        return format;
    }
}
