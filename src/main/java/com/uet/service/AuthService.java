package com.uet.service;

import com.uet.bidding.model.user.Bidder;
import com.uet.bidding.model.user.User;

import java.util.Optional;

/**
 * AuthService handles authentication logic.
 * Currently uses mock logic with a hardcoded user.
 */
public class AuthService {

    private static final String MOCK_USERNAME = "admin";
    private static final String MOCK_PASSWORD = "password123";

    // Mock user for testing
    private final User mockUser;

    public AuthService() {
        // In this mock implementation, we hash the password as the User constructor/verifyPassword logic expects.
        // Assuming User.hashCode() is used for simple hashing as seen in User.verifyPassword.
        this.mockUser = new Bidder(MOCK_USERNAME, "admin@example.com", String.valueOf(MOCK_PASSWORD.hashCode()));
    }

    /**
     * Authenticates a user based on username and password.
     *
     * @param username The username provided
     * @param password The raw password provided
     * @return An Optional containing the User if successful, or empty otherwise
     */
    public Optional<User> login(String username, String password) {
        if (username == null || password == null) {
            return Optional.empty();
        }

        if (MOCK_USERNAME.equals(username) && mockUser.verifyPassword(password)) {
            return Optional.of(mockUser);
        }

        return Optional.empty();
    }
}
