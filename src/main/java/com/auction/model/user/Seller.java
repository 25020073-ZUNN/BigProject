package com.auction.model.user;

/**
 * @deprecated Use {@link User} instead.
 */
@Deprecated
public class Seller extends User {
    public Seller(String username, String email, String passwordHash) {
        super(username, email, passwordHash);
    }
}
