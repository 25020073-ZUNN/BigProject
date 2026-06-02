package com.auction.util;

/**
 * ValidationUtil
 *
 * Lớp tiện ích dùng để kiểm tra dữ liệu đầu vào.
 *
 * Chức năng:
 * - Kiểm tra mật khẩu
 * - Kiểm tra username
 * - Kiểm tra email
 *
 * Mục đích:
 * - Ngăn dữ liệu sai đi vào hệ thống
 * - Tăng bảo mật
 * - Giảm lỗi khi lưu Database
 */
public class ValidationUtil {

    /**
     * Kiểm tra mật khẩu hợp lệ.
     *
     * Điều kiện:
     * - Không được null
     * - Ít nhất 8 ký tự
     * - Có chữ hoa
     * - Có chữ thường
     * - Có chữ số
     *
     * Ví dụ hợp lệ:
     * Abc12345
     *
     * Ví dụ không hợp lệ:
     * abc12345
     * ABC12345
     * Abcdefgh
     */
    public static boolean isPasswordValid(String password) {

        // Kiểm tra null hoặc quá ngắn
        if (password == null || password.length() < 8) {
            return false;
        }

        // Cờ kiểm tra các điều kiện
        boolean hasUpper = false; // có chữ hoa
        boolean hasLower = false; // có chữ thường
        boolean hasDigit = false; // có số

        // Duyệt từng ký tự trong mật khẩu
        for (char c : password.toCharArray()) {

            if (Character.isUpperCase(c))
                hasUpper = true;

            else if (Character.isLowerCase(c))
                hasLower = true;

            else if (Character.isDigit(c))
                hasDigit = true;
        }

        // Chỉ hợp lệ khi đủ cả 3 điều kiện
        return hasUpper && hasLower && hasDigit;
    }

    /**
     * Kiểm tra username hợp lệ.
     *
     * Điều kiện:
     * - Không null
     * - Độ dài từ 3 đến 16 ký tự
     * - Chỉ chứa chữ và số
     *
     * Hợp lệ:
     * thuyet123
     *
     * Không hợp lệ:
     * thuyet_123
     * thuyet@
     */
    public static boolean isUsernameValid(String username) {

        if (username == null
                || username.length() < 3
                || username.length() > 16) {
            return false;
        }

        /**
         * Regex:
         * ^           : bắt đầu chuỗi
         * [a-zA-Z0-9] : chỉ cho chữ và số
         * +           : 1 hoặc nhiều ký tự
         * $           : kết thúc chuỗi
         */
        return username.matches("^[a-zA-Z0-9]+$");
    }

    /**
     * Kiểm tra email hợp lệ.
     *
     * Ví dụ:
     * abc@gmail.com
     * user123@yahoo.com
     *
     * Không hợp lệ:
     * abcgmail.com
     * abc@
     */
    public static boolean isEmailValid(String email) {

        // Email không được null
        if (email == null)
            return false;

        /**
         * Regex kiểm tra format email.
         *
         * Cấu trúc:
         * username@domain
         */
        return email.matches(
                "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"
        );
    }
}
/*ValidationUtil là lớp tiện ích dùng để kiểm tra tính hợp lệ của dữ liệu đầu vào như username, password và email trước khi xử lý hoặc lưu vào database.
Việc kiểm tra sớm giúp tăng tính bảo mật, đảm bảo dữ liệu đúng định dạng và giảm lỗi trong hệ thống*/