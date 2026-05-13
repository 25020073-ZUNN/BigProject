package com.auction.util;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

import com.auction.model.Auction;
import com.auction.controller.AuctionDetailController;
import com.auction.controller.AuctionSummaryController;

/**
 * Tiện ích điều hướng giữa các màn hình (Scene) trong ứng dụng.
 * Tập trung toàn bộ logic chuyển cảnh để tránh lặp code ở mỗi Controller.
 */
public final class SceneNavigator {

    private SceneNavigator() {
    }

    /**
     * Chuyển sang Scene mới từ file FXML.
     */
    public static void switchScene(ActionEvent event, String fxmlFile) {
        try {
            Parent root = FXMLLoader.load(SceneNavigator.class.getResource("/" + fxmlFile));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene currentScene = stage.getScene();
            if (currentScene == null) {
                stage.setScene(new Scene(root, 1380, 920));
            } else {
                currentScene.setRoot(root);
            }
            stage.setMinWidth(1280);
            stage.setMinHeight(820);
        } catch (IOException e) {
            e.printStackTrace();
            AlertHelper.showError("Lỗi chuyển trang", "Không thể tải giao diện: " + fxmlFile);
        }
    }

    // --- Các phương thức điều hướng chuẩn ---

    public static void goToHome(ActionEvent event) {
        switchScene(event, "giaodien.fxml");
    }

    public static void goToLogin(ActionEvent event) {
        switchScene(event, "login.fxml");
    }

    public static void goToRegister(ActionEvent event) {
        switchScene(event, "register.fxml");
    }

    public static void goToAuctionList(ActionEvent event) {
        switchScene(event, "auction-detail.fxml");
    }

    public static void goToProductDetail(ActionEvent event) {
        switchScene(event, "product-detail.fxml");
    }

    public static void goToSessions(ActionEvent event) {
        switchScene(event, "sessions.fxml");
    }

    public static void goToNews(ActionEvent event) {
        switchScene(event, "news.fxml");
    }

    public static void goToContact(ActionEvent event) {
        switchScene(event, "contact.fxml");
    }

    public static void goToCreateAuction(ActionEvent event) {
        switchScene(event, "create-auction.fxml");
    }

    public static void goToAuctionSummary(ActionEvent event) {
        switchScene(event, "auction-summary.fxml");
    }

    /**
     * Điều hướng thông minh: tự động chọn màn hình phù hợp dựa trên trạng thái phiên.
     * - Finished → auction-summary.fxml
     * - Running/Upcoming → product-detail.fxml
     *
     * Truyền thẳng đối tượng Auction vào controller đích để tránh gọi lại server (giảm lag).
     */
    public static void navigateToAuctionDetailOrSummary(Stage stage, Auction auction) {
        if (auction == null) return;
        try {
            String fxml = auction.isFinished() ? "/auction-summary.fxml" : "/product-detail.fxml";
            FXMLLoader loader = new FXMLLoader(SceneNavigator.class.getResource(fxml));
            Parent root = loader.load();

            // Truyền dữ liệu vào controller tương ứng
            Object ctrl = loader.getController();
            if (ctrl instanceof AuctionSummaryController summary) {
                summary.setAuctionData(auction);
            } else if (ctrl instanceof AuctionDetailController detail) {
                detail.setAuctionData(auction);
            }

            applyRoot(stage, root);
        } catch (IOException e) {
            e.printStackTrace();
            AlertHelper.showError("Lỗi chuyển trang", "Không thể tải giao diện chi tiết/tổng kết phiên đấu giá.");
        }
    }

    /**
     * Áp dụng root mới lên Stage hiện tại.
     */
    private static void applyRoot(Stage stage, Parent root) {
        Scene currentScene = stage.getScene();
        if (currentScene == null) {
            stage.setScene(new Scene(root, 1380, 920));
        } else {
            currentScene.setRoot(root);
        }
        stage.setMinWidth(1280);
        stage.setMinHeight(820);
    }
}
