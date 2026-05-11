package com.auction;

import com.auction.config.DBConnection;
import com.auction.service.AuthService;
import com.auction.network.server.Server;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Lớp Main - Điểm vào chính của ứng dụng.
 * Khởi tạo giao diện đồ họa (JavaFX), Server nội bộ và kiểm tra kết nối cơ sở dữ liệu.
 */
public class Main extends Application {

    private Server embeddedServer; // Server nhúng để xử lý các yêu cầu mạng nội bộ

    @Override
    public void start(Stage stage) throws Exception {
        // 1. Khởi động Server nhúng trong một luồng riêng biệt
        startEmbeddedServer();
        
        // 2. Kiểm tra và in thông tin cấu hình Database ra console
        logDatabaseConfiguration();

        // 3. Nạp giao diện chính từ file FXML
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/giaodien.fxml"));
        Scene scene = new Scene(loader.load());
        
        // 4. Thiết lập các thông số cho cửa sổ ứng dụng
        stage.setTitle("HỆ THỐNG ĐẤU GIÁ - NHÓM 6 UET");
        stage.setScene(scene);
        stage.setMinWidth(1280);
        stage.setMinHeight(820);
        stage.centerOnScreen();
        stage.show();
    }

    /**
     * Dừng ứng dụng và giải phóng tài nguyên.
     */
    @Override
    public void stop() throws Exception {
        if (embeddedServer != null) {
            embeddedServer.stop(); // Dừng server khi tắt ứng businessman
        }
        super.stop();
    }

    /**
     * Khởi động server nhúng dưới dạng Daemon Thread để không chặn luồng giao diện.
     */
    private void startEmbeddedServer() {
        embeddedServer = new Server();
        Thread serverThread = new Thread(() -> {
            try {
                embeddedServer.start();
            } catch (java.net.BindException ignored) {
                // Đã có một instance khác đang chạy và giữ Port
            } catch (Exception e) {
                System.err.println("Lỗi khởi động Server nhúng: " + e.getMessage());
            }
        }, "auction-embedded-server");
        serverThread.setDaemon(true); // Tự động đóng thread khi ứng dụng chính thoát
        serverThread.start();
    }

    /**
     * Ghi nhận thông tin cấu hình cơ sở dữ liệu hiện tại để dễ dàng gỡ lỗi.
     */
    private void logDatabaseConfiguration() {
        System.out.println("[DB] URL cấu hình: " + DBConnection.getConfiguredUrl());
        System.out.println("[DB] Tài khoản DB: " + DBConnection.getConfiguredUser());
        
        // Kiểm tra xem thực tế có kết nối được đến MySQL không
        if (AuthService.getInstance().isDatabaseAvailable()) {
            System.out.println("[DB] Trạng thái: ĐÃ KẾT NỐI");
        } else {
            System.out.println("[DB] Trạng thái: KHÔNG KHẢ DỤNG (Vui lòng kiểm tra Docker/MySQL)");
        }
    }

    public static void main(String[] args) {
        launch(); // Kích hoạt vòng đời JavaFX
    }
}
