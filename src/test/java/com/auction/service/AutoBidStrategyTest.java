package com.auction.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoBidStrategyTest {

    private final AutoBidStrategy strategy = new AutoBidStrategy();

    @Test
    void decideUsesConfiguredMaximumWhenPreferredStepOvershoots() {
        AutoBidStrategy.AutoBidDecision decision = strategy.decide(
                new BigDecimal("100"),
                new BigDecimal("10"),
                new BigDecimal("50"),
                new BigDecimal("120")
        );

        assertTrue(decision.shouldBid());
        assertTrue(decision.usedMaximum());
        assertEquals(new BigDecimal("120"), decision.bidAmount());
    }

    @Test
    void decideStopsWhenMaximumIsBelowNextValidBid() {
        AutoBidStrategy.AutoBidDecision decision = strategy.decide(
                new BigDecimal("100"),
                new BigDecimal("10"),
                new BigDecimal("50"),
                new BigDecimal("105")
        );

        assertFalse(decision.shouldBid());
        assertEquals("Auto-bid đã dừng vì giá hợp lệ tiếp theo vượt quá mức tối đa.", decision.stopReason());
    }
}
