package com.auction.util;

import javafx.scene.Scene;
import javafx.scene.Parent;

import java.net.URL;
import java.util.prefs.Preferences;

/**
 * ThemeManager — Singleton quản lý chế độ sáng/tối cho toàn bộ ứng dụng.
 *
 * Cách hoạt động:
 * - Lưu lựa chọn theme vào java.util.prefs.Preferences (tương đương localStorage)
 * - Nếu chưa có lựa chọn, tự detect theo OS dark mode setting
 * - Khi bật dark mode, thêm dark-theme.css vào scene stylesheets
 * - Khi tắt dark mode, xóa dark-theme.css khỏi scene stylesheets
 *
 * Sử dụng:
 *   ThemeManager.getInstance().applyTheme(scene);     // Áp dụng theme hiện tại
 *   ThemeManager.getInstance().toggleTheme(scene);     // Chuyển đổi dark/light
 *   ThemeManager.getInstance().isDarkMode();            // Kiểm tra trạng thái
 */
public final class ThemeManager {

    private static final ThemeManager INSTANCE = new ThemeManager();

    /** Key lưu preference */
    private static final String PREF_KEY = "darkMode";

    /** Giá trị đặc biệt khi chưa có lựa chọn */
    private static final String PREF_UNSET = "unset";

    /** Đường dẫn tới dark theme CSS */
    private static final String DARK_CSS_PATH = "/dark-theme.css";

    /** Trạng thái dark mode hiện tại */
    private boolean darkMode;

    /** Preferences để lưu trữ lâu dài */
    private final Preferences prefs;

    /** URL của dark-theme.css (cache lại để tránh lookup nhiều lần) */
    private String darkCssUrl;

    private ThemeManager() {
        prefs = Preferences.userNodeForPackage(ThemeManager.class);

        // Tìm và cache URL của dark-theme.css
        URL darkCssResource = getClass().getResource(DARK_CSS_PATH);
        if (darkCssResource != null) {
            darkCssUrl = darkCssResource.toExternalForm();
        }

        // Khởi tạo trạng thái: ưu tiên preference đã lưu, nếu chưa có thì detect OS
        String savedPref = prefs.get(PREF_KEY, PREF_UNSET);
        if (PREF_UNSET.equals(savedPref)) {
            darkMode = detectOsDarkMode();
        } else {
            darkMode = Boolean.parseBoolean(savedPref);
        }
    }

    /**
     * Lấy instance duy nhất của ThemeManager.
     */
    public static ThemeManager getInstance() {
        return INSTANCE;
    }

    /**
     * Kiểm tra đang ở chế độ dark mode hay không.
     */
    public boolean isDarkMode() {
        return darkMode;
    }

    /**
     * Chuyển đổi giữa dark/light mode và áp dụng lên scene.
     *
     * @param scene Scene hiện tại cần áp dụng theme
     */
    public void toggleTheme(Scene scene) {
        darkMode = !darkMode;
        prefs.put(PREF_KEY, String.valueOf(darkMode));
        applyTheme(scene);
    }

    /**
     * Áp dụng theme hiện tại lên scene.
     * Thêm hoặc xóa dark-theme.css khỏi danh sách stylesheets.
     *
     * @param scene Scene cần áp dụng theme
     */
    public void applyTheme(Scene scene) {
        if (scene == null || darkCssUrl == null) {
            return;
        }

        if (darkMode) {
            if (!scene.getStylesheets().contains(darkCssUrl)) {
                scene.getStylesheets().add(darkCssUrl);
            }
        } else {
            scene.getStylesheets().remove(darkCssUrl);
        }

        // Cực kỳ quan trọng: Áp dụng/xóa theme trên Root Node của Scene 
        // để override được stylesheets="@../style.css" khai báo trong FXML!
        if (scene.getRoot() != null) {
            applyTheme(scene.getRoot());
        }
    }

    /**
     * Áp dụng theme hiện tại lên Parent node (dùng cho loading screen
     * hoặc các node chưa gắn vào scene).
     *
     * @param root Parent node cần áp dụng theme
     */
    public void applyTheme(Parent root) {
        if (root == null || darkCssUrl == null) {
            return;
        }

        if (darkMode) {
            if (!root.getStylesheets().contains(darkCssUrl)) {
                root.getStylesheets().add(darkCssUrl);
            }
        } else {
            root.getStylesheets().remove(darkCssUrl);
        }
    }

    /**
     * Detect xem hệ điều hành đang dùng dark mode không.
     * Hỗ trợ Windows 10+ (registry) và macOS (defaults).
     * Mặc định trả về false nếu không detect được.
     */
    private boolean detectOsDarkMode() {
        try {
            String os = System.getProperty("os.name", "").toLowerCase();

            if (os.contains("win")) {
                // Windows 10+: Đọc registry key AppsUseLightTheme
                // Giá trị 0 = dark mode, 1 = light mode
                ProcessBuilder pb = new ProcessBuilder(
                        "reg", "query",
                        "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
                        "/v", "AppsUseLightTheme"
                );
                pb.redirectErrorStream(true);
                Process process = pb.start();
                String output = new String(process.getInputStream().readAllBytes());
                process.waitFor();

                // Tìm giá trị "0x0" hoặc "0x1"
                if (output.contains("0x0")) {
                    return true; // Dark mode
                }
                return false; // Light mode

            } else if (os.contains("mac")) {
                // macOS: Kiểm tra Dark mode qua defaults
                ProcessBuilder pb = new ProcessBuilder(
                        "defaults", "read", "-g", "AppleInterfaceStyle"
                );
                pb.redirectErrorStream(true);
                Process process = pb.start();
                String output = new String(process.getInputStream().readAllBytes());
                process.waitFor();

                return output.trim().equalsIgnoreCase("Dark");
            }

        } catch (Exception e) {
            // Không detect được → mặc định light mode
            System.out.println("[ThemeManager] Không thể detect OS dark mode: " + e.getMessage());
        }

        return false; // Mặc định light mode
    }
}
