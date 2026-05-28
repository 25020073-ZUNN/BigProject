package com.auction.model.user;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    // Tạo một lớp con cụ thể của User để thực hiện kiểm thử vì User là lớp trừu tượng (abstract)
    private static class ConcreteUser extends User {
        public ConcreteUser(String username, String email, String passwordHash) {
            super(username, email, passwordHash);
        }

        @Override
        public String getRole() {
            return "CONCRETE_USER";
        }
    }

    @Test
    void depositIncreasesBalanceCorrectly() {
        User user = new ConcreteUser("testuser", "test@example.com", "hash");
        assertEquals(0L, user.getBalance());

        user.deposit(5000L);
        assertEquals(5000L, user.getBalance());

        user.deposit(10000L);
        assertEquals(15000L, user.getBalance());
    }

    @Test
    void depositRejectsZeroOrNegativeAmounts() {
        User user = new ConcreteUser("testuser", "test@example.com", "hash");

        assertThrows(IllegalArgumentException.class, () -> user.deposit(0L));
        assertThrows(IllegalArgumentException.class, () -> user.deposit(-500L));
    }

    @Test
    void withdrawDecreasesBalanceCorrectly() {
        User user = new ConcreteUser("testuser", "test@example.com", "hash");
        user.deposit(10000L);

        user.withdraw(4000L);
        assertEquals(6000L, user.getBalance());

        user.withdraw(6000L);
        assertEquals(0L, user.getBalance());
    }

    @Test
    void withdrawRejectsZeroOrNegativeAmounts() {
        User user = new ConcreteUser("testuser", "test@example.com", "hash");
        user.deposit(5000L);

        assertThrows(IllegalArgumentException.class, () -> user.withdraw(0L));
        assertThrows(IllegalArgumentException.class, () -> user.withdraw(-100L));
    }

    @Test
    void withdrawThrowsExceptionOnInsufficientFunds() {
        User user = new ConcreteUser("testuser", "test@example.com", "hash");
        user.deposit(5000L);

        assertThrows(InsufficientFundsException.class, () -> user.withdraw(5001L));
    }

    @Test
    void verifyPasswordChecksHashCodeCorrectly() {
        String password = "mySecurePassword";
        String passwordHash = String.valueOf(password.hashCode());
        User user = new ConcreteUser("testuser", "test@example.com", passwordHash);

        assertTrue(user.verifyPassword(password));
        assertFalse(user.verifyPassword("wrongPassword"));
    }

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

    @Test
    void addRatingRejectsOutOfBoundsRatings() {
        User user = new ConcreteUser("testuser", "test@example.com", "hash");

        assertThrows(IllegalArgumentException.class, () -> user.addRating(-1.0));
        assertThrows(IllegalArgumentException.class, () -> user.addRating(5.1));
    }

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
}
