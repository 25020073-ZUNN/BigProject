package com.auction.controller;

import com.auction.model.Auction;
import com.auction.model.BidTransaction;
import com.auction.model.item.Item;
import com.auction.model.user.Bidder;
import com.auction.model.user.User;
import com.auction.network.client.AuctionPayloadMapper;
import com.auction.network.client.AuctionUpdateListener;
import com.auction.network.client.NetworkService;
import com.auction.service.AutoBidStrategy;
import com.auction.util.FxAsync;
import com.auction.util.UserSession;
import com.auction.util.AlertHelper;
import com.auction.util.PriceFormatter;
import com.auction.util.SceneNavigator;
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
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Controller điều khiển giao diện chi tiết phiên đấu giá (Auction Detail).
 * Quản lý việc hiển thị thông tin sản phẩm, thời gian còn lại, lịch sử đặt giá, 
 * đồ thị giá và các chức năng đặt giá (thủ công và tự động).
 */
public class AuctionDetailController {

    // Bước giá tối thiểu mặc định nếu không được chỉ định trong phiên đấu giá
    private static final BigDecimal MIN_INCREMENT_FLOOR = new BigDecimal("500000");

    // Các dịch vụ và tiện ích hỗ trợ
    private final NetworkService networkService = NetworkService.getInstance();
    private final AutoBidStrategy autoBidStrategy = new AutoBidStrategy();
    
    // Người dùng hiện tại từ session, nếu chưa đăng nhập thì dùng guest
    private final User currentUser = UserSession.isLoggedIn()
            ? UserSession.getLoggedInUser()
            : new Bidder("guest_user", "guest@example.com", "guest");
            
    // Listener nhận cập nhật dữ liệu từ server và cập nhật giao diện trên UI thread
    private final AuctionUpdateListener auctionUpdateListener = auctionData -> Platform.runLater(this::handleAuctionsUpdated);

    // Chuỗi dữ liệu cho biểu đồ đường (LineChart) hiển thị biến động giá
    private final XYChart.Series<Number, Number> priceSeries = new XYChart.Series<>();

    // Các biến trạng thái của phiên đấu giá hiện tại
    private Auction currentAuction;
    private Timeline countdownTimeline;
    private int lastSeenBidCount = -1;
    private int lastRenderedBidCount = -1;
    private BigDecimal lastRenderedPrice;
    private LocalDateTime lastRenderedEndTime;
    private String currentAuctionId;

    // Trạng thái cấu hình đặt giá tự động (Auto-bid)
    private boolean autoBidEnabled;
    private boolean autoBidInFlight; // Đang trong quá trình gửi yêu cầu đặt giá tự động
    private BigDecimal autoBidMaximum;
    private BigDecimal autoBidStep;
    private boolean navigatingToSummary;

    // Các thành phần giao diện FXML
    @FXML private Label lblName;            // Tên sản phẩm
    @FXML private Label lblStatus;          // Trạng thái phiên (Đang chạy, kết thúc...)
    @FXML private Label lblPrice;           // Giá hiện tại
    @FXML private Label lblLeader;          // Người đang dẫn đầu (đã che ID)
    @FXML private Label lblSeller;          // Tên người bán
    @FXML private Label lblTime;            // Đồng hồ đếm ngược
    @FXML private Label lblMinimumBid;      // Bước giá tối thiểu
    @FXML private Label lblBidHint;         // Gợi ý mức giá tiếp theo
    @FXML private Label lblAutoBidStatus;   // Thông báo trạng thái auto-bid
    @FXML private Label lblNotification;    // Thông báo nhanh cho người dùng
    @FXML private TextField txtBidAmount;   // Ô nhập giá thủ công
    @FXML private TextField txtAutoBidMax;  // Mức tối đa auto-bid
    @FXML private TextField txtAutoBidStep; // Bước tăng auto-bid
    @FXML private Button btnBid;            // Nút đặt giá
    @FXML private Button loginButton;       // Nút đăng nhập/đăng xuất
    @FXML private ListView<String> lvBidHistory; // Danh sách lịch sử đặt giá
    @FXML private LineChart<Number, Number> priceChart; // Biểu đồ giá
    @FXML private NumberAxis xAxis;
    @FXML private NumberAxis yAxis;

