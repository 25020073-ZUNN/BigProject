package com.auction.controller;

import com.auction.model.Auction;
import com.auction.model.item.Item;
import com.auction.util.LoginStateHelper;
import com.auction.util.PriceFormatter;
import com.auction.util.SceneNavigator;
import com.auction.util.UserSession;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Controller cho trang "Chi tiết tài sản đấu giá".
 *
 * Hiển thị thông tin tổng quan của tài sản, countdown thời gian,
 * giá hiện tại, và nút hành động (tham gia đấu giá / xem tổng kết).
 *
 * Luồng điều hướng:
 * - AuctionCatalog → AssetDetail (trang này) → ProductDetail (nếu running) hoặc AuctionSummary (nếu finished)
 */
public class AssetDetailController {

    private static final DateTimeFormatter DT_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    @FXML private Label breadcrumbName;
    @FXML private Label lblStatusBadge;
    @FXML private Label lblDescription;
    @FXML private Label lblItemId;
    @FXML private Label lblCategory;
    @FXML private Label lblSeller;
    @FXML private Label lblAuctionId;
    @FXML private Label lblBidStep;
    @FXML private Label lblBidCount;

    // Countdown
    @FXML private Label lblCountdownTitle;
    @FXML private Label lblDays;
    @FXML private Label lblHours;
    @FXML private Label lblMinutes;
    @FXML private Label lblSeconds;

    // Time info
    @FXML private Label lblStartTime;
    @FXML private Label lblEndTime;

    // Price info
    @FXML private Label lblStartPrice;
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblLeader;

    // Action buttons
    @FXML private Button btnJoinAuction;
    @FXML private Button btnViewSummary;
    @FXML private Button loginButton;

    /** Dữ liệu phiên đấu giá được truyền từ AuctionCatalogController */
    private Auction auction;

    /** Timeline cho countdown timer, chạy mỗi giây */
    private Timeline countdownTimeline;

    @FXML
    public void initialize() {
        LoginStateHelper.updateLoginButton(loginButton);
    }

    /**
     * Nhận dữ liệu Auction từ trang danh sách.
     * Gọi bởi SceneNavigator.navigateToAssetDetail().
     */
    public void setAuctionData(Auction auction) {
        this.auction = auction;
        if (auction == null) return;

        Item item = auction.getItem();

        // ── Breadcrumb ──
        breadcrumbName.setText(item.getName());

        // ── Status badge ──
        String status = resolveStatus();
        lblStatusBadge.setText(resolveStatusEmoji(status) + "  " + status);
        lblStatusBadge.getStyleClass().clear();
        lblStatusBadge.getStyleClass().add(resolveStatusBadgeClass(status));

        // ── Thông tin tài sản ──
        lblDescription.setText(item.getDescription() != null && !item.getDescription().isBlank()
                ? item.getDescription()
                : "Chưa có mô tả cho tài sản này.");
        lblItemId.setText(item.getId());
        lblCategory.setText(item.getCategory());
        lblSeller.setText(auction.getSeller().getUsername());
        lblAuctionId.setText(auction.getId());
        lblBidStep.setText(PriceFormatter.formatPrice(auction.getMinimumBidStep()));
        lblBidCount.setText(String.valueOf(auction.getBidHistory().size()));

        // ── Thời gian ──
        lblStartTime.setText(item.getStartTime().format(DT_FORMAT));
        lblEndTime.setText(item.getEndTime().format(DT_FORMAT));

        // ── Giá ──
        lblStartPrice.setText(PriceFormatter.formatPrice(auction.getStartingPrice()));
        lblCurrentPrice.setText(PriceFormatter.formatPrice(auction.getCurrentPrice()));
        lblLeader.setText(auction.getHighestBidder() != null
                ? auction.getHighestBidder().getUsername()
                : "Chưa có");

        // ── Cấu hình nút hành động dựa trên trạng thái ──
        configureActionButtons(status);

        // ── Bắt đầu countdown ──
        startCountdown();
    }

    /**
     * Cấu hình nút hành động:
     * - "Sắp diễn ra": Nút tham gia disabled, ẩn nút tổng kết
     * - "Đang diễn ra": Nút tham gia active, ẩn nút tổng kết
     * - "Đã kết thúc": Ẩn nút tham gia, hiện nút tổng kết
     */
    private void configureActionButtons(String status) {
        switch (status) {
            case "Sắp diễn ra" -> {
                btnJoinAuction.setText("⏳  Phiên chưa bắt đầu");
                btnJoinAuction.setDisable(true);
                btnViewSummary.setVisible(false);
                btnViewSummary.setManaged(false);
            }
            case "Đang diễn ra" -> {
                btnJoinAuction.setText("🏛  Tham gia đấu giá ngay");
                btnJoinAuction.setDisable(false);
                btnViewSummary.setVisible(false);
                btnViewSummary.setManaged(false);
            }
            case "Đã kết thúc" -> {
                btnJoinAuction.setVisible(false);
                btnJoinAuction.setManaged(false);
                btnViewSummary.setVisible(true);
                btnViewSummary.setManaged(true);
            }
        }
    }

