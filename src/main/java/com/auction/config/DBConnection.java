package com.auction.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Lớp DBConnection chịu trách nhiệm thiết lập và quản lý kết nối tới cơ sở dữ liệu MySQL.
 * Hỗ trợ đọc cấu hình từ file môi trường (.env.local) hoặc sử dụng giá trị mặc định.
 */
public final class DBConnection {
    // Thông số mặc định nếu không tìm thấy cấu hình bên ngoài
    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3307/auction_db";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "123456";
    
    // Lưu trữ các biến môi trường đọc từ file .env.local
    private static final Map<String, String> LOCAL_ENV = loadLocalEnv();

    private DBConnection() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Tạo và trả về một kết nối mới tới cơ sở dữ liệu.
     * @return Connection đối tượng kết nối JDBC.
     * @throws SQLException Nếu có lỗi xảy ra khi kết nối.
     */
    public static Connection getConnection() throws SQLException {
        // DriverManager sẽ dùng URL, Username và Password để mở một "đường truyền" tới MySQL
        return DriverManager.getConnection(getUrl(), getUser(), getPassword());
    }

    /**
     * Lấy URL kết nối đã cấu hình (dùng để hiển thị trạng thái).
     */
    public static String getConfiguredUrl() {
        return getUrl();
    }

    /**
     * Lấy tên đăng nhập DB đã cấu hình.
     */
    public static String getConfiguredUser() {
        return getUser();
    }

    // Các hàm bổ trợ để lấy thông tin cấu hình theo thứ tự ưu tiên: 
    // Hệ thống -> File .env -> Giá trị mặc định
    private static String getUrl() {
        return getConfig("DB_URL", "db.url", DEFAULT_URL);
    }

    private static String getUser() {
        return getConfig("DB_USER", "db.user", DEFAULT_USER);
    }

    private static String getPassword() {
        return getConfig("DB_PASSWORD", "db.password", DEFAULT_PASSWORD);
    }

    /**
     * Hàm lấy giá trị cấu hình dựa trên khóa (key).
     */
    private static String getConfig(String envKey, String propertyKey, String defaultValue) {
        // 1. Kiểm tra biến môi trường hệ thống (System Environment)
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }

        // 2. Kiểm tra biến từ file .env.local
        String localEnvValue = LOCAL_ENV.get(envKey);
        if (localEnvValue != null && !localEnvValue.isBlank()) {
            return localEnvValue;
        }

        // 3. Kiểm tra thuộc tính hệ thống Java (-Dkey=value)
        String propertyValue = System.getProperty(propertyKey);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue;
        }

        // 4. Trả về giá trị mặc định nếu không tìm thấy ở đâu
        return defaultValue;
    }

    /**
     * Tự động tìm và đọc file .env.local trong các thư mục của dự án.
     */
    private static Map<String, String> loadLocalEnv() {
        for (Path envFile : getLocalEnvCandidates()) {
            if (Files.isRegularFile(envFile)) {
                return readLocalEnv(envFile);
            }
        }
        return Map.of();
    }

    /**
     * Đọc và phân tích cú pháp file .env.local (định dạng KEY=VALUE).
     */
    private static Map<String, String> readLocalEnv(Path envFile) {
        Map<String, String> values = new HashMap<>();
        try {
            List<String> lines = Files.readAllLines(envFile);
            for (String rawLine : lines) {
                String line = rawLine.trim();
                // Bỏ qua dòng trống hoặc dòng chú thích (bắt đầu bằng #)
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                int separatorIndex = line.indexOf('=');
                if (separatorIndex <= 0) {
                    continue;
                }

                String key = line.substring(0, separatorIndex).trim();
                String value = stripQuotes(line.substring(separatorIndex + 1).trim());
                if (!key.isEmpty() && !value.isEmpty()) {
                    values.put(key, value);
                }
            }
        } catch (IOException e) {
            System.err.println("[DB] Không thể đọc file " + envFile + ": " + e.getMessage());
        }

        return Map.copyOf(values);
    }

    /**
     * Xác định các vị trí khả nghi có thể chứa file .env.local.
     */
    private static Set<Path> getLocalEnvCandidates() {
        Set<Path> candidates = new LinkedHashSet<>();
        // Thư mục chạy ứng dụng
        addLocalEnvCandidates(candidates, Path.of(System.getProperty("user.dir", ".")));
        // Thư mục gốc tuyệt đối
        addLocalEnvCandidates(candidates, Path.of("").toAbsolutePath());

        return candidates;
    }

    private static void addLocalEnvCandidates(Set<Path> candidates, Path start) {
        Path current = start.toAbsolutePath().normalize();
        if (Files.isRegularFile(current)) {
            current = current.getParent();
        }

        // Tìm ngược lên các thư mục cha để tìm file .env
        while (current != null) {
            candidates.add(current.resolve(".env.local"));
            current = current.getParent();
        }
    }

    /**
     * Loại bỏ dấu ngoặc kép hoặc ngoặc đơn bao quanh giá trị trong file .env.
     */
    private static String stripQuotes(String value) {
        if (value.length() >= 2) {
            boolean wrappedInDoubleQuotes = value.startsWith("\"") && value.endsWith("\"");
            boolean wrappedInSingleQuotes = value.startsWith("'") && value.endsWith("'");
            if (wrappedInDoubleQuotes || wrappedInSingleQuotes) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }
}
