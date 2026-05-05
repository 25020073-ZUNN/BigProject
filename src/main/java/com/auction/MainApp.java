package com.auction;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        // Tìm và nạp file giao diện FXML
        FXMLLoader fxmlLoader = new FXMLLoader(MainApp.class.getResource("/auctions.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1380, 920);

        stage.setTitle("Hệ thống Đấu giá - Danh sách sản phẩm");
        stage.setScene(scene);
        stage.setMinWidth(1280);
        stage.setMinHeight(820);
        stage.centerOnScreen();
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
