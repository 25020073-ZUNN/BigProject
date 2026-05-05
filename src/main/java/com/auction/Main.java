package com.auction;

import com.auction.config.DBConnection;
import com.auction.service.AuthService;
import com.auction.network.Server;
import com.auction.util.LoggingConfig;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Main extends Application {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    private Server embeddedServer;

    @Override
    public void start(Stage stage) throws Exception {
        LoggingConfig.configure();
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
                LOGGER.log(Level.SEVERE, "Embedded server failed", e);
            }
        }, "auction-embedded-server");
        serverThread.setDaemon(true);
        serverThread.start();
    }

    private void logDatabaseConfiguration() {
        LOGGER.info(() -> "[DB] Configured URL: " + DBConnection.getConfiguredUrl());
        LOGGER.info(() -> "[DB] Configured user: " + DBConnection.getConfiguredUser());
        if (AuthService.getInstance().isDatabaseAvailable()) {
            LOGGER.info("[DB] Connection status: CONNECTED");
        } else {
            LOGGER.warning("[DB] Connection status: UNAVAILABLE");
        }
    }

    public static void main(String[] args) {
        launch();
    }
}
