package com.auction.model;

import com.auction.model.item.Item;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;
import com.auction.model.user.User;
import com.auction.factory.ItemFactory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class BidTransactionTest {

    @Test
    void constructorAcceptsValidParametersAndSetsBidTime() {
        User seller = new Seller("seller", "seller@example.com", "hash");
        User bidder = new Bidder("bidder", "bidder@example.com", "hash");
        Item item = ItemFactory.createElectronics(
                "Phone",
                "New phone",
                new BigDecimal("1000"),
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusHours(1),
                seller.getId(),
                "Apple",
                12
        );
        Auction auction = new Auction(item, seller, new BigDecimal("1000"));

        BigDecimal bidAmount = new BigDecimal("1500");
        BidTransaction transaction = new BidTransaction(auction, bidder, bidAmount);

        assertEquals(auction, transaction.getAuction());
        assertEquals(bidder, transaction.getBidder());
        assertEquals(bidAmount, transaction.getBidAmount());
        assertNotNull(transaction.getBidTime());
    }

    @Test
    void constructorRejectsNullAuction() {
        User bidder = new Bidder("bidder", "bidder@example.com", "hash");
        assertThrows(IllegalArgumentException.class, () -> new BidTransaction(null, bidder, new BigDecimal("1500")));
    }

    @Test
    void constructorRejectsNullBidder() {
        User seller = new Seller("seller", "seller@example.com", "hash");
        Item item = ItemFactory.createElectronics(
                "Phone",
                "New phone",
                new BigDecimal("1000"),
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusHours(1),
                seller.getId(),
                "Apple",
                12
        );
        Auction auction = new Auction(item, seller, new BigDecimal("1000"));

        assertThrows(IllegalArgumentException.class, () -> new BidTransaction(auction, null, new BigDecimal("1500")));
    }

    @Test
    void constructorRejectsNullZeroOrNegativeBidAmount() {
        User seller = new Seller("seller", "seller@example.com", "hash");
        User bidder = new Bidder("bidder", "bidder@example.com", "hash");
        Item item = ItemFactory.createElectronics(
                "Phone",
                "New phone",
                new BigDecimal("1000"),
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusHours(1),
                seller.getId(),
                "Apple",
                12
        );
        Auction auction = new Auction(item, seller, new BigDecimal("1000"));

        assertThrows(IllegalArgumentException.class, () -> new BidTransaction(auction, bidder, null));
        assertThrows(IllegalArgumentException.class, () -> new BidTransaction(auction, bidder, BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> new BidTransaction(auction, bidder, new BigDecimal("-10")));
    }
}