    /**
     * Khởi tạo giao diện khi controller được load.
     */
    @FXML
    public void initialize() {
        // Thiết lập cấu hình cho biểu đồ
        priceChart.getData().add(priceSeries);
        xAxis.setForceZeroInRange(false);
        yAxis.setForceZeroInRange(false);

        // Cập nhật trạng thái đăng nhập và đăng ký nhận dữ liệu từ server
        updateLoginState();
        registerObserverLifecycle();
        networkService.addAuctionUpdateListener(auctionUpdateListener);

        // Nếu chưa có dữ liệu, thử tải từ server
        if (currentAuction == null) {
            List<Auction> auctions = loadAuctionsFromServer();
            if (!auctions.isEmpty()) {
                bindAuction(auctions.get(0));
            }
        }
    }

    /**
     * Cập nhật văn bản và hành động cho nút login dựa trên session.
     */
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

    /**
     * Xử lý khi nhấn Đăng xuất.
     */
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

    // --- Các phương thức chuyển hướng giao diện ---

    @FXML public void goToHome(ActionEvent event) { switchScene(event, "giaodien.fxml"); }
    @FXML public void goToLogin(ActionEvent event) { switchScene(event, "login.fxml"); }
    @FXML public void goToAuctionList(ActionEvent event) { switchScene(event, "auction-detail.fxml"); }
    @FXML public void goToSessions(ActionEvent event) { switchScene(event, "sessions.fxml"); }
    @FXML public void goToNews(ActionEvent event) { switchScene(event, "news.fxml"); }
    @FXML public void goToContact(ActionEvent event) { switchScene(event, "contact.fxml"); }

    /**
     * Chuyển đổi giữa các file FXML.
     */
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


    /**
     * Truyền thẳng Auction đã có sẵn vào controller.
     * Dùng bởi SceneNavigator để tránh gọi lại server.
     */
    public void setAuctionData(Auction auction) {
        if (auction != null) {
            bindAuction(auction);
        }
    }

    /**
     * Tìm Auction từ server theo Item ID nếu không có sẵn đối tượng Auction.
     */
    public void setItemData(Item item) {
        Auction auction = loadAuctionsFromServer().stream()
                .filter(candidate -> candidate.getItem().getId().equals(item.getId()))
                .findFirst()
                .orElse(null);
        if (auction != null) {
            bindAuction(auction);
        }
    }

