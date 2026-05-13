package com.auction.service;

import java.math.BigDecimal;

public class AutoBidStrategy {

    public AutoBidDecision decide(BigDecimal currentPrice,
                                  BigDecimal minimumIncrement,
                                  BigDecimal configuredStep,
                                  BigDecimal maximumAmount) {
        if (currentPrice == null || minimumIncrement == null || configuredStep == null || maximumAmount == null) {
            return AutoBidDecision.stop("Auto-bid chưa có đủ dữ liệu để hoạt động.");
        }

        BigDecimal effectiveStep = configuredStep.max(minimumIncrement);
        BigDecimal minimumAllowed = currentPrice.add(minimumIncrement);
        BigDecimal preferredBid = currentPrice.add(effectiveStep);
        BigDecimal candidateBid = preferredBid.min(maximumAmount);

        if (candidateBid.compareTo(minimumAllowed) < 0) {
            return AutoBidDecision.stop("Auto-bid đã dừng vì giá hợp lệ tiếp theo vượt quá mức tối đa.");
        }

        return AutoBidDecision.place(candidateBid, candidateBid.compareTo(maximumAmount) == 0
                && preferredBid.compareTo(maximumAmount) > 0);
    }

    public static final class AutoBidDecision {
        private final boolean shouldBid;
        private final BigDecimal bidAmount;
        private final String stopReason;
        private final boolean usedMaximum;

        private AutoBidDecision(boolean shouldBid, BigDecimal bidAmount, String stopReason, boolean usedMaximum) {
            this.shouldBid = shouldBid;
            this.bidAmount = bidAmount;
            this.stopReason = stopReason;
            this.usedMaximum = usedMaximum;
        }

        public static AutoBidDecision place(BigDecimal bidAmount, boolean usedMaximum) {
            return new AutoBidDecision(true, bidAmount, null, usedMaximum);
        }

        public static AutoBidDecision stop(String stopReason) {
            return new AutoBidDecision(false, null, stopReason, false);
        }

        public boolean shouldBid() {
            return shouldBid;
        }

        public BigDecimal bidAmount() {
            return bidAmount;
        }

        public String stopReason() {
            return stopReason;
        }

        public boolean usedMaximum() {
            return usedMaximum;
        }
    }
}
