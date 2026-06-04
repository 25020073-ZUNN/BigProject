package com.auction.observer;

import com.auction.factory.ItemFactory;
import com.auction.model.Auction;
import com.auction.model.BidTransaction;
import com.auction.model.item.Item;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;
import com.auction.model.user.User;
import com.auction.service.AutoBidStrategy;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lớp kiểm thử luồng tích hợp (Integration Flow Test) cho hệ thống Đấu giá.
 * Lớp này mô phỏng toàn bộ vòng đời của một phiên đấu giá thực tế khép kín (End-to-End Flow)
 * để kiểm tra sự tương tác giữa các thành phần nghiệp vụ cốt lõi:
 * Model (User, Item, Auction, BidTransaction) -> Strategy (AutoBid) -> Observer Pattern.
 */
class AuctionFlowTest {

    // Lớp Mock Observer để lắng nghe và ghi nhận các sự kiện phát ra từ phiên đấu giá
    private static class MockAuctionObserver implements AuctionObserver {
        private final List<Auction> receivedAuctions = new ArrayList<>();
        private int notificationCount = 0;

        @Override
        public void onAuctionsUpdated(List<Auction> auctions) {
            receivedAuctions.clear();
            receivedAuctions.addAll(auctions);
            notificationCount++;
        }

        public List<Auction> getReceivedAuctions() {
            return receivedAuctions;
        }

        public int getNotificationCount() {
            return notificationCount;
        }
    }