    /**
     * Xử lý sự kiện khi người dùng nhấn nút "Đặt giá".
     */
    @FXML
    public void handleBid() {
        if (currentAuction == null) {
            showError("Không có phiên đấu giá để đặt giá.");
            return;
        }

        try {
            BigDecimal bidAmount = parseAmount(txtBidAmount.getText());
            BigDecimal minimumAllowed = currentAuction.getCurrentPrice().add(resolveMinimumIncrement());

            // Kiểm tra mức giá đặt có hợp lệ không (phải >= giá hiện tại + bước giá)
            if (bidAmount.compareTo(minimumAllowed) < 0) {
                showError("Giá đặt tối thiểu là " + formatPrice(minimumAllowed) + ".");
                return;
            }

            String itemId = currentAuction.getItem().getId();
            String username = currentUser.getUsername();
            String amount = bidAmount.toPlainString();

            // Gửi yêu cầu đặt giá bất đồng bộ lên server
            FxAsync.run(
                    () -> networkService.placeBid(itemId, username, amount),
                    result -> {
                        txtBidAmount.clear();
                        publishNotification("Bạn đang dẫn đầu với mức " + formatPrice(bidAmount) + ".");
                        refreshAuctionState();
                    },
                    errorMsg -> showError("Đặt giá thất bại: " + errorMsg)
            );
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    /**
     * Kích hoạt tính năng đặt giá tự động (Auto-bid).
     */
    @FXML
    public void handleActivateAutoBid() {
        if (currentAuction == null) {
            showError("Không có phiên đấu giá để cấu hình auto-bid.");
            return;
        }

        try {
            autoBidMaximum = parseAmount(txtAutoBidMax.getText());
            autoBidStep = parseAmount(txtAutoBidStep.getText());
            
            // Đảm bảo bước giá tự động không nhỏ hơn bước giá tối thiểu của hệ thống
            if (autoBidStep.compareTo(resolveMinimumIncrement()) < 0) {
                showError("Bước tăng auto-bid phải từ " + formatPrice(resolveMinimumIncrement()) + " trở lên.");
                return;
            }

            autoBidEnabled = true;
            lblAutoBidStatus.setText(
                    "Đang theo dõi đến " + formatPrice(autoBidMaximum) + " với bước tăng " + formatPrice(autoBidStep) + "."
            );
            publishNotification("Auto-bid đã được kích hoạt.");
            
            // Kiểm tra xem có cần đặt giá ngay lập tức không
            maybeExecuteAutoBid();
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        } catch (Exception ex) {
            showError("Thông tin auto-bid không hợp lệ.");
        }
    }

    /**
     * Tắt tính năng đặt giá tự động.
     */
    @FXML
    public void handleDisableAutoBid() {
        autoBidEnabled = false;
        autoBidInFlight = false;
        autoBidMaximum = null;
        autoBidStep = null;
        lblAutoBidStatus.setText("Auto-bid chưa được kích hoạt.");
        publishNotification("Auto-bid đã được tắt.");
    }

    /**
     * Liên kết dữ liệu Auction vào các thành phần giao diện và khởi tạo trạng thái.
     */
    private void bindAuction(Auction auction) {
        currentAuction = auction;
        currentAuctionId = auction.getId();
        lastSeenBidCount = -1;
        lastRenderedBidCount = -1;
        lastRenderedPrice = null;
        autoBidEnabled = false;
        autoBidInFlight = false;
        autoBidMaximum = null;
        autoBidStep = null;
        lastRenderedEndTime = auction.getItem().getEndTime();
        lblAutoBidStatus.setText("Auto-bid chưa được kích hoạt.");

        lblName.setText(auction.getItem().getName());
        lblSeller.setText(auction.getSeller().getUsername());
        
        // Gợi ý mức giá đặt tối thiểu tiếp theo trong ô nhập liệu
        txtBidAmount.setText(formatInputSuggestion(auction.getCurrentPrice().add(resolveMinimumIncrement())));
        
        refreshAuctionState();
        startCountdown(lastRenderedEndTime);
    }

    /**
     * Cập nhật trạng thái hiển thị của phiên đấu giá dựa trên dữ liệu mới nhất.
     */
    private void refreshAuctionState() {
        if (currentAuction == null) return;

        // Lấy dữ liệu mới nhất của phiên này từ danh sách chung
        Auction latestAuction = findAuctionById(currentAuctionId);
        if (latestAuction != null) {
            currentAuction = latestAuction;
        }

        // Cập nhật lại countdown nếu thời gian kết thúc thay đổi
        if (!Objects.equals(lastRenderedEndTime, currentAuction.getItem().getEndTime())) {
            lastRenderedEndTime = currentAuction.getItem().getEndTime();
            startCountdown(lastRenderedEndTime);
        }

        // Cập nhật các Label thông tin
        lblPrice.setText(formatPrice(currentAuction.getCurrentPrice()));
        lblLeader.setText(maskLeader(currentAuction.getHighestBidder()));
        lblStatus.setText(resolveStatusLabel());
        lblMinimumBid.setText(formatPrice(resolveMinimumIncrement()));
        lblBidHint.setText("Giá đặt tiếp theo tối thiểu: "
                + formatPrice(currentAuction.getCurrentPrice().add(resolveMinimumIncrement())) + ".");

        // Nếu phiên đã kết thúc, chuyển sang màn hình tổng kết
        if (currentAuction.isFinished()) {
            navigateToSummary();
            return;
        }

        // Kiểm tra xem có cần vẽ lại lịch sử hoặc biểu đồ không (chỉ khi có thay đổi)
        int currentBidCount = currentAuction.getBidHistory().size();
        BigDecimal currentPrice = currentAuction.getCurrentPrice();
        if (currentBidCount != lastRenderedBidCount || !Objects.equals(currentPrice, lastRenderedPrice)) {
            redrawHistory();
            redrawChart();
            lastRenderedBidCount = currentBidCount;
            lastRenderedPrice = currentPrice;
        }
    }

    /**
     * Vẽ lại danh sách lịch sử đặt giá.
     */
    private void redrawHistory() {
        List<BidTransaction> history = currentAuction.getBidHistory();
        List<String> rows = new ArrayList<>();

        // Hiển thị lịch sử từ mới nhất đến cũ nhất
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

        // Xử lý khi có số lượt bid mới
        if (history.size() != lastSeenBidCount) {
            handleBidCountChanged(history);
            lastSeenBidCount = history.size();
        }
    }

    /**
     * Thông báo và kích hoạt auto-bid khi có người khác vừa đặt giá.
     */
    private void handleBidCountChanged(List<BidTransaction> history) {
        if (history.isEmpty()) return;

        BidTransaction latestBid = history.get(history.size() - 1);
        User latestBidder = latestBid.getBidder();

        // Bỏ qua nếu người vừa đặt giá chính là mình
        if (currentUser.getId().equals(latestBidder.getId())) {
            return;
        }

        publishNotification("Có bid mới từ " + maskUserId(latestBidder.getId())
                + ": " + formatPrice(latestBid.getBidAmount()) + ".");
        maybeExecuteAutoBid();
    }

    /**
     * Vẽ lại biểu đồ giá theo thời gian.
     */
    private void redrawChart() {
        priceSeries.getData().clear();

        List<BidTransaction> history = currentAuction.getBidHistory();
        if (history.isEmpty()) {
            priceSeries.getData().add(new XYChart.Data<>(0, currentAuction.getCurrentPrice().doubleValue()));
            return;
        }

        LocalDateTime firstBidTime = history.get(0).getBidTime();
        for (BidTransaction transaction : history) {
            // Trục X là số giây kể từ lượt bid đầu tiên
            long seconds = ChronoUnit.SECONDS.between(firstBidTime, transaction.getBidTime());
            priceSeries.getData().add(new XYChart.Data<>(seconds, transaction.getBidAmount().doubleValue()));
        }
    }

    /**
     * Logic kiểm tra và thực hiện đặt giá tự động.
     */
    private void maybeExecuteAutoBid() {
        // Kiểm tra điều kiện tiên quyết
        if (!autoBidEnabled || autoBidInFlight || currentAuction == null || autoBidMaximum == null || autoBidStep == null) {
            return;
        }

        // Không cần đặt giá nếu mình đang dẫn đầu
        User leader = currentAuction.getHighestBidder();
        if (leader != null && currentUser.getId().equals(leader.getId())) {
            return;
        }

        // Sử dụng Strategy để quyết định mức giá và xem có nên bid tiếp không
        AutoBidStrategy.AutoBidDecision decision = autoBidStrategy.decide(
                currentAuction.getCurrentPrice(),
                resolveMinimumIncrement(),
                autoBidStep,
                autoBidMaximum
        );

        if (!decision.shouldBid()) {
            autoBidEnabled = false;
            lblAutoBidStatus.setText(decision.stopReason());
            publishNotification(decision.stopReason());
            return;
        }

        BigDecimal bidAmount = decision.bidAmount();
        String itemId = currentAuction.getItem().getId();
        String username = currentUser.getUsername();
        boolean usedMax = decision.usedMaximum();

        autoBidInFlight = true;

        // Gửi lệnh đặt giá lên server
        FxAsync.run(
                () -> networkService.placeBid(itemId, username, bidAmount.toPlainString()),
                result -> {
                    autoBidInFlight = false;
                    String statusMsg = usedMax
                            ? "Auto-bid đã đặt mức tối đa: " + formatPrice(bidAmount) + "."
                            : "Auto-bid vừa nâng lên " + formatPrice(bidAmount) + ".";
                    lblAutoBidStatus.setText(statusMsg);
                    publishNotification(statusMsg);
                    refreshAuctionState();
                },
                errorMsg -> {
                    autoBidInFlight = false;
                    publishNotification("Auto-bid lỗi: " + errorMsg);
                }
        );
    }

    /**
     * Bắt đầu đồng hồ đếm ngược.
     */
    private void startCountdown(LocalDateTime endTime) {
        stopCountdown();
        updateTimeLabel(endTime);

        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            updateTimeLabel(endTime);
            // Kiểm tra nếu hết thời gian
            if (!LocalDateTime.now().isBefore(endTime) && currentAuction != null) {
                Auction latestAuction = findAuctionById(currentAuctionId);
                if (latestAuction != null) {
                    currentAuction = latestAuction;
                }
                if (!currentAuction.isFinished()) {
                    currentAuction.closeAuction();
                }
                publishNotification("Phiên đấu giá đã kết thúc.");
                navigateToSummary();
            }
        }));
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
    }

    /**
     * Chuyển sang màn hình tổng kết phiên (Summary).
     */
    private void navigateToSummary() {
        if (navigatingToSummary || currentAuction == null || lblName == null || lblName.getScene() == null) {
            return;
        }

        navigatingToSummary = true;
        stopCountdown();
        // Hủy đăng ký listener để tránh rò rỉ bộ nhớ
        networkService.removeAuctionUpdateListener(auctionUpdateListener);

        Auction latestAuction = findAuctionById(currentAuctionId);
        if (latestAuction != null) {
            currentAuction = latestAuction;
            if (!currentAuction.isFinished()) {
                currentAuction.closeAuction();
            }
        }

        Stage stage = (Stage) lblName.getScene().getWindow();
        SceneNavigator.navigateToAuctionDetailOrSummary(stage, currentAuction);
    }

    /**
     * Dừng đồng hồ đếm ngược.
     */
    private void stopCountdown() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }
    }

    /**
     * Đăng ký sự kiện hủy listener khi giao diện bị đóng/chuyển scene.
     */
    private void registerObserverLifecycle() {
        lblName.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (oldScene != null && newScene == null) {
                networkService.removeAuctionUpdateListener(auctionUpdateListener);
                stopCountdown();
            }
        });
    }

    /**
     * Xử lý khi có thông báo cập nhật danh sách đấu giá từ server.
     */
    private void handleAuctionsUpdated() {
        if (currentAuction == null) {
            List<Auction> auctions = loadAuctionsFromServer();
            if (!auctions.isEmpty()) {
                bindAuction(auctions.get(0));
            }
            return;
        }
        refreshAuctionState();
    }

    /**
     * Cập nhật nhãn hiển thị thời gian (Ngày và Giờ:Phút:Giây).
     */
    private void updateTimeLabel(LocalDateTime endTime) {
        LocalDateTime now = LocalDateTime.now();
        if (!now.isBefore(endTime)) {
            lblTime.setText("0 ngày 00:00:00");
            return;
        }

        // Tách rõ phần ngày và phần giờ/phút/giây để dễ đọc cho các phiên dài
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
     * Chuyển đổi văn bản nhập liệu thành BigDecimal (loại bỏ ký tự không phải số).
     */
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

    /**
     * Xác định bước giá tối thiểu cho phiên đấu giá hiện tại.
     * Thường là 1% giá hiện tại hoặc một mức sàn cố định.
     */
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

    /**
     * Trả về nhãn trạng thái của phiên.
     */
    private String resolveStatusLabel() {
        if (currentAuction.isFinished()) {
            return "FINISHED";
        }
        return currentAuction.isActive() ? "RUNNING" : "PAUSED";
    }

    /**
     * Định dạng số cho ô nhập liệu.
     */
    private String formatInputSuggestion(BigDecimal amount) {
        return PriceFormatter.formatNumber(amount);
    }

    /**
     * Định dạng tiền tệ để hiển thị (ví dụ: 1.000.000 VNĐ).
     */
    private String formatPrice(BigDecimal amount) {
        return PriceFormatter.formatPrice(amount);
    }

    /**
     * Che thông tin người dẫn đầu để bảo mật.
     */
    private String maskLeader(User user) {
        return user == null ? "Chưa có" : maskUserId(user.getId());
    }

    /**
     * Rút gọn ID người dùng (chỉ hiện 8 ký tự đầu).
     */
    private String maskUserId(String userId) {
        if (userId == null || userId.length() <= 8) {
            return userId == null ? "Ẩn danh" : userId;
        }
        return userId.substring(0, 8) + "...";
    }

    /**
     * Tìm đối tượng Auction trong snapshot từ server theo ID.
     */
    private Auction findAuctionById(String auctionId) {
        if (auctionId == null || auctionId.isBlank()) {
            return null;
        }
        return loadAuctionsFromServer().stream()
                .filter(auction -> auctionId.equals(auction.getId()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Tải danh sách phiên đấu giá mới nhất từ server.
     */
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

    /**
     * Hiển thị thông báo nhanh trên giao diện.
     */
    private void publishNotification(String message) {
        lblNotification.setText(message);
    }

    /**
     * Hiển thị hộp thoại lỗi.
     */
    private void showError(String message) {
        AlertHelper.showError("Lỗi", message);
    }
}
