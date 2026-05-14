package com.auction.util;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;

import com.auction.model.Auction;
import com.auction.controller.AuctionDetailController;
import com.auction.controller.AuctionSummaryController;

/**
 * Tiện ích điều hướng giữa các màn hình (Scene) trong ứng dụng.
 *
 * Hiệu ứng: Fade-in cực nhanh (120ms) — đủ mượt để mắt nhận biết
 * nhưng không gây cảm giác lag. Không dùng Scale vì tốn tài nguyên render.
 */
public final class SceneNavigator {

    /** 120ms = gần như tức thì, chỉ đủ để mắt nhận biết sự chuyển đổi mượt */
    private static final double FADE_MS = 120;

    private SceneNavigator() {}

    /**
     * Chuyển sang Scene mới. Load FXML → set root → fade-in nhanh.
     */
    public static void switchScene(ActionEvent event, String fxmlFile) {
        try {
            Parent root = FXMLLoader.load(SceneNavigator.class.getResource("/" + fxmlFile));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            root.setOpacity(0);

            Scene currentScene = stage.getScene();
            if (currentScene == null) {
                stage.setScene(new Scene(root, 1380, 920));
            } else {
                currentScene.setRoot(root);
            }
            stage.setMinWidth(1280);
            stage.setMinHeight(820);

            // Fade-in cực nhanh — không gây lag, vẫn mượt
            FadeTransition ft = new FadeTransition(Duration.millis(FADE_MS), root);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.setInterpolator(Interpolator.EASE_IN);
            ft.play();
        } catch (IOException e) {
            e.printStackTrace();
            AlertHelper.showError("Lỗi chuyển trang", "Không thể tải giao diện: " + fxmlFile);
        }
    }

    public static void goToHome(ActionEvent event) { switchScene(event, "giaodien.fxml"); }
    public static void goToLogin(ActionEvent event) { switchScene(event, "login.fxml"); }
    public static void goToRegister(ActionEvent event) { switchScene(event, "register.fxml"); }
    public static void goToAuctionList(ActionEvent event) { switchScene(event, "auction-detail.fxml"); }
    public static void goToProductDetail(ActionEvent event) { switchScene(event, "product-detail.fxml"); }
    public static void goToSessions(ActionEvent event) { switchScene(event, "sessions.fxml"); }
    public static void goToNews(ActionEvent event) { switchScene(event, "news.fxml"); }
    public static void goToContact(ActionEvent event) { switchScene(event, "contact.fxml"); }
    public static void goToCreateAuction(ActionEvent event) { switchScene(event, "create-auction.fxml"); }
    public static void goToAuctionSummary(ActionEvent event) { switchScene(event, "auction-summary.fxml"); }
    public static void goToUserProfile(ActionEvent event) { switchScene(event, "user-profile.fxml"); }

    /**
     * Điều hướng thông minh dựa trên trạng thái phiên đấu giá.
     */
    public static void navigateToAuctionDetailOrSummary(Stage stage, Auction auction) {
        if (auction == null) return;
        try {
            String fxml = auction.isFinished() ? "/auction-summary.fxml" : "/product-detail.fxml";
            FXMLLoader loader = new FXMLLoader(SceneNavigator.class.getResource(fxml));
            Parent root = loader.load();

            Object ctrl = loader.getController();
            if (ctrl instanceof AuctionSummaryController s) s.setAuctionData(auction);
            else if (ctrl instanceof AuctionDetailController d) d.setAuctionData(auction);

            root.setOpacity(0);
            applyRoot(stage, root);

            FadeTransition ft = new FadeTransition(Duration.millis(FADE_MS), root);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.setInterpolator(Interpolator.EASE_IN);
            ft.play();
        } catch (IOException e) {
            e.printStackTrace();
            AlertHelper.showError("Lỗi chuyển trang", "Không thể tải giao diện chi tiết/tổng kết.");
        }
    }

    private static void applyRoot(Stage stage, Parent root) {
        Scene s = stage.getScene();
        if (s == null) stage.setScene(new Scene(root, 1380, 920));
        else s.setRoot(root);
        stage.setMinWidth(1280);
        stage.setMinHeight(820);
    }
}
