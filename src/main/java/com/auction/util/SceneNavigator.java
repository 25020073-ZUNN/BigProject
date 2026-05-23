package com.auction.util;

import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;

import com.auction.model.Auction;
import com.auction.controller.AssetDetailController;
import com.auction.controller.AuctionDetailController;
import com.auction.controller.AuctionSummaryController;

/**
 * Tiện ích điều hướng giữa các màn hình (Scene) trong ứng dụng.
 */
public final class SceneNavigator {

    private static final double LOADING_DELAY_MS = 80;

    private SceneNavigator() {}

    /**
     * Chuyển sang Scene mới. Hiển thị loading tạm thời, load FXML rồi set root trực tiếp.
     */
    public static void switchScene(ActionEvent event, String fxmlFile) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        switchScene(stage, "Không thể tải giao diện: " + fxmlFile, () ->
                FXMLLoader.load(SceneNavigator.class.getResource("/fxml/" + fxmlFile)));
    }

    private static void switchScene(Stage stage, String errorMessage, PageLoader loader) {
        if (stage == null) return;

        Parent previousRoot = stage.getScene() == null ? null : stage.getScene().getRoot();
        showLoading(stage);

        PauseTransition delay = new PauseTransition(Duration.millis(LOADING_DELAY_MS));
        delay.setOnFinished(event -> {
            try {
                Parent root = loader.load();
                applyRoot(stage, root);
            } catch (IOException e) {
                e.printStackTrace();
                if (previousRoot != null) {
                    applyRoot(stage, previousRoot);
                }
                AlertHelper.showError("Lỗi chuyển trang", errorMessage);
            }
        });
        delay.play();
    }

    private static void showLoading(Stage stage) {
        StackPane loadingRoot = new StackPane();
        loadingRoot.getStyleClass().addAll("root", "page-loading-root");
        URL stylesheet = SceneNavigator.class.getResource("/style.css");
        if (stylesheet != null) {
            loadingRoot.getStylesheets().add(stylesheet.toExternalForm());
        }

        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(72, 72);
        spinner.getStyleClass().add("page-loading-spinner");

        Label brand = new Label("AUREX");
        brand.getStyleClass().add("page-loading-brand");

        Label status = new Label("Đang chuyển trang...");
        status.getStyleClass().add("page-loading-text");

        VBox content = new VBox(14, spinner, brand, status);
        content.setAlignment(Pos.CENTER);
        content.getStyleClass().add("page-loading-card");
        loadingRoot.getChildren().add(content);

        applyRoot(stage, loadingRoot);
    }

    public static void goToHome(ActionEvent event) { switchScene(event, "giaodien.fxml"); }
    public static void goToLogin(ActionEvent event) { switchScene(event, "login.fxml"); }
    public static void goToForgotPassword(ActionEvent event) { switchScene(event, "forgot-password.fxml"); }
    public static void goToRegister(ActionEvent event) { switchScene(event, "register.fxml"); }
    public static void goToAuctionList(ActionEvent event) { switchScene(event, "auction-detail.fxml"); }
    public static void goToProductDetail(ActionEvent event) { switchScene(event, "product-detail.fxml"); }
    public static void goToSessions(ActionEvent event) { goToAuctionList(event); }
    public static void goToNews(ActionEvent event) { switchScene(event, "news.fxml"); }
    public static void goToContact(ActionEvent event) { switchScene(event, "contact.fxml"); }
    public static void goToCreateAuction(ActionEvent event) { switchScene(event, "create-auction.fxml"); }
    public static void goToAuctionSummary(ActionEvent event) { switchScene(event, "auction-summary.fxml"); }
    public static void goToUserProfile(ActionEvent event) { switchScene(event, "user-profile.fxml"); }
    public static void goToAdminDashboard(ActionEvent event) { switchScene(event, "admin-dashboard.fxml"); }
    public static void goToAuctionHistory(ActionEvent event) { switchScene(event, "auction-history.fxml"); }

    /**
     * Điều hướng tới trang chi tiết tài sản (asset-detail.fxml).
     * Đây là trang trung gian hiển thị thông tin tổng quan + countdown,
     * trước khi người dùng vào phòng đấu giá hoặc xem tổng kết.
     *
     * Luồng: AuctionCatalog → AssetDetail → ProductDetail/AuctionSummary
     */
    public static void navigateToAssetDetail(Stage stage, Auction auction) {
        if (auction == null) return;
        switchScene(stage, "Không thể tải trang chi tiết tài sản.", () -> {
            FXMLLoader loader = new FXMLLoader(SceneNavigator.class.getResource("/fxml/asset-detail.fxml"));
            Parent root = loader.load();

            AssetDetailController ctrl = loader.getController();
            ctrl.setAuctionData(auction);

            return root;
        });
    }

    /**
     * Điều hướng thông minh dựa trên trạng thái phiên đấu giá.
     * Running → product-detail.fxml (đấu giá realtime)
     * Finished → auction-summary.fxml (tổng kết)
     */
    public static void navigateToAuctionDetailOrSummary(Stage stage, Auction auction) {
        if (auction == null) return;
        switchScene(stage, "Không thể tải giao diện chi tiết/tổng kết.", () -> {
            String fxml = auction.isFinished() ? "/fxml/auction-summary.fxml" : "/fxml/product-detail.fxml";
            FXMLLoader loader = new FXMLLoader(SceneNavigator.class.getResource(fxml));
            Parent root = loader.load();

            Object ctrl = loader.getController();
            if (ctrl instanceof AuctionSummaryController s) s.setAuctionData(auction);
            else if (ctrl instanceof AuctionDetailController d) d.setAuctionData(auction);

            return root;
        });
    }

    private static void applyRoot(Stage stage, Parent root) {
        Scene s = stage.getScene();
        if (s == null) stage.setScene(new Scene(root, 1380, 920));
        else s.setRoot(root);
        stage.setMinWidth(1280);
        stage.setMinHeight(820);
    }

    @FunctionalInterface
    private interface PageLoader {
        Parent load() throws IOException;
    }
}
