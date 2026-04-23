package com.uet.bidding.Dao;
import com.uet.bidding.config.DBConnection;
import com.uet.bidding.model.user.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDao {

    /**
     * Lấy danh sách tất cả người dùng từ database
     */
    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error getting all users: " + e.getMessage());
            e.printStackTrace();
        }
        return users;
    }

    /**
     * Kiểm tra đăng nhập
     */
    public User login(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setString(2, password);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error during login: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Chuyển đổi ResultSet thành đối tượng User (Admin, Bidder, hoặc Seller)
     */
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        String username = rs.getString("username");
        String fullname = rs.getString("full_name");
        String email = rs.getString("email");
        String passwordHash = rs.getString("password");
        String role = rs.getString("role");
        long balance = rs.getLong("balance");
        String id = String.valueOf(rs.getObject("id")); // Lấy ID từ DB

        User user;
        // Khởi tạo subclass tương ứng với Role
        if ("ADMIN".equalsIgnoreCase(role)) {
            user = new Admin(username, email, passwordHash, "STANDARD");
        } else if ("SELLER".equalsIgnoreCase(role)) {
            user = new Seller(username, email, passwordHash);
        } else {
            user = new Bidder(username, email, passwordHash);
        }

        // Gán các thông tin bổ sung
        user.setId(id);
        user.setFullname(fullname);
        if (balance > 0) {
            user.deposit(balance);
        }

        return user;
    }

    /**
     * Hàm main để chạy thử nghiệm (Test)
     */
    public static void main(String[] args) {
        UserDao dao = new UserDao();

        System.out.println("=== TEST: GET ALL USERS ===");
        List<User> users = dao.getAllUsers();
        if (users.isEmpty()) {
            System.out.println("No users found (Database might be empty or connection failed).");
        } else {
            for (User u : users) {
                System.out.println("ID: " + u.getId() + " | " + u.getUsername() + " - " + u.getRole() + " (" + u.getFullname() + ")");
            }
        }

        System.out.println("\n=== TEST: LOGIN ===");
        // Thử login với tài khoản admin (giả sử đã có trong DB)
        User user = dao.login("admin", "123");
        if (user != null) {
            System.out.println("Login success!");
            user.printInfo();
        } else {
            System.out.println("Login failed: Username or password incorrect.");
        }
    }
}
