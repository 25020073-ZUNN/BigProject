package com.auction.service;

import com.auction.model.user.Admin;
import com.auction.model.user.Bidder;
import com.auction.model.user.User;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * UserService
 *
 * Chức năng:
 * - Quản lý danh sách người dùng
 * - Đăng nhập
 * - Đăng ký
 * - Tìm kiếm người dùng
 * - Trả về danh sách User
 *
 * Đây là tầng Service trong mô hình:
 *
 * Controller
 * ↓
 * UserService
 * ↓
 * UserDao / Database
 */
public class UserService {

    /**
     * Danh sách User hiện tại.
     *
     * Dùng CopyOnWriteArrayList để:
     * - Thread-safe
     * - Nhiều luồng truy cập an toàn
     */
    private final List<User> users =
            new CopyOnWriteArrayList<>();

    /**
     * Singleton Instance
     *
     * Toàn bộ hệ thống chỉ có
     * một UserService duy nhất.
     */
    private static final UserService instance =
            new UserService();

    /**
     * Trả về instance duy nhất.
     */
    public static UserService getInstance() {
        return instance;
    }

    /**
     * Constructor private.
     *
     * Không cho:
     * new UserService()
     *
     * Đảm bảo Singleton Pattern.
     */
    private UserService() {
        initializeMockData();
    }

    /**
     * Khởi tạo dữ liệu mẫu.
     *
     * Dùng cho:
     * - Demo
     * - Test
     * - Chạy khi chưa kết nối DB
     */
    private void initializeMockData() {

        // Hash mật khẩu mẫu
        String commonPasswordHash =
                String.valueOf("password".hashCode());

        // Tạo các Bidder mẫu
        users.add(
                new Bidder(
                        "user1",
                        "user1@example.com",
                        commonPasswordHash
                )
        );

        users.add(
                new Bidder(
                        "user2",
                        "user2@example.com",
                        commonPasswordHash
                )
        );

        users.add(
                new Bidder(
                        "user3",
                        "user3@example.com",
                        commonPasswordHash
                )
        );

        // Tạo Admin mẫu
        users.add(
                new Admin(
                        "admin",
                        "admin@auction.com",
                        commonPasswordHash,
                        "SUPER_ADMIN"
                )
        );
    }

    /**
     * Đăng nhập hệ thống.
     *
     * Điều kiện:
     * - Username đúng
     * - Password đúng
     * - User đang active
     */
    public Optional<User> login(
            String username,
            String password
    ) {

        if (username == null || password == null)
            return Optional.empty();

        return users.stream()

                // Tìm username
                .filter(u ->
                        u.getUsername()
                                .equalsIgnoreCase(username))

                // Kiểm tra password
                .filter(u ->
                        u.verifyPassword(password))

                // Chỉ cho user active
                .filter(User::isActive)

                // Lấy user đầu tiên
                .findFirst();
    }

    /**
     * Đăng ký người dùng mới.
     *
     * Điều kiện:
     * - Không null
     * - Username không trùng
     * - Email không trùng
     */
    public boolean register(User user) {

        if (user == null)
            return false;

        if (isBlank(user.getUsername())
                || isBlank(user.getEmail()))
            return false;

        boolean exists =
                users.stream().anyMatch(u ->

                        u.getUsername()
                                .equalsIgnoreCase(
                                        user.getUsername()
                                )

                                ||

                                u.getEmail()
                                        .equalsIgnoreCase(
                                                user.getEmail()
                                        )
                );

        // Nếu đã tồn tại
        if (exists)
            return false;

        // Thêm user mới
        return users.add(user);
    }

    /**
     * Tìm User theo ID.
     */
    public Optional<User> getUserById(String id) {

        if (id == null)
            return Optional.empty();

        return users.stream()
                .filter(u -> u.getId().equals(id))
                .findFirst();
    }

    /**
     * Tìm User theo Username.
     */
    public Optional<User> getUserByUsername(
            String username
    ) {

        if (username == null)
            return Optional.empty();

        return users.stream()
                .filter(u ->
                        u.getUsername()
                                .equalsIgnoreCase(username))
                .findFirst();
    }

    /**
     * Trả về toàn bộ User.
     *
     * Trả bản sao để tránh sửa
     * trực tiếp danh sách gốc.
     */
    public List<User> getAllUsers() {
        return new ArrayList<>(users);
    }

    /**
     * Kiểm tra chuỗi rỗng.
     */
    private boolean isBlank(String str) {
        return str == null
                || str.trim().isEmpty();
    }
}