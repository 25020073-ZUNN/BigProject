package com.auction.util;

public class ValidationUtil {
    /**
     * Kiểm tra mật khẩu có hợp lệ không.
     * Yêu cầu: Ít nhất 8 ký tự, chứa chữ hoa, chữ thường và số.
     */
    public static boolean isPasswordValid(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }

        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;

        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c))
                hasUpper = true;
            else if (Character.isLowerCase(c))
                hasLower = true;
            else if (Character.isDigit(c))
                hasDigit = true;
        }

        return hasUpper && hasLower && hasDigit;
    }

    /**
     * Kiểm tra tên đăng nhập có hợp lệ không.
     * Yêu cầu: Chỉ chứa chữ cái và số, độ dài 3-16 ký tự.
     */
    public static boolean isUsernameValid(String username) {
        if (username == null || username.length() < 3 || username.length() > 16) {
            return false;
        }
        return username.matches("^[a-zA-Z0-9]+$");
    }

    /**
     * Kiểm tra email có hợp lệ không.
     */
    public static boolean isEmailValid(String email) {
        if (email == null)
            return false;
        // Regex đơn giản để check format email
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }
}
