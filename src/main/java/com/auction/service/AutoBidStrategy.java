package com.auction.service;

import java.math.BigDecimal;
/**
 * AutoBidStrategy
 * Chức năng:Tính toán mức giá tiếp theo khi Auto-Bid hoạt động.
 * Mục tiêu:
 * - Tự động tăng giá khi có người vượt giá hiện tại.
 * - Không vượt quá mức tối đa người dùng cấu hình.
 * - Tuân thủ bước giá tối thiểu của hệ thống.
 * Luồng:
 * Giá hiện tại
 * ↓
 * Tính giá tiếp theo
 * ↓
 * Kiểm tra mức tối đa
 * ↓
 * Đặt giá hoặc dừng Auto-Bid
 */
public class AutoBidStrategy {

    public AutoBidDecision decide(BigDecimal currentPrice,
                                  BigDecimal minimumIncrement,
                                  BigDecimal configuredStep,
                                  BigDecimal maximumAmount) {
        if (currentPrice == null || minimumIncrement == null || configuredStep == null || maximumAmount == null) {
            return AutoBidDecision.stop("Auto-bid chưa có đủ dữ liệu để hoạt động.");
        }

        /**
         * Bước giá thực tế.
         * Luôn lấy giá trị lớn hơn giữa:Bước giá người dùng và Bước giá tối thiểu hệ thống
         *
         * Ví dụ:
         * User: 100.000
         * System: 500.000
         * => dùng 500.000
         */
        BigDecimal effectiveStep = configuredStep.max(minimumIncrement);
        //Giá thấp nhất hợp lệ tiếp theo.
        BigDecimal minimumAllowed = currentPrice.add(minimumIncrement);
        //Giá Auto-Bid muốn đặt=Giá hiện tại + Bước giá thực tế
        BigDecimal preferredBid = currentPrice.add(effectiveStep);
        //Giá cuối cùng dự kiến đặt:Không được vượt quá mức tối đa người dùng cấu hình.
        BigDecimal candidateBid = preferredBid.min(maximumAmount);

        if (candidateBid.compareTo(minimumAllowed) < 0) {
            //auto bid dừng vì vượt quá ngân sách đã đặt
            return AutoBidDecision.stop("Auto-bid đã dừng vì giá hợp lệ tiếp theo vượt quá mức tối đa.");
        }
        /**
         * Cho phép đặt giá mới.
         * Trả về:
         * - số tiền cần đặt
         * - trạng thái đã dùng mức tối đa hay chưa
         */
        return AutoBidDecision.place(candidateBid, candidateBid.compareTo(maximumAmount) == 0
                && preferredBid.compareTo(maximumAmount) > 0);
    }
    /**
     * decide():Đây là hàm quan trọng nhất.
     * Quyết định xem Auto-Bid có nên đặt giá tiếp hay không.
     * Input:
     * - currentPrice      : Giá hiện tại
     * - minimumIncrement  : Bước giá tối thiểu hệ thống
     * - configuredStep    : Bước giá người dùng cấu hình
     * - maximumAmount     : Mức giá tối đa cho phép
     * Output:
     * - Đặt giá mới hoặc Dừng Auto-Bid
     */
    public AutoBidDecision decide(...)
    public static final class AutoBidDecision {
        private final boolean shouldBid;
        private final BigDecimal bidAmount;
        private final String stopReason;
        private final boolean usedMaximum;

        /**
         * Kết quả quyết định của Auto-Bid.
         * Có thể: PLACE BID hoặc STOP
         */
        public static final class AutoBidDecision
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

        /**
         * true=>tiếp tục đặt giá
         * false=>dừng Auto-Bid
         */
        public boolean shouldBid() {
            return shouldBid;
        }

        //số tiền auto-bid đặt
        public BigDecimal bidAmount() {
            return bidAmount;
        }

        /**
         * Lý do Auto-Bid dừng.
         * Ví dụ:vượt ngân sách
         */
        public String stopReason() {
            return stopReason;
        }

        /**
         * Đánh dấu lần đặt giá này có sử dụng mức tối đa hay không.
         */
        public boolean usedMaximum() {
            return usedMaximum;
        }
    }
}
