package com.auction;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class HomeController {

    @FXML
    private Label clockLabel;

    @FXML
    private TextField searchField;

    @FXML
    public void initialize() {
        if (clockLabel != null) {
            initClock();
        }
    }

    private void showInformation(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
    @FXML
    public void handleLogin(ActionEvent event) {
        System.out.println("Login clicked");

        // 👉 sau này sẽ thêm logic login thật ở đây
        goToHome(event); // tạm thời chuyển màn
    }
    @FXML
    public void handleBid(ActionEvent event) {
        showInformation("Đặt giá thành công", "Bạn đã đặt giá thành công cho tài sản này!\nChúng tôi sẽ thông báo cho bạn nếu có người đặt giá cao hơn.");
    }

    @FXML
    public void handleSearch(ActionEvent event) {
        String query = (searchField != null) ? searchField.getText() : "";
        showInformation("Tìm kiếm tài sản", "Đang lọc danh sách tài sản với từ khóa: " + (query.isEmpty() ? "Tất cả" : query));
    }

    @FXML
    public void handleFollow(ActionEvent event) {
        showInformation("Theo dõi", "Tài sản này đã được thêm vào danh sách quan tâm của bạn.");
    }

    @FXML
    public void handleComingSoon(ActionEvent event) {
        showInformation("Tính năng sắp ra mắt", "Cảm ơn bạn quan tâm! Tính năng này hiện đang được hoàn thiện.");
    }

    @FXML
    public void handleSubscribe(ActionEvent event) {
        showInformation("Đăng ký thành công", "Chúng tôi sẽ gửi các bản tin đấu giá mới nhất qua email của bạn.");
    }

    private void initClock() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss\ndd/MM/yyyy");
        Timeline clock = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            clockLabel.setText(LocalDateTime.now().format(formatter));
        }));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();

        // Cập nhật ngay lập tức khi khởi tạo
        clockLabel.setText(LocalDateTime.now().format(formatter));
    }

    private void switchScene(ActionEvent event, String fxmlFile) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/" + fxmlFile));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
    public void goToLogin(ActionEvent event) {
        switchScene(event, "login.fxml");
    }

    @FXML
    public void goToRegister(ActionEvent event) {
        switchScene(event, "register.fxml");
    }

    @FXML
    public void goToProductDetail(ActionEvent event) {
        switchScene(event, "product-detail.fxml");
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
}
