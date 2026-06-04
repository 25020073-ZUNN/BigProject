package com.auction.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Lớp kiểm thử đơn vị cho ValidationUtil.
 * Kiểm tra các tính năng kiểm thử tính hợp lệ của dữ liệu đầu vào (mật khẩu, tên tài khoản, địa chỉ email)
 * thông qua các biểu thức chính quy (Regex) và quy chuẩn bảo mật/kỹ thuật của hệ thống.
 */
class ValidationUtilTest {

    /**
     * Kiểm thử trường hợp: Mật khẩu hợp lệ.
     * Đảm bảo mật khẩu đáp ứng đủ tiêu chuẩn tối thiểu (8 ký tự, có chữ hoa, chữ thường và chữ số).
     */
    @Test
    void isPasswordValidAcceptsCorrectPasswords() {
        assertTrue(ValidationUtil.isPasswordValid("StrongPass123"));
        assertTrue(ValidationUtil.isPasswordValid("aB1cDefGhi"));
    }

    /**
     * Kiểm thử trường hợp: Mật khẩu quá ngắn.
     * Mật khẩu có độ dài dưới 8 ký tự hoặc rỗng/null phải bị từ chối.
     */
    @Test
    void isPasswordValidRejectsShortPasswords() {
        assertFalse(ValidationUtil.isPasswordValid("Str1"));
        assertFalse(ValidationUtil.isPasswordValid(""));
        assertFalse(ValidationUtil.isPasswordValid(null));
    }

    /**
     * Kiểm thử trường hợp: Mật khẩu thiếu các nhóm ký tự bắt buộc.
     * Mật khẩu thiếu ít nhất một trong các nhóm: chữ hoa, chữ thường, chữ số phải bị từ chối.
     */
    @Test
    void isPasswordValidRejectsPasswordsMissingRequiredCharacters() {
        // Thiếu chữ hoa
        assertFalse(ValidationUtil.isPasswordValid("strongpass123"));
        // Thiếu chữ thường
        assertFalse(ValidationUtil.isPasswordValid("STRONGPASS123"));
        // Thiếu chữ số
        assertFalse(ValidationUtil.isPasswordValid("StrongPassWord"));
    }

    /**
     * Kiểm thử trường hợp: Tên tài khoản (username) hợp lệ.
     * Chấp nhận tên tài khoản chỉ gồm chữ cái và chữ số không dấu nằm trong khoảng từ 3 đến 16 ký tự.
     */
    @Test
    void isUsernameValidAcceptsAlphanumericWithinRange() {
        assertTrue(ValidationUtil.isUsernameValid("user123"));
        assertTrue(ValidationUtil.isUsernameValid("admin"));
        assertTrue(ValidationUtil.isUsernameValid("A1B2C3d4"));
    }

    /**
     * Kiểm thử trường hợp: Tên tài khoản có độ dài không hợp lệ (quá ngắn < 3 hoặc quá dài > 16 hoặc rỗng).
     */
    @Test
    void isUsernameValidRejectsInvalidLength() {
        // Quá ngắn (< 3)
        assertFalse(ValidationUtil.isUsernameValid("us"));
        // Quá dài (> 16)
        assertFalse(ValidationUtil.isUsernameValid("verylongusername123"));
        // Rỗng hoặc null
        assertFalse(ValidationUtil.isUsernameValid(""));
        assertFalse(ValidationUtil.isUsernameValid(null));
    }

    /**
     * Kiểm thử trường hợp: Tên tài khoản chứa các ký tự không được phép (khoảng trắng hoặc ký tự đặc biệt).
     */
    @Test
    void isUsernameValidRejectsNonAlphanumeric() {
        // Chứa khoảng trắng
        assertFalse(ValidationUtil.isUsernameValid("user name"));
        // Chứa ký tự đặc biệt
        assertFalse(ValidationUtil.isUsernameValid("user_123"));
        assertFalse(ValidationUtil.isUsernameValid("user!"));
    }

    /**
     * Kiểm thử trường hợp: Địa chỉ email đúng định dạng tiêu chuẩn.
     */
    @Test
    void isEmailValidAcceptsCorrectFormat() {
        assertTrue(ValidationUtil.isEmailValid("test@example.com"));
        assertTrue(ValidationUtil.isEmailValid("user.name+tag@sub.domain.org"));
    }

    /**
     * Kiểm thử trường hợp: Địa chỉ email sai định dạng (thiếu ký tự @, thiếu tên miền hoặc rỗng).
     */
    @Test
    void isEmailValidRejectsIncorrectFormat() {
        assertFalse(ValidationUtil.isEmailValid("missingat.com"));
        assertFalse(ValidationUtil.isEmailValid("missingdomain@"));
        assertFalse(ValidationUtil.isEmailValid("@missinguser.com"));
        assertFalse(ValidationUtil.isEmailValid(""));
        assertFalse(ValidationUtil.isEmailValid(null));
    }

    /**
     * Kiểm thử trường hợp: Tên tài khoản chứa ký tự Tiếng Việt có dấu hoặc ký tự Unicode (không được phép).
     */
    @Test
    void isUsernameValidRejectsVietnameseAccentAndUnicode() {
        assertFalse(ValidationUtil.isUsernameValid("nguyễn"));
        assertFalse(ValidationUtil.isUsernameValid("trần123"));
        assertFalse(ValidationUtil.isUsernameValid("userđẹp"));
    }
}

