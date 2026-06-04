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

/**
 * Lớp kiểm thử đơn vị cho giao dịch đặt giá thầu (BidTransaction).
 * Đảm bảo tính toàn vẹn của dữ liệu giao dịch bằng cách kiểm tra việc khởi tạo
 * các tham số hợp lệ và ngăn chặn các tham số null hoặc số tiền thầu không hợp lý (<= 0).
 */
class BidTransactionTest {

    /**
     * Kiểm thử trường hợp: Khởi tạo đối tượng BidTransaction thành công
     * khi truyền vào đầy đủ các tham số hợp lệ, tự động gán thời gian đặt giá thầu.
     */
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

        // Xác nhận các thuộc tính được gán đúng giá trị và thời gian giao dịch không bị null
        assertEquals(auction, transaction.getAuction());
        assertEquals(bidder, transaction.getBidder());
        assertEquals(bidAmount, transaction.getBidAmount());
        assertNotNull(transaction.getBidTime());
    }

    /**
     * Kiểm thử trường hợp: Từ chối khởi tạo giao dịch thầu nếu đối tượng Auction bị null.
     */
    @Test
    void constructorRejectsNullAuction() {
        User bidder = new Bidder("bidder", "bidder@example.com", "hash");
        
        // Xác nhận ném ra IllegalArgumentException khi thiếu phiên đấu giá
        assertThrows(IllegalArgumentException.class, () -> new BidTransaction(null, bidder, new BigDecimal("1500")));
    }

    /**
     * Kiểm thử trường hợp: Từ chối khởi tạo giao dịch thầu nếu đối tượng Bidder bị null.
     */
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

        // Xác nhận ném ra IllegalArgumentException khi thiếu người đặt giá
        assertThrows(IllegalArgumentException.class, () -> new BidTransaction(auction, null, new BigDecimal("1500")));
    }

    /**
     * Kiểm thử trường hợp: Từ chối khởi tạo giao dịch thầu nếu số tiền đặt bị null, bằng 0 hoặc là số âm.
     */
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

        // Xác nhận ném ra ngoại lệ IllegalArgumentException với các mốc tiền không hợp lệ
        assertThrows(IllegalArgumentException.class, () -> new BidTransaction(auction, bidder, null));
        assertThrows(IllegalArgumentException.class, () -> new BidTransaction(auction, bidder, BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> new BidTransaction(auction, bidder, new BigDecimal("-10")));
    }
}

