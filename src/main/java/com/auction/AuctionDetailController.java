package com.auction;

import com.auction.model.Auction;
import com.auction.model.item.Item;
import com.auction.model.user.Bidder;
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

/**
 * AuctionDetailController - Bộ điều khiển cho màn hình chi tiết sản phẩm và đặt giá.
 * Hỗ trợ hiển thị thông tin sản phẩm, đếm ngược thời gian và thực hiện giao dịch đặt giá.
 */
public class AuctionDetailController {

    // Định dạng giá tiền (phân cách hàng nghìn bằng dấu chấm)
    private static final DecimalFormat PRICE_FORMAT = createPriceFormat();

    private final AuctionService auctionService = AuctionService.getInstance();
    // Người dùng mặc định (Khách) nếu chưa đăng nhập
    private final User currentUser = new Bidder("guest_user", "guest@example.com", "guest");
    private Auction currentAuction; // Phiên đấu giá hiện tại đang được hiển thị
    private Timeline countdownTimeline; // Luồng xử lý đếm ngược thời gian (JavaFX Timeline)

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

    /**
     * Nhận dữ liệu mặt hàng từ Controller danh sách và cập nhật giao diện.
     */
    public void setItemData(Item item) {
        this.currentAuction = auctionService.getAuctionByItem(item);
        if (currentAuction != null) {
            lblName.setText(item.getName());
            lblPrice.setText(formatPrice(currentAuction.getCurrentPrice()));
            lblSeller.setText(currentAuction.getSeller().getUsername());
            startCountdown(item.getEndTime()); // Bắt đầu đếm ngược thời gian kết thúc
        }
    }

    /**
     * Xử lý sự kiện khi người dùng nhấn nút "Bid" (Đặt giá).
     */
    @FXML
    public void handleBid() {
        try {
            String bidText = txtBidAmount.getText();
            if (bidText == null || bidText.isBlank()) {
                showError("Vui lòng nhập số tiền muốn đặt giá.");
                return;
            }

            BigDecimal bidAmount = parseAmount(bidText);
            
            // Lấy thông tin người dùng đang đăng nhập từ Session, nếu không có thì dùng Guest
            User bidder = UserSession.isLoggedIn() ? UserSession.getLoggedInUser() : currentUser;
            
            // Gửi yêu cầu đặt giá tới AuctionService
            boolean success = auctionService.placeBid(currentAuction, bidder, bidAmount);
            
            if (success) {
                // Nếu thành công: cập nhật lại giá hiển thị, xóa ô nhập và thông báo
                lblPrice.setText(formatPrice(currentAuction.getCurrentPrice()));
                txtBidAmount.clear();
                showSuccess("Đặt giá thành công!");
            } else {
                // Nếu thất bại: thông báo giá đặt phải cao hơn giá hiện tại
                showError("Giá đặt phải cao hơn giá hiện tại: " + formatPrice(currentAuction.getCurrentPrice()));
            }
        } catch (Exception e) {
            showError("Định dạng số tiền không hợp lệ.");
        }
    }

    /**
     * Khởi tạo và chạy bộ đếm ngược thời gian.
     */
    private void startCountdown(LocalDateTime endTime) {
        stopCountdown(); // Dừng bộ đếm cũ nếu đang chạy
        updateTimeLabel(endTime); // Cập nhật nhãn thời gian lần đầu

        // Tạo Timeline chạy lặp lại mỗi 1 giây
        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            updateTimeLabel(endTime);
            // Kiểm tra nếu đã quá thời gian kết thúc
            if (LocalDateTime.now().isAfter(endTime)) {
                stopCountdown();
                lblTime.setText("Phiên đấu giá đã kết thúc");
            }
        }));
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
    }

    /**
     * Tính toán khoảng cách thời gian từ hiện tại đến lúc kết thúc và hiển thị lên Label.
     */
    private void updateTimeLabel(LocalDateTime endTime) {
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(endTime)) {
            lblTime.setText("0 ngày 00:00:00");
            return;
        }

        // Tính toán Số ngày, Giờ, Phút, Giây còn lại
        long days = now.until(endTime, ChronoUnit.DAYS);
        now = now.plusDays(days);
        long hours = now.until(endTime, ChronoUnit.HOURS);
        now = now.plusHours(hours);
        long minutes = now.until(endTime, ChronoUnit.MINUTES);
        now = now.plusMinutes(minutes);
        long seconds = now.until(endTime, ChronoUnit.SECONDS);

        lblTime.setText(String.format("%d ngày %02d:%02d:%02d", days, hours, minutes, seconds));
    }

    /**
     * Dừng bộ đếm ngược để giải phóng tài nguyên.
     */
    private void stopCountdown() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }
    }

    /**
     * Chuyển đổi chuỗi văn bản nhập vào thành BigDecimal (loại bỏ các ký tự không phải số).
     */
    private BigDecimal parseAmount(String text) {
        return new BigDecimal(text.trim().replaceAll("[^\\d]", ""));
    }

    /**
     * Định dạng số tiền sang chuẩn VND (Ví dụ: 25.000.000 VND).
     */
    private String formatPrice(BigDecimal amount) {
        return PRICE_FORMAT.format(amount) + " VND";
    }

    /**
     * Hiển thị hộp thoại thông báo lỗi.
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Hiển thị hộp thoại thông báo thành công.
     */
    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Cấu hình định dạng số (phân cách hàng nghìn bằng dấu chấm).
     */
    private static DecimalFormat createPriceFormat() {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance();
        symbols.setGroupingSeparator('.');
        DecimalFormat format = new DecimalFormat("#,##0", symbols);
        format.setGroupingUsed(true);
        return format;
    }
}