    /**
     * Countdown timer chạy mỗi giây.
     *
     * Logic:
     * - Nếu phiên chưa bắt đầu → đếm ngược tới startTime, title = "Thời gian bắt đầu đấu giá"
     * - Nếu phiên đang chạy → đếm ngược tới endTime, title = "Thời gian còn lại"
     * - Nếu đã kết thúc → hiển thị 0:0:0:0
     */
    private void startCountdown() {
        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime target;
            String status = resolveStatus();

            if ("Sắp diễn ra".equals(status)) {
                target = auction.getItem().getStartTime();
                lblCountdownTitle.setText("⏰  Thời gian bắt đầu đấu giá");
            } else if ("Đang diễn ra".equals(status)) {
                target = auction.getItem().getEndTime();
                lblCountdownTitle.setText("⏳  Thời gian còn lại");
            } else {
                lblDays.setText("0");
                lblHours.setText("0");
                lblMinutes.setText("0");
                lblSeconds.setText("0");
                lblCountdownTitle.setText("🏁  Phiên đấu giá đã kết thúc");
                countdownTimeline.stop();
                return;
            }

            long totalSecs = ChronoUnit.SECONDS.between(now, target);
            if (totalSecs <= 0) {
                totalSecs = 0;
                // Trạng thái thay đổi → cập nhật lại nút
                configureActionButtons(resolveStatus());
            }

            long days = totalSecs / 86400;
            long hours = (totalSecs % 86400) / 3600;
            long minutes = (totalSecs % 3600) / 60;
            long secs = totalSecs % 60;

            lblDays.setText(String.valueOf(days));
            lblHours.setText(String.valueOf(hours));
            lblMinutes.setText(String.valueOf(minutes));
            lblSeconds.setText(String.valueOf(secs));
        }));
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();

        // Dừng timer khi rời trang (tránh leak)
        breadcrumbName.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (oldScene != null && newScene == null && countdownTimeline != null) {
                countdownTimeline.stop();
            }
        });
    }

    // ── Resolve trạng thái ──

    private String resolveStatus() {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(auction.getItem().getStartTime())) return "Sắp diễn ra";
        if (now.isBefore(auction.getItem().getEndTime()) && !auction.isFinished()) return "Đang diễn ra";
        return "Đã kết thúc";
    }

    private String resolveStatusEmoji(String status) {
        return switch (status) {
            case "Sắp diễn ra" -> "⏳";
            case "Đang diễn ra" -> "🟢";
            default -> "🏁";
        };
    }

    private String resolveStatusBadgeClass(String status) {
        return switch (status) {
            case "Đang diễn ra" -> "badge-live";
            case "Sắp diễn ra" -> "badge-new";
            default -> "badge-hot";
        };
    }

    // ── Action Handlers ──

    /**
     * Chuyển sang màn hình đấu giá realtime (product-detail.fxml).
     */
    @FXML
    public void handleJoinAuction(ActionEvent event) {
        if (auction == null) return;
        if (!UserSession.isLoggedIn()) {
            SceneNavigator.goToLogin(event);
            return;
        }
        Stage stage = (Stage) btnJoinAuction.getScene().getWindow();
        SceneNavigator.navigateToAuctionDetailOrSummary(stage, auction);
    }

    /**
     * Chuyển sang màn hình tổng kết (auction-summary.fxml).
     */
    @FXML
    public void handleViewSummary(ActionEvent event) {
        if (auction == null) return;
        Stage stage = (Stage) btnViewSummary.getScene().getWindow();
        SceneNavigator.navigateToAuctionDetailOrSummary(stage, auction);
    }

    // ── Navigation (giống các Controller khác) ──

    @FXML public void goToHome(ActionEvent event) { SceneNavigator.goToHome(event); }
    @FXML public void goToAuctionList(ActionEvent event) { SceneNavigator.goToAuctionList(event); }
    @FXML public void goToSessions(ActionEvent event) { SceneNavigator.goToSessions(event); }
    @FXML public void goToNews(ActionEvent event) { SceneNavigator.goToNews(event); }
    @FXML public void goToContact(ActionEvent event) { SceneNavigator.goToContact(event); }
    @FXML public void goToLogin(ActionEvent event) { SceneNavigator.goToLogin(event); }
    @FXML public void handleLogout(ActionEvent event) { LoginStateHelper.handleLogout(event); }
}
