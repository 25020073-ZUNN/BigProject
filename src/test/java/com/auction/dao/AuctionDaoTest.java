package com.auction.dao;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Lớp kiểm thử đơn vị cho AuctionDao.
 * Lớp này tập trung kiểm thử logic nghiệp vụ xử lý thời gian kết thúc của phiên đấu giá
 * (đặc biệt là tính năng chống bắn tỉa - Anti-sniping).
 */
class AuctionDaoTest {

    /**
     * Kiểm thử trường hợp: Tự động gia hạn thời gian kết thúc thêm 2 phút
     * nếu có người đặt giá thầu (Bid) thành công trong khoảng 2 phút cuối của phiên.
     */
    @Test
    void resolveEffectiveEndTimeExtendsAuctionWhenBidArrivesInLastTwoMinutes() {
        // Thiết lập thời gian kết thúc gốc là sau 90 giây nữa (nằm trong cửa sổ 2 phút cuối)
        LocalDateTime originalEndTime = LocalDateTime.now().plusSeconds(90);

        // Chạy hàm xử lý gia hạn thời gian kết thúc
        LocalDateTime effectiveEndTime = AuctionDao.resolveEffectiveEndTime(originalEndTime);

        // Xác nhận: Thời gian kết thúc mới phải bằng thời gian cũ cộng thêm 2 phút
        assertEquals(originalEndTime.plusMinutes(2), effectiveEndTime);
    }

    /**
     * Kiểm thử trường hợp: Giữ nguyên thời hạn kết thúc
     * nếu lượt đặt giá thầu diễn ra khi phiên đấu giá còn nhiều thời gian (ngoài 2 phút cuối).
     */
    @Test
    void resolveEffectiveEndTimeKeepsDeadlineWhenAuctionIsNotNearEnding() {
        // Thiết lập thời gian kết thúc gốc là sau 5 phút nữa (ngoài cửa sổ 2 phút cuối)
        LocalDateTime originalEndTime = LocalDateTime.now().plusMinutes(5);

        // Chạy hàm xử lý gia hạn thời gian kết thúc
        LocalDateTime effectiveEndTime = AuctionDao.resolveEffectiveEndTime(originalEndTime);

        // Xác nhận: Thời gian kết thúc không đổi, vẫn giữ nguyên mốc cũ
        assertEquals(originalEndTime, effectiveEndTime);
    }
}

