package com.auction.dao;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuctionDaoTest {

    @Test
    void resolveEffectiveEndTimeExtendsAuctionWhenBidArrivesInLastTwoMinutes() {
        LocalDateTime originalEndTime = LocalDateTime.now().plusSeconds(90);

        LocalDateTime effectiveEndTime = AuctionDao.resolveEffectiveEndTime(originalEndTime);

        assertEquals(originalEndTime.plusMinutes(2), effectiveEndTime);
    }

    @Test
    void resolveEffectiveEndTimeKeepsDeadlineWhenAuctionIsNotNearEnding() {
        LocalDateTime originalEndTime = LocalDateTime.now().plusMinutes(5);

        LocalDateTime effectiveEndTime = AuctionDao.resolveEffectiveEndTime(originalEndTime);

        assertEquals(originalEndTime, effectiveEndTime);
    }
}
