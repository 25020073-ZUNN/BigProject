package com.auction.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Lớp kiểm thử đơn vị cho thuật toán Tự động đấu giá (AutoBidStrategy).
 * Đảm bảo hệ thống nâng giá hợp lý dựa theo bước giá, dừng thầu chính xác
 * khi vượt quá hạn mức tối đa của người dùng, và xử lý an toàn các dữ liệu lỗi (null/âm).
 */
class AutoBidStrategyTest {

    private final AutoBidStrategy strategy = new AutoBidStrategy();

    /**
     * Kiểm thử trường hợp: Đề xuất mức giá thầu tối đa được cấu hình
     * nếu bước nhảy ưa thích cộng thêm làm vượt quá hạn mức tối đa (nhưng vẫn nằm trong phạm vi cho phép).
     */
    @Test
    void decideUsesConfiguredMaximumWhenPreferredStepOvershoots() {
        // Giá hiện tại: 100, bước tối thiểu: 10, bước nhảy ưa thích: 50, hạn mức tối đa: 120
        // Giá thầu hợp lý tiếp theo là 110. Cộng bước nhảy ưa thích (50) sẽ thành 150 (vượt quá 120).
        // Thuật toán phải đưa ra đề nghị thầu bằng đúng hạn mức tối đa là 120.
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

    /**
     * Kiểm thử trường hợp: Dừng đấu giá tự động
     * khi giá thầu hợp lệ tiếp theo (giá hiện tại + bước tối thiểu) đã vượt quá hạn mức tối đa của người dùng.
     */
    @Test
    void decideStopsWhenMaximumIsBelowNextValidBid() {
        // Giá hiện tại: 100, bước tối thiểu: 10, giá tối thiểu tiếp theo phải là 110.
        // Hạn mức tối đa đặt là 105 (nhỏ hơn 110). Hệ thống phải dừng thầu.
        AutoBidStrategy.AutoBidDecision decision = strategy.decide(
                new BigDecimal("100"),
                new BigDecimal("10"),
                new BigDecimal("50"),
                new BigDecimal("105")
        );

        assertFalse(decision.shouldBid());
        assertEquals("Auto-bid đã dừng vì giá hợp lệ tiếp theo vượt quá mức tối đa.", decision.stopReason());
    }

    /**
     * Kiểm thử trường hợp: Xử lý an toàn khi các tham số đầu vào bị null,
     * trả về quyết định không thầu cùng lý do thiếu dữ liệu để tránh làm lỗi luồng chạy của Server.
     */
    @Test
    void decideHandlesNullParametersGracefully() {
        // Trường hợp giá hiện tại bị null
        AutoBidStrategy.AutoBidDecision decision1 = strategy.decide(null, new BigDecimal("10"), new BigDecimal("50"), new BigDecimal("105"));
        assertFalse(decision1.shouldBid());
        assertEquals("Auto-bid chưa có đủ dữ liệu để hoạt động.", decision1.stopReason());

        // Trường hợp bước tối thiểu bị null
        AutoBidStrategy.AutoBidDecision decision2 = strategy.decide(new BigDecimal("100"), null, new BigDecimal("50"), new BigDecimal("105"));
        assertFalse(decision2.shouldBid());
        assertEquals("Auto-bid chưa có đủ dữ liệu để hoạt động.", decision2.stopReason());
    }

    /**
     * Kiểm thử trường hợp: Dừng đấu giá tự động khi gặp các tham số giá âm,
     * đảm bảo hệ thống không chấp nhận đặt giá thầu âm.
     */
    @Test
    void decideWithNegativeParameters() {
        AutoBidStrategy.AutoBidDecision decision = strategy.decide(
                new BigDecimal("-10"),
                new BigDecimal("-5"),
                new BigDecimal("-2"),
                new BigDecimal("-20")
        );
        assertFalse(decision.shouldBid());
        assertEquals("Auto-bid đã dừng vì giá hợp lệ tiếp theo vượt quá mức tối đa.", decision.stopReason());
    }
}

