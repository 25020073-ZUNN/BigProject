package com.auction.model.user;

/**
 * @deprecated Use {@link User} instead.
 */
@Deprecated
public class Bidder extends User {
    public Bidder(String username, String email, String passwordHash) {
        super(username, email, passwordHash);
    }
}
