package com.auction;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/giaodien.fxml"));
        Scene scene = new Scene(loader.load());
        stage.setTitle("Aurex Auction Platform");
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
