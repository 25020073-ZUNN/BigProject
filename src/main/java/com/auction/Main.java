package com.auction;

import com.auction.config.DBConnection;
import com.auction.service.AuthService;
import com.auction.network.Server;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    private Server embeddedServer;

    @Override
    public void start(Stage stage) throws Exception {
        startEmbeddedServer();
        logDatabaseConfiguration();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/giaodien.fxml"));
        Scene scene = new Scene(loader.load());
        stage.setTitle("Aurex Auction Platform");
        stage.setScene(scene);
        stage.setMinWidth(1280);
        stage.setMinHeight(820);
        stage.centerOnScreen();
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        if (embeddedServer != null) {
            embeddedServer.stop();
        }
        super.stop();
    }

    private void startEmbeddedServer() {
        embeddedServer = new Server();
        Thread serverThread = new Thread(() -> {
            try {
                embeddedServer.start();
            } catch (java.net.BindException ignored) {
                // Another server is already listening on this port.
            } catch (Exception e) {
                System.err.println("Embedded server failed: " + e.getMessage());
            }
        }, "auction-embedded-server");
        serverThread.setDaemon(true);
        serverThread.start();
    }

    private void logDatabaseConfiguration() {
        System.out.println("[DB] Configured URL: " + DBConnection.getConfiguredUrl());
        System.out.println("[DB] Configured user: " + DBConnection.getConfiguredUser());
        if (AuthService.getInstance().isDatabaseAvailable()) {
            System.out.println("[DB] Connection status: CONNECTED");
        } else {
            System.out.println("[DB] Connection status: UNAVAILABLE");
        }
    }

    public static void main(String[] args) {
        launch();
    }
}
