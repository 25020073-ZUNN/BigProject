package com.uet.bidding;

import com.uet.bidding.model.user.Bidder;
import com.uet.service.AuctionService;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AuctionDetailController {

    private static final DecimalFormat PRICE_FORMAT = createPriceFormat();
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d+).*?(\\d{1,2}):(\\d{2}):(\\d{2})");

    private AuctionService auctionService = new AuctionService();
    private final Bidder currentUser = new Bidder("guest_bidder", "guest@example.com", "guest");
    private Timeline countdownTimeline;
    private long remainingSeconds;

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

    public void setProductData(AuctionListController.Product product) {
        lblName.setText(product.getName());
        lblPrice.setText(product.getPrice());
        lblSeller.setText(product.getSeller());
        initializeCurrentPrice(product.getPrice());
        startCountdown(product.getTime());
    }

    @FXML
    public void handleBid() {
        try {
            String bidText = txtBidAmount.getText();

            if (bidText == null || bidText.isBlank()) {
                showError("Please enter a bid amount.");
                return;
            }

            BigDecimal bidAmount;
            try {
                bidAmount = parseAmount(bidText);
            } catch (NumberFormatException exception) {
                showError("Bid amount must be a valid number.");
                return;
            }

            boolean success = auctionService.placeBid(currentUser, bidAmount);
            if (!success) {
                showError("Bid amount must be greater than the current price.");
                return;
            }

            lblPrice.setText(formatPrice(auctionService.getCurrentPrice()));
            txtBidAmount.clear();
            showSuccess("Bid placed successfully.");
        } catch (Exception exception) {
            showError("Unable to place bid. Please try again.");
        }
    }

    private void initializeCurrentPrice(String priceText) {
        BigDecimal currentPrice = parseAmount(priceText);
        auctionService = new AuctionService(currentPrice);
    }

    private void startCountdown(String timeText) {
        stopCountdown();
        remainingSeconds = parseRemainingSeconds(timeText);
        updateTimeLabel();

        if (remainingSeconds <= 0) {
            return;
        }

        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            if (remainingSeconds > 0) {
                remainingSeconds--;
                updateTimeLabel();
            }

            if (remainingSeconds <= 0) {
                stopCountdown();
            }
        }));
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
    }

    private void stopCountdown() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
            countdownTimeline = null;
        }
    }

    private long parseRemainingSeconds(String timeText) {
        if (timeText == null || timeText.isBlank()) {
            return 0;
        }

        Matcher matcher = TIME_PATTERN.matcher(timeText.trim());
        if (!matcher.find()) {
            return 0;
        }

        long days = Long.parseLong(matcher.group(1));
        long hours = Long.parseLong(matcher.group(2));
        long minutes = Long.parseLong(matcher.group(3));
        long seconds = Long.parseLong(matcher.group(4));

        return days * 24 * 60 * 60 + hours * 60 * 60 + minutes * 60 + seconds;
    }

    private void updateTimeLabel() {
        long days = remainingSeconds / 86_400;
        long hours = (remainingSeconds % 86_400) / 3_600;
        long minutes = (remainingSeconds % 3_600) / 60;
        long seconds = remainingSeconds % 60;

        lblTime.setText(String.format("%d days %02d:%02d:%02d", days, hours, minutes, seconds));
    }

    private BigDecimal parseAmount(String amountText) {
        String normalizedAmount = amountText.trim().replace(".", "").replace(",", "");
        if (normalizedAmount.isBlank() || !normalizedAmount.matches("\\d+")) {
            throw new NumberFormatException("Amount is empty");
        }
        return new BigDecimal(normalizedAmount);
    }

    private String formatPrice(BigDecimal amount) {
        return PRICE_FORMAT.format(amount) + " VND";
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText("Bid failed");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText("Bid successful");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static DecimalFormat createPriceFormat() {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance();
        symbols.setGroupingSeparator('.');

        DecimalFormat format = new DecimalFormat("#,##0", symbols);
        format.setGroupingUsed(true);
        format.setGroupingSize(3);
        return format;
    }
}
