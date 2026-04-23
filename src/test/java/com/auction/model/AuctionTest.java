package com.auction.model;

import com.auction.model.item.Item;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;
import com.auction.factory.ItemFactory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuctionTest {

    @Test
    void placeBidUpdatesCurrentPriceAndHighestBidder() {
        Seller seller = new Seller("seller", "seller@example.com", String.valueOf("password".hashCode()));
        Bidder bidder = new Bidder("bidder", "bidder@example.com", String.valueOf("password".hashCode()));
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

        Auction auction = new Auction(item, seller, item.getCurrentPrice());
        auction.placeBid(bidder, new BigDecimal("1500"));

        assertEquals(new BigDecimal("1500"), auction.getCurrentPrice());
        assertEquals(bidder, auction.getHighestBidder());
        assertEquals(1, auction.getBidHistory().size());
    }

    @Test
    void placeBidRejectsAmountNotHigherThanCurrentPrice() {
        Seller seller = new Seller("seller", "seller@example.com", String.valueOf("password".hashCode()));
        Bidder bidder = new Bidder("bidder", "bidder@example.com", String.valueOf("password".hashCode()));
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

        Auction auction = new Auction(item, seller, item.getCurrentPrice());

        assertThrows(IllegalArgumentException.class, () -> auction.placeBid(bidder, new BigDecimal("1000")));
    }

    @Test
    void closeAuctionMarksAuctionFinished() {
        Seller seller = new Seller("seller", "seller@example.com", String.valueOf("password".hashCode()));
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

        Auction auction = new Auction(item, seller, item.getCurrentPrice());
        auction.closeAuction();

        assertTrue(auction.isFinished());
        assertTrue(!auction.isActive());
    }
}