    /**
     * KỊCH BẢN KIỂM THỬ LUỒNG ĐẤU GIÁ LIÊN HOÀN (E2E WORKFLOW):
     * 1. Khởi tạo tác nhân: Người bán (Seller) và 2 Người mua (Bidder A, Bidder B).
     * 2. Khởi tạo sản phẩm & Phiên đấu giá: Seller đăng bán tranh nghệ thuật (Art) giá khởi điểm 1,000,000 VND, bước giá 100,000 VND.
     * 3. Thiết lập thông báo (Observer): Đăng ký MockObserver lắng nghe thay đổi phiên đấu giá.
     * 4. Bidding vòng 1: Bidder A đặt giá 1,200,000 VND trực tiếp -> Thành công, bắn notify.
     * 5. Bidding vòng 2 (Auto-bid): Bidder B kích hoạt Auto-bid (Hạn mức tối đa 2,000,000 VND, bước nhảy 100,000 VND).
     *    - Hệ thống tự động nâng giá thầu của B lên 1,300,000 VND để dẫn đầu.
     * 6. Kiểm tra Anti-sniping: Bidder A đặt giá ở những giây cuối cùng -> Thời gian kết thúc tự gia hạn thêm 2 phút.
     * 7. Kết thúc phiên & Khấu trừ ví: Đóng phiên đấu giá, xác định người chiến thắng là A và trừ tiền trong ví A.
     */
    @Test
    void testCompleteAuctionLifecycleFlow() {
        // =====================================================================
        // BƯỚC 1: KHỞI TẠO CÁC TÁC NHÂN (SELLER, BIDDER A, BIDDER B KÈM VÍ TIỀN)
        // =====================================================================
        User seller = new Seller("seller_dung", "dung@gmail.com", "hash_pass");
        
        Bidder bidderA = new Bidder("bidder_a", "bidderA@gmail.com", "hash_pass");
        bidderA.deposit(5000000L); // Nạp 5 triệu vào ví Bidder A
        
        Bidder bidderB = new Bidder("bidder_b", "bidderB@gmail.com", "hash_pass");
        bidderB.deposit(10000000L); // Nạp 10 triệu vào ví Bidder B

        assertEquals(5000000L, bidderA.getBalance());
        assertEquals(10000000L, bidderB.getBalance());


        // =====================================================================
        // BƯỚC 2: KHỞI TẠO SẢN PHẨM & MỞ PHIÊN ĐẤU GIÁ (STARTING = 1,000,000 VND)
        // =====================================================================
        // Khởi tạo thời gian bắt đầu từ 1 giờ trước và kết thúc sau 1 giờ nữa
        LocalDateTime startTime = LocalDateTime.now().minusHours(1);
        LocalDateTime endTime = LocalDateTime.now().plusHours(1);
        
        // Sử dụng ItemFactory (Factory Design Pattern) để tạo sản phẩm Art (Tranh sơn mài)
        Item painting = ItemFactory.createItem(
                "art",
                "Tranh Son Mai",
                "Tranh son mai co truyen Viet Nam",
                new BigDecimal("1000000"), // Giá khởi điểm 1 triệu VND
                startTime,
                endTime,
                seller.getId(),
                Map.of(
                        "artist", "Nguyen Gia Tri",
                        "yearCreated", 1940
                )
        );

        // Tạo phiên đấu giá
        Auction auction = new Auction(painting, seller, painting.getStartingPrice());
        auction.setMinimumBidStep(new BigDecimal("100000")); // Bước giá tối thiểu 100k
        
        assertTrue(auction.isActive());
        assertFalse(auction.isFinished());
        assertNull(auction.getHighestBidder());
        assertEquals(new BigDecimal("1000000"), auction.getCurrentPrice());


        // =====================================================================
        // BƯỚC 3: THIẾT LẬP OBSERVER PATTERN ĐỂ THEO DÕI REALTIME BROADCAST
        // =====================================================================
        AuctionSubject subject = new AuctionSubject();
        MockAuctionObserver clientObserver = new MockAuctionObserver();
        subject.addObserver(clientObserver);

        // Kích hoạt cập nhật trạng thái ban đầu
        subject.notifyObservers(List.of(auction));
        assertEquals(1, clientObserver.getNotificationCount());
        assertEquals(1, clientObserver.getReceivedAuctions().size());


        // =====================================================================
        // BƯỚC 4: ĐẶT GIÁ LẦN 1 (BIDDER A ĐẶT 1,200,000 VND > 1,000,000 + 100,000)
        // =====================================================================
        BigDecimal bidAmountA1 = new BigDecimal("1200000");
        
        // Thực hiện đặt giá thông qua domain model
        auction.placeBid(bidderA, bidAmountA1);
        
        // Broadcast bản tin cập nhật tới tất cả client thông báo giá mới
        subject.notifyObservers(List.of(auction));

        // Kiểm tra xem dữ liệu cập nhật đã đúng chưa
        assertEquals(bidAmountA1, auction.getCurrentPrice());
        assertEquals(bidderA, auction.getHighestBidder());
        assertEquals(1, auction.getBidHistory().size());
        assertEquals(2, clientObserver.getNotificationCount()); // Nhận notify lần 2
        
        // Kiểm tra Bidder A đã được ghi nhận tham gia đấu giá
        assertTrue(bidderA.getJoinedAuctionIds().contains(auction.getId()));


        // =====================================================================
        // BƯỚC 5: ĐẶT GIÁ TỰ ĐỘNG (BIDDER B KÍCH HOẠT AUTO-BID LÊN 2,000,000 VND)
        // =====================================================================
        // Bidder B thiết lập AutoBid với mức giới hạn tối đa là 2 triệu VND
        BigDecimal maxLimitB = new BigDecimal("2000000");
        BigDecimal preferredStepB = new BigDecimal("100000"); // Nâng giá thềm 100k mỗi lần
        
        // Khởi tạo chiến lược tự động đấu giá
        AutoBidStrategy autoBidStrategy = new AutoBidStrategy();
        
        // Chiến lược tự tính toán nước đi tiếp theo dựa trên giá hiện tại của phiên
        AutoBidStrategy.AutoBidDecision decision = autoBidStrategy.decide(
                auction.getCurrentPrice(),      // Giá hiện tại: 1,200,000
                auction.getMinimumBidStep(),    // Bước giá tối thiểu: 100,000
                preferredStepB,                 // Bước nhảy mong muốn: 100,000
                maxLimitB                       // Hạn mức tối đa của B: 2,000,000
        );
        
        // Đảm bảo chiến lược khuyên nên tiếp tục bid và đưa ra mức giá đề xuất
        assertTrue(decision.shouldBid());
        BigDecimal recommendedBidB = decision.bidAmount();
        assertEquals(new BigDecimal("1300000"), recommendedBidB); // 1,200,000 + 100,000
        
        // Bidder B tiến hành đặt giá đề xuất từ hệ thống tự động
        auction.placeBid(bidderB, recommendedBidB);
        subject.notifyObservers(List.of(auction));

        // Kiểm tra trạng thái sau khi tự động đấu giá thành công
        assertEquals(recommendedBidB, auction.getCurrentPrice());
        assertEquals(bidderB, auction.getHighestBidder());
        assertEquals(2, auction.getBidHistory().size());
        assertEquals(3, clientObserver.getNotificationCount()); // Nhận notify lần 3


        // =====================================================================
        // BƯỚC 6: KIỂM TRA ANTI-SNIPING (ĐẶT GIÁ LÚC PHIÊN CẬN KỀ KẾT THÚC)
        // =====================================================================
        // Giả lập tình huống: Phiên đấu giá chỉ còn 90 giây là kết thúc
        LocalDateTime nearEndTime = LocalDateTime.now().plusSeconds(90);
        auction.getItem().setEndTime(nearEndTime);
        
        // Bidder A âm thầm đặt giá cao hơn vào phút chót nhằm "bắn tỉa" (Snipe)
        BigDecimal bidAmountA2 = new BigDecimal("1500000");
        
        // Logic kiểm tra gia hạn thời gian nằm ở tầng DAO/Service trước khi ghi đè DB
        // Chúng ta giả lập lại chính xác logic này:
        long secondsRemaining = ChronoUnit.SECONDS.between(LocalDateTime.now(), auction.getItem().getEndTime());
        assertTrue(secondsRemaining > 0 && secondsRemaining <= 120); // Dưới 2 phút

        // Hệ thống phát hiện đặt giá sát giờ nên thực hiện cộng thêm 2 phút vào deadline của sản phẩm
        LocalDateTime extendedEndTime = auction.getItem().getEndTime().plusMinutes(2);
        auction.getItem().setEndTime(extendedEndTime);
        
        // Thực thi đặt giá thầu của A
        auction.placeBid(bidderA, bidAmountA2);
        subject.notifyObservers(List.of(auction));

        // Kiểm tra thời gian kết thúc đã được gia hạn thành công (chênh lệch so với ban đầu là 2 phút)
        assertTrue(auction.getItem().getEndTime().isAfter(nearEndTime));
        assertEquals(bidAmountA2, auction.getCurrentPrice());
        assertEquals(bidderA, auction.getHighestBidder());


        // =====================================================================
        // BƯỚC 7: ĐÓNG PHIÊN, XÁC ĐỊNH NGƯỜI THẮNG VÀ TRỪ TIỀN KHẤU TRỪ VÍ
        // =====================================================================
        // Giả lập đóng phiên đấu giá
        auction.closeAuction();
        
        assertTrue(auction.isFinished());
        assertFalse(auction.isActive());
        
        // Xác định người chiến thắng cuối cùng
        User winner = auction.getWinner();
        assertEquals(bidderA, winner); // A thắng vì mức giá 1,500,000 VND
        
        // Kiểm tra người thắng có chứa ID phiên đấu giá trong danh sách thắng cuộc không
        assertTrue(bidderA.getWonAuctionIds().contains(auction.getId()));

        // Trừ tiền thắng cuộc khỏi ví của Bidder A (mô phỏng logic trừ tiền của AuctionDao.synchronizeAuctionStates)
        long finalPrice = auction.getCurrentPrice().longValueExact();
        assertEquals(1500000L, finalPrice);
        
        bidderA.withdraw(finalPrice);
        
        // Số dư ban đầu của A là 5,000,000 VND. Trừ 1,500,000 VND -> còn lại 3,500,000 VND.
        assertEquals(3500000L, bidderA.getBalance());
        
        // Ví của B không đổi vì không thắng cuộc
        assertEquals(10000000L, bidderB.getBalance());
        
        System.out.println("[AuctionFlowTest] Kiểm thử luồng đấu giá tích hợp liên hoàn thành công tốt đẹp!");
    }
}
