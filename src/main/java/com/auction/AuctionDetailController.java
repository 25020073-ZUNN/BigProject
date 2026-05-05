package com.auction;

import com.auction.model.Auction;
import com.auction.model.item.Item;
import com.auction.model.user.User;
import com.auction.service.AuctionService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class AuctionDetailController {

    private static final DecimalFormat PRICE_FORMAT = createPriceFormat();

    private final AuctionService auctionService = AuctionService.getInstance();
    private final User currentUser = new User("guest_user", "guest@example.com", "guest");
    private Auction currentAuction;
    private Timeline countdownTimeline;

    @FXML
    private ImageView imgProduct;
    @FXML
    private Label lblName;
    @FXML
    private Label lblPrice;
    @FXML
    private Label lblSeller;
    @FXML
    private Label lblTime;
    @FXML
    private TextField txtBidAmount;
    @FXML
    private Button btnBid;

    public void setItemData(Item item) {
        this.currentAuction = auctionService.getAuctionByItem(item);
        if (currentAuction != null) {
            lblName.setText(item.getName());
            lblPrice.setText(formatPrice(currentAuction.getCurrentPrice()));
            lblSeller.setText(currentAuction.getSeller().getUsername());
            startCountdown(item.getEndTime());
        }
    }

    @FXML
    public void handleBid() {
        try {
            String bidText = txtBidAmount.getText();
            if (bidText == null || bidText.isBlank()) {
                showError("Please enter a bid amount.");
                return;
            }

            BigDecimal bidAmount = parseAmount(bidText);
            
            // Sử dụng user đã đăng nhập nếu có
            User bidder = UserSession.isLoggedIn() ? UserSession.getLoggedInUser() : currentUser;
            boolean success = auctionService.placeBid(currentAuction, bidder, bidAmount);
            
            if (success) {
                lblPrice.setText(formatPrice(currentAuction.getCurrentPrice()));
                txtBidAmount.clear();
                showSuccess("Bid placed successfully.");
            } else {
                showError("Bid must be higher than current price: " + formatPrice(currentAuction.getCurrentPrice()));
            }
        } catch (Exception e) {
            showError("Invalid bid amount format.");
        }
    }

    private void startCountdown(LocalDateTime endTime) {
        stopCountdown();
        updateTimeLabel(endTime);

        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            updateTimeLabel(endTime);
            if (LocalDateTime.now().isAfter(endTime)) {
                stopCountdown();
                lblTime.setText("Auction Finished");
            }
        }));
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
    }

    private void updateTimeLabel(LocalDateTime endTime) {
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(endTime)) {
            lblTime.setText("0 days 00:00:00");
            return;
        }

        long days = now.until(endTime, ChronoUnit.DAYS);
        now = now.plusDays(days);
        long hours = now.until(endTime, ChronoUnit.HOURS);
        now = now.plusHours(hours);
        long minutes = now.until(endTime, ChronoUnit.MINUTES);
        now = now.plusMinutes(minutes);
        long seconds = now.until(endTime, ChronoUnit.SECONDS);

        lblTime.setText(String.format("%d days %02d:%02d:%02d", days, hours, minutes, seconds));
    }

    private void stopCountdown() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }
    }

    private BigDecimal parseAmount(String text) {
        return new BigDecimal(text.trim().replaceAll("[^\\d]", ""));
    }

    private String formatPrice(BigDecimal amount) {
        return PRICE_FORMAT.format(amount) + " VND";
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static DecimalFormat createPriceFormat() {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance();
        symbols.setGroupingSeparator('.');
        DecimalFormat format = new DecimalFormat("#,##0", symbols);
        format.setGroupingUsed(true);
        return format;
    }
}
