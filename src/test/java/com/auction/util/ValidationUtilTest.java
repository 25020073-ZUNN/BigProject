package com.auction.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidationUtilTest {

    @Test
    void isPasswordValidAcceptsCorrectPasswords() {
        // Có ít nhất 8 ký tự, chữ hoa, chữ thường và chữ số
        assertTrue(ValidationUtil.isPasswordValid("StrongPass123"));
        assertTrue(ValidationUtil.isPasswordValid("aB1cDefGhi"));
    }

    @Test
    void isPasswordValidRejectsShortPasswords() {
        // Ngắn hơn 8 ký tự
        assertFalse(ValidationUtil.isPasswordValid("Str1"));
        assertFalse(ValidationUtil.isPasswordValid(""));
        assertFalse(ValidationUtil.isPasswordValid(null));
    }

    @Test
    void isPasswordValidRejectsPasswordsMissingRequiredCharacters() {
        // Thiếu chữ hoa
        assertFalse(ValidationUtil.isPasswordValid("strongpass123"));
        // Thiếu chữ thường
        assertFalse(ValidationUtil.isPasswordValid("STRONGPASS123"));
        // Thiếu chữ số
        assertFalse(ValidationUtil.isPasswordValid("StrongPassWord"));
    }

    @Test
    void isUsernameValidAcceptsAlphanumericWithinRange() {
        assertTrue(ValidationUtil.isUsernameValid("user123"));
        assertTrue(ValidationUtil.isUsernameValid("admin"));
        assertTrue(ValidationUtil.isUsernameValid("A1B2C3d4"));
    }

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

    @Test
    void isUsernameValidRejectsNonAlphanumeric() {
        // Chứa khoảng trắng
        assertFalse(ValidationUtil.isUsernameValid("user name"));
        // Chứa ký tự đặc biệt
        assertFalse(ValidationUtil.isUsernameValid("user_123"));
        assertFalse(ValidationUtil.isUsernameValid("user!"));
    }

    @Test
    void isEmailValidAcceptsCorrectFormat() {
        assertTrue(ValidationUtil.isEmailValid("test@example.com"));
        assertTrue(ValidationUtil.isEmailValid("user.name+tag@sub.domain.org"));
    }

    @Test
    void isEmailValidRejectsIncorrectFormat() {
        assertFalse(ValidationUtil.isEmailValid("missingat.com"));
        assertFalse(ValidationUtil.isEmailValid("missingdomain@"));
        assertFalse(ValidationUtil.isEmailValid("@missinguser.com"));
        assertFalse(ValidationUtil.isEmailValid(""));
        assertFalse(ValidationUtil.isEmailValid(null));
    }

    @Test
    void isUsernameValidRejectsVietnameseAccentAndUnicode() {
        assertFalse(ValidationUtil.isUsernameValid("nguyễn"));
        assertFalse(ValidationUtil.isUsernameValid("trần123"));
        assertFalse(ValidationUtil.isUsernameValid("userđẹp"));
    }
}
