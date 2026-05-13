package com.auction.util;

import javafx.scene.control.Alert;

/**
 * Tiện ích hiển thị thông báo (Alert) cho người dùng.
 * Tập trung logic hiển thị dialog để tránh lặp code ở mỗi Controller.
 */
public final class AlertHelper {

    private AlertHelper() {}

    /**
     * Hiển thị thông báo dạng Information.
     */
    public static void showInformation(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Hiển thị thông báo lỗi dạng Error.
     */
    public static void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
