package com.auction.controller;

import com.auction.model.Auction;
import com.auction.model.item.Item;
import com.auction.model.user.User;
import com.auction.service.AuctionService;
import com.auction.util.UserSession;
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
import com.auction.service.AuctionService;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
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
 * - Luôn đọc danh sách tài sản thật từ AuctionService/DB.
 * - Cho phép tìm kiếm và lọc theo danh mục/trạng thái ngay trên dữ liệu thật.
 */
public class AuctionCatalogController {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final DecimalFormat PRICE_FORMAT = createPriceFormat();

    private final AuctionService auctionService = AuctionService.getInstance();

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

        updateLoginState();

        statusFilter.setItems(FXCollections.observableArrayList("Tất cả", "Sắp diễn ra", "Đang diễn ra", "Đã kết thúc"));
        statusFilter.setValue("Tất cả");

        searchField.textProperty().addListener((observable, oldValue, newValue) -> renderAuctions());
        categoryFilter.valueProperty().addListener((observable, oldValue, newValue) -> renderAuctions());
        statusFilter.valueProperty().addListener((observable, oldValue, newValue) -> renderAuctions());

        renderAuctions();
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
        renderAuctions();
    }

    private void renderAuctions() {
        auctionService.refreshAuctions();
        List<Auction> filteredAuctions = filterAuctions(auctionService.getAllAuctions());

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

    private List<Auction> filterAuctions(List<Auction> auctions) {
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        String category = categoryFilter.getValue();
        String status = statusFilter.getValue();

        /*
         * Ghi chú quan trọng:
         * Bộ lọc phải chạy trên dữ liệu thật lấy từ DB thay vì text mẫu trong FXML.
         * Vì vậy mỗi lần render, controller luôn lấy snapshot mới nhất từ AuctionService,
         * sau đó áp dụng toàn bộ điều kiện lọc trong Java để tránh phụ thuộc giao diện cứng.
         */
        return auctions.stream()
                .filter(auction -> matchesKeyword(auction, keyword))
                .filter(auction -> matchesCategory(auction, category))
                .filter(auction -> matchesStatus(auction, status))
                .collect(Collectors.toList());
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
            showError("Không thể mở màn hình chi tiết phiên đấu giá.");
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

    @FXML
    public void goToCreateAuction(ActionEvent event) {
        switchScene(event, "create-auction.fxml");
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
