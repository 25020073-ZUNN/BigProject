package com.auction.dao;

import com.auction.config.DBConnection;
import com.auction.model.user.Admin;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;
import com.auction.model.user.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Lớp UserDao: Thực hiện các lệnh SQL (CRUD) trên bảng 'users'.
 * Đây là nơi Java giao tiếp trực tiếp với MySQL.
 */
public class UserDao {

    /**
     * Kiểm tra sự tồn tại của username bằng câu lệnh SELECT.
     */
    public boolean usernameExists(String username) {
        String sql = "SELECT 1 FROM users WHERE username = ?";
        
        // Sử dụng try-with-resources để tự động đóng Connection và PreparedStatement
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            // Truyền tham số vào dấu ? để chống SQL Injection
            stmt.setString(1, username);
            
            // Thực thi truy vấn và nhận về tập kết quả ResultSet
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next(); // Nếu rs.next() là true nghĩa là có ít nhất 1 dòng kết quả
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Kiểm tra sự tồn tại của email.
     */
    public boolean emailExists(String email) {
        String sql = "SELECT 1 FROM users WHERE email = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Đăng ký người dùng mới bằng lệnh INSERT.
     */
    public boolean register(String username, String password, String fullName, String email) {
        return insertUser(
                UUID.randomUUID().toString(),
                username,
                fullName,
                email,
                hashPassword(password),
                "USER",
                0L,
                true
        );
    }

    /**
     * Đăng ký từ đối tượng User.
     */
    public boolean register(User user) {
        if (user == null) return false;
        return insertUser(
                user.getId(),
                user.getUsername(),
                user.getFullname(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getRole(),
                user.getBalance(),
                user.isActive()
        );
    }

    /**
     * Xác thực đăng nhập bằng lệnh SELECT với cả username và password.
     */
    public User login(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ? AND active = TRUE";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, username);
            stmt.setString(2, hashPassword(password));
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) return null; // Không tìm thấy hoặc sai thông tin
                return mapUser(rs); // Chuyển dòng dữ liệu hiện tại thành đối tượng User
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Tìm người dùng theo ID.
     * Hàm này rất quan trọng cho bài toán đấu giá thật vì dữ liệu phiên đấu giá
     * chỉ lưu khóa ngoại `seller_id` hoặc `highest_bidder_id`.
     * Khi dựng lại đối tượng Auction từ DB, chúng ta cần truy ngược từ ID sang User.
     */
    public User findById(String id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return mapUser(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Tìm người dùng theo username.
     * Form tạo tài sản dùng hàm này để xác thực người bán nhập vào có thực sự tồn tại hay không.
     */
    public User findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return mapUser(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Lấy danh sách người bán đang hoạt động.
     * Danh sách này hữu ích khi cần đổ vào ComboBox hoặc hỗ trợ người kiểm thử chọn seller hợp lệ.
     */
    public List<User> findActiveSellers() {
        String sql = "SELECT * FROM users WHERE role = 'SELLER' AND active = TRUE ORDER BY username";
        List<User> sellers = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                sellers.add(mapUser(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return sellers;
    }

    /**
     * Kiểm tra xem Database có đang "sống" hay không.
     */
    public boolean isDatabaseAvailable() {
        try (Connection ignored = DBConnection.getConnection()) {
            return true; // Kết nối thành công
        } catch (SQLException e) {
            return false; // Không thể kết nối (DB tắt, sai pass, v.v.)
        }
    }

    /**
     * Thực thi lệnh INSERT để thêm mới một dòng vào bảng users.
     */
    private boolean insertUser(String id, String username, String fullName, String email, 
                               String passwordHash, String role, long balance, boolean active) {
        if (isBlank(username) || isBlank(passwordHash) || isBlank(fullName) || isBlank(email)) return false;
        
        String sql = "INSERT INTO users(id, username, full_name, email, password, role, balance, active) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            // Gán các giá trị tương ứng cho từng dấu ?
            stmt.setString(1, isBlank(id) ? UUID.randomUUID().toString() : id);
            stmt.setString(2, username);
            stmt.setString(3, fullName);
            stmt.setString(4, email);
            stmt.setString(5, passwordHash);
            stmt.setString(6, role.toUpperCase(Locale.ROOT));
            stmt.setLong(7, balance);
            stmt.setBoolean(8, active);
            
            // executeUpdate() dùng cho các lệnh INSERT, UPDATE, DELETE. Trả về số dòng bị ảnh hưởng.
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Hàm ánh xạ (Mapping) từ dữ liệu dòng trong SQL sang đối tượng Java.
     */
    private User mapUser(ResultSet rs) throws SQLException {
        User user = mapRowToUser(
                rs.getString("username"),
                rs.getString("full_name"),
                rs.getString("email"),
                rs.getString("password"),
                rs.getString("role"),
                rs.getLong("balance"),
                rs.getBoolean("active")
        );
        user.setId(rs.getString("id"));
        return user;
    }

    User mapRowToUser(String username, String fullName, String email, String passwordHash,
                      String role, long balance, boolean active) {
        String normalizedRole = role.toUpperCase(Locale.ROOT);

        User user;
        if ("ADMIN".equals(normalizedRole)) {
            user = new Admin(username, email, passwordHash, "SYSTEM_ADMIN");
        } else if ("SELLER".equals(normalizedRole)) {
            user = new Seller(username, email, passwordHash);
        } else {
            user = new Bidder(username, email, passwordHash);
        }

        user.setFullname(fullName);
        user.setActive(active);
        if (balance > 0) user.deposit(balance);
        return user;
    }

    private String hashPassword(String password) {
        return (password == null) ? null : String.valueOf(password.hashCode());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
