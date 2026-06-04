package com.auction.model;

import com.auction.model.item.Item;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;
import com.auction.model.user.User;
import com.auction.factory.ItemFactory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Lớp kiểm thử đơn vị cho mô hình phiên đấu giá (Auction).
 * Kiểm tra các quy tắc nghiệp vụ (Business Rules) chính liên quan đến
 * việc đặt giá thầu, kết thúc phiên, quyền sở hữu tài sản và phân định người chiến thắng.
 */
class AuctionTest {

    /**
     * Kiểm thử trường hợp: Đặt giá thầu hợp lệ cập nhật đúng giá hiện tại,
     * thiết lập người giữ giá cao nhất và lưu lịch sử giao dịch.
     */
    @Test
    void placeBidUpdatesCurrentPriceAndHighestBidder() {
        User seller = new Seller("seller", "seller@example.com", "pass");
        User bidder = new Bidder("bidder", "bidder@example.com", "pass");
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
        
        // Thực hiện đặt giá thầu 1,500 VND (lớn hơn giá khởi điểm 1,000 VND)
        auction.placeBid(bidder, new BigDecimal("1500"));

        // Xác nhận giá hiện tại tăng lên 1,500 VND và người giữ giá cao nhất là bidder
        assertEquals(new BigDecimal("1500"), auction.getCurrentPrice());
        assertEquals(bidder, auction.getHighestBidder());
        assertEquals(1, auction.getBidHistory().size());
    }

    /**
     * Kiểm thử trường hợp: Từ chối đặt giá thầu nếu số tiền đặt
     * không lớn hơn giá thầu hiện tại của phiên đấu giá.
     */
    @Test
    void placeBidRejectsAmountNotHigherThanCurrentPrice() {
        User seller = new Seller("seller", "seller@example.com", "pass");
        User bidder = new Bidder("bidder", "bidder@example.com", "pass");
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

        // Xác nhận ném ra ngoại lệ IllegalArgumentException khi đặt giá 1,000 VND (bằng giá hiện tại)
        assertThrows(IllegalArgumentException.class, () -> auction.placeBid(bidder, new BigDecimal("1000")));
    }

    /**
     * Kiểm thử trường hợp: Đóng phiên đấu giá thành công,
     * chuyển trạng thái phiên sang không hoạt động (inactive) và đã hoàn thành (finished).
     */
    @Test
    void closeAuctionMarksAuctionFinished() {
        User seller = new Seller("seller", "seller@example.com", "pass");
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
        
        // Gọi đóng phiên
        auction.closeAuction();

        // Xác nhận trạng thái đã hoàn tất và ngừng hoạt động
        assertTrue(auction.isFinished());
        assertTrue(!auction.isActive());
    }

    /**
     * Kiểm thử trường hợp: Ngăn chặn và từ chối mọi lượt đặt giá thầu mới
     * khi phiên đấu giá đã kết thúc hoặc không còn hoạt động.
     */
    @Test
    void placeBidRejectsBidOnFinishedOrInactiveAuction() {
        User seller = new Seller("seller", "seller@example.com", "pass");
        User bidder = new Bidder("bidder", "bidder@example.com", "pass");
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

        // Xác nhận ném ra ngoại lệ IllegalStateException khi đặt giá khi phiên đã đóng
        assertThrows(IllegalStateException.class, () -> auction.placeBid(bidder, new BigDecimal("1500")));

        // Khởi tạo phiên thứ hai và đặt ở trạng thái inactive
        Auction auction2 = new Auction(item, seller, item.getCurrentPrice());
        auction2.setActive(false);
        
        // Xác nhận ném ra ngoại lệ IllegalStateException khi đặt giá khi phiên inactive
        assertThrows(IllegalStateException.class, () -> auction2.placeBid(bidder, new BigDecimal("1500")));
    }

    /**
     * Kiểm thử trường hợp: Ngăn chặn người bán (Seller) tự đấu giá sản phẩm
     * do chính họ đăng bán để giữ sự minh bạch cho hệ thống.
     */
    @Test
    void placeBidRejectsBidBySellerOnTheirOwnItem() {
        User seller = new Seller("seller", "seller@example.com", "pass");
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

        // Xác nhận ném ra ngoại lệ IllegalArgumentException khi người bán tự bid
        assertThrows(IllegalArgumentException.class, () -> auction.placeBid(seller, new BigDecimal("1500")));
    }

    /**
     * Kiểm thử trường hợp: Ngăn chặn việc đóng một phiên đấu giá
     * khi phiên đó đã được đóng từ trước.
     */
    @Test
    void closeAuctionThrowsIfAlreadyClosed() {
        User seller = new Seller("seller", "seller@example.com", "pass");
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

        // Xác nhận ném ra ngoại lệ IllegalStateException khi cố đóng lại lần 2
        assertThrows(IllegalStateException.class, () -> auction.closeAuction());
    }

    /**
     * Kiểm thử trường hợp: Ngăn chặn việc truy vấn người chiến thắng
     * khi phiên đấu giá vẫn đang diễn ra và chưa kết thúc.
     */
    @Test
    void getWinnerThrowsIfAuctionNotFinished() {
        User seller = new Seller("seller", "seller@example.com", "pass");
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

        // Xác nhận ném ra ngoại lệ IllegalStateException khi cố lấy người thắng từ phiên chưa đóng
        assertThrows(IllegalStateException.class, () -> auction.getWinner());
    }

    /**
     * Kiểm thử trường hợp: Khi đóng phiên đấu giá, ghi nhận chính xác người chiến thắng
     * và đưa ID phiên đấu giá vào danh sách thắng thầu của người dùng đó.
     */
    @Test
    void closeAuctionAddsWonAuctionToHighestBidder() {
        User seller = new Seller("seller", "seller@example.com", "pass");
        User bidder = new Bidder("bidder", "bidder@example.com", "pass");
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
        auction.closeAuction();

        // Xác nhận người chiến thắng
        assertEquals(bidder, auction.getWinner());
        // Kiểm tra xem ID phiên đấu giá đã được đưa vào danh sách thắng của bidder chưa
        assertTrue(bidder.getWonAuctionIds().contains(auction.getId()));
    }
}

