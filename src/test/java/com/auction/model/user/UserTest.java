package com.auction.model.user;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lớp kiểm thử đơn vị cho mô hình User.
 * Kiểm tra các nghiệp vụ quản lý ví tiền (nạp/rút), xác thực mật khẩu,
 * đánh giá chất lượng (rating), vai trò kế thừa (Admin, Seller, Bidder) và các biên tràn số.
 */
class UserTest {

    // Lớp con cụ thể kế thừa User để thực hiện kiểm thử vì User là lớp trừu tượng (abstract)
    private static class ConcreteUser extends User {
        public ConcreteUser(String username, String email, String passwordHash) {
            super(username, email, passwordHash);
        }

        @Override
        public String getRole() {
            return "CONCRETE_USER";
        }
    }

    /**
     * Kiểm thử trường hợp: Nạp tiền (deposit) làm tăng số dư tài khoản tương ứng.
     */
    @Test
    void depositIncreasesBalanceCorrectly() {
        User user = new ConcreteUser("testuser", "test@example.com", "hash");
        assertEquals(0L, user.getBalance());

        user.deposit(5000L);
        assertEquals(5000L, user.getBalance());

        user.deposit(10000L);
        assertEquals(15000L, user.getBalance());
    }

    /**
     * Kiểm thử trường hợp: Từ chối các lệnh nạp tiền có giá trị <= 0.
     */
    @Test
    void depositRejectsZeroOrNegativeAmounts() {
        User user = new ConcreteUser("testuser", "test@example.com", "hash");

        assertThrows(IllegalArgumentException.class, () -> user.deposit(0L));
        assertThrows(IllegalArgumentException.class, () -> user.deposit(-500L));
    }

    /**
     * Kiểm thử trường hợp: Rút tiền (withdraw) giảm số dư ví chính xác.
     */
    @Test
    void withdrawDecreasesBalanceCorrectly() {
        User user = new ConcreteUser("testuser", "test@example.com", "hash");
        user.deposit(10000L);

        user.withdraw(4000L);
        assertEquals(6000L, user.getBalance());

        user.withdraw(6000L);
        assertEquals(0L, user.getBalance());
    }

    /**
     * Kiểm thử trường hợp: Từ chối các lệnh rút tiền <= 0.
     */
    @Test
    void withdrawRejectsZeroOrNegativeAmounts() {
        User user = new ConcreteUser("testuser", "test@example.com", "hash");
        user.deposit(5000L);

        assertThrows(IllegalArgumentException.class, () -> user.withdraw(0L));
        assertThrows(IllegalArgumentException.class, () -> user.withdraw(-100L));
    }

    /**
     * Kiểm thử trường hợp: Ném ngoại lệ InsufficientFundsException
     * khi số tiền rút vượt quá số dư hiện tại trong ví.
     */
    @Test
    void withdrawThrowsExceptionOnInsufficientFunds() {
        User user = new ConcreteUser("testuser", "test@example.com", "hash");
        user.deposit(5000L);

        assertThrows(InsufficientFundsException.class, () -> user.withdraw(5001L));
    }

    /**
     * Kiểm thử trường hợp: Xác thực mật khẩu thành công bằng cách
     * so khớp mã băm (hashCode) trong mô hình test đơn giản.
     */
    @Test
    void verifyPasswordChecksHashCodeCorrectly() {
        String password = "mySecurePassword";
        String passwordHash = String.valueOf(password.hashCode());
        User user = new ConcreteUser("testuser", "test@example.com", passwordHash);

        assertTrue(user.verifyPassword(password));
        assertFalse(user.verifyPassword("wrongPassword"));
    }

    /**
     * Kiểm thử trường hợp: Đánh giá chất lượng (Rating)
     * tự động tính toán lại điểm trung bình cộng lũy tiến qua các lượt.
     */
    @Test
    void addRatingCalculatesRunningAverageCorrectly() {
        User user = new ConcreteUser("testuser", "test@example.com", "hash");
        assertEquals(0.0, user.getRating());

        user.addRating(4.0);
        assertEquals(4.0, user.getRating());

        user.addRating(5.0);
        assertEquals(4.5, user.getRating()); // (4.0 + 5.0) / 2

        user.addRating(3.0);
        assertEquals(4.0, user.getRating()); // (4.5 * 2 + 3.0) / 3
    }

    /**
     * Kiểm thử trường hợp: Từ chối các lượt đánh giá ngoài phạm vi từ 0 đến 5 điểm.
     */
    @Test
    void addRatingRejectsOutOfBoundsRatings() {
        User user = new ConcreteUser("testuser", "test@example.com", "hash");

        assertThrows(IllegalArgumentException.class, () -> user.addRating(-1.0));
        assertThrows(IllegalArgumentException.class, () -> user.addRating(5.1));
    }

    /**
     * Kiểm thử trường hợp: Các lớp con cụ thể (Admin, Bidder, Seller, RegisteredUser)
     * trả về đúng nhãn Role nghiệp vụ của chúng.
     */
    @Test
    void testSubclassRolesReturnCorrectValues() {
        User admin = new Admin("admin", "admin@example.com", "hash", "SUPER");
        User bidder = new Bidder("bidder", "bidder@example.com", "hash");
        User seller = new Seller("seller", "seller@example.com", "hash");
        User regUser = new RegisteredUser("reg", "reg@example.com", "hash");

        assertEquals("ADMIN", admin.getRole());
        assertEquals("BIDDER", bidder.getRole());
        assertEquals("SELLER", seller.getRole());
        assertEquals("USER", regUser.getRole());
    }

    /**
     * Kiểm thử trường hợp: Phát hiện biên lỗi tràn số ví tiền lên mức âm
     * khi nạp vượt mức giới hạn Long.MAX_VALUE.
     */
    @Test
    void depositOverflowsBalanceToNegative() {
        User user = new ConcreteUser("testuser", "test@example.com", "hash");
        user.deposit(Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, user.getBalance());

        // Lỗi tràn số (overflow) biến số dư thành âm
        user.deposit(1L);
        assertTrue(user.getBalance() < 0);
        assertEquals(Long.MIN_VALUE, user.getBalance());
    }

    /**
     * Kiểm thử trường hợp: Từ chối thiết lập email nếu giá trị email bị null.
     */
    @Test
    void setEmailThrowsIllegalArgumentExceptionOnNull() {
        User user = new ConcreteUser("testuser", "test@example.com", "hash");
        assertThrows(IllegalArgumentException.class, () -> user.setEmail(null));
    }
}

