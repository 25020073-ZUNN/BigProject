package com.auction.service;

import com.auction.dao.UserDao;
import com.auction.model.user.User;

import java.util.Optional;

/**
 * AuthService - Dịch vụ xác thực người dùng.
 * Lớp này đóng vai trò trung gian giữa Network Server và lớp truy cập dữ liệu (UserDao).
 * Nó xử lý các nghiệp vụ liên quan đến Đăng nhập, Đăng ký và kiểm tra tính sẵn sàng của cơ sở dữ liệu.
 */
public class AuthService {
    // Áp dụng mẫu thiết kế Singleton để đảm bảo chỉ có một instance duy nhất của AuthService trong hệ thống
    private static final AuthService instance = new AuthService();

    private final UserDao userDao; // Đối tượng truy cập cơ sở dữ liệu cho người dùng

    /**
     * Constructor riêng tư (Private) để ngăn việc tạo instance từ bên ngoài (Singleton pattern).
     */
    private AuthService() {
        this.userDao = new UserDao();
    }

    /**
     * Phương thức tĩnh để lấy instance duy nhất của AuthService.
     */
    public static AuthService getInstance() {
        return instance;
    }

    /**
     * Xử lý nghiệp vụ đăng nhập.
     * @param username Tên đăng nhập
     * @param password Mật khẩu
     * @return Một Optional chứa thông tin User nếu thành công, hoặc rỗng nếu thất bại.
     */
    public Optional<User> login(String username, String password) {
        // Kiểm tra dữ liệu đầu vào cơ bản
        if (username == null || password == null) {
            return Optional.empty();
        }

        // Đảm bảo Database đang hoạt động trước khi thực hiện truy vấn
        ensureDatabaseAvailable();

        // Gọi UserDao để kiểm tra thông tin đăng nhập trong cơ sở dữ liệu
        User user = userDao.login(username, password);
        if (user != null) {
            return Optional.of(user);
        }

        return Optional.empty();
    }

    /**
     * Xử lý nghiệp vụ đăng ký người dùng mới.
     * @param user Đối tượng người dùng chứa thông tin đăng ký
     * @return true nếu đăng ký thành công, false nếu thất bại.
     */
    public boolean register(User user) {
        if (user == null) {
            return false;
        }

        // Đảm bảo Database đang hoạt động trước khi thực hiện ghi dữ liệu
        ensureDatabaseAvailable();
        
        // Gọi UserDao để lưu thông tin người dùng vào cơ sở dữ liệu
        return userDao.register(user);
    }

    /**
     * Kiểm tra xem cơ sở dữ liệu có đang sẵn sàng để kết nối hay không.
     * Phương thức này thường được gọi từ Server khi nhận yêu cầu DB_STATUS từ Client.
     */
    public boolean isDatabaseAvailable() {
        return userDao.isDatabaseAvailable();
    }

    /**
     * Kiểm tra nội bộ trạng thái Database.
     * Nếu không thể kết nối, ném ra một ngoại lệ để ngăn chặn các thao tác tiếp theo.
     */
    private void ensureDatabaseAvailable() {
        if (!userDao.isDatabaseAvailable()) {
            throw new IllegalStateException("Cơ sở dữ liệu hiện không khả dụng. Vui lòng kiểm tra lại cấu hình DB_URL, DB_USER, DB_PASSWORD.");
        }
    }
}
