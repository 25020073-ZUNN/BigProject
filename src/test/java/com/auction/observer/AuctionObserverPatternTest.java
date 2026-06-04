package com.auction.observer;

import com.auction.model.Auction;
import com.auction.model.item.Item;
import com.auction.model.user.Seller;
import com.auction.model.user.User;
import com.auction.factory.ItemFactory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lớp kiểm thử đơn vị cho Observer Design Pattern trong module đấu giá.
 * Đảm bảo các Observer (người quan sát / client) đăng ký lắng nghe
 * sẽ nhận được bản tin thông báo (update broadcast) khi danh sách đấu giá thay đổi.
 */
class AuctionObserverPatternTest {

    // Lớp Mock Observer dùng để giả lập các client kết nối nhận cập nhật
    private static class MockAuctionObserver implements AuctionObserver {
        private final List<Auction> receivedAuctions = new ArrayList<>();
        private int updateCount = 0;

        @Override
        public void onAuctionsUpdated(List<Auction> auctions) {
            receivedAuctions.clear();
            receivedAuctions.addAll(auctions);
            updateCount++;
        }

        public List<Auction> getReceivedAuctions() {
            return receivedAuctions;
        }

        public int getUpdateCount() {
            return updateCount;
        }
    }

    /**
     * Kiểm thử trường hợp: Đăng ký observer mới.
     * Đảm bảo một observer chỉ được đăng ký duy nhất 1 lần (không trùng lặp).
     */
    @Test
    void addObserverRegistersNewObserversOnlyOnce() {
        AuctionSubject subject = new AuctionSubject();
        assertFalse(subject.hasObservers());

        MockAuctionObserver observer = new MockAuctionObserver();
        subject.addObserver(observer);
        assertTrue(subject.hasObservers());

        // Đăng ký lại cùng 1 observer không làm tăng số lượng (vì dùng CopyOnWriteArrayList.addIfAbsent)
        subject.addObserver(observer);
        
        subject.removeObserver(observer);
        assertFalse(subject.hasObservers());
    }

    /**
     * Kiểm thử trường hợp: Gửi thông báo (notify).
     * Khi gọi notifyObservers(), toàn bộ các Observer đã đăng ký
     * đều phải nhận được đúng dữ liệu và số lần callback tương ứng.
     */
    @Test
    void notifyObserversTriggersCallbackOnAllRegisteredObservers() {
        AuctionSubject subject = new AuctionSubject();
        MockAuctionObserver observer1 = new MockAuctionObserver();
        MockAuctionObserver observer2 = new MockAuctionObserver();

        subject.addObserver(observer1);
        subject.addObserver(observer2);

        // Tạo dữ liệu giả lập sản phẩm và phiên đấu giá
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
        List<Auction> auctionList = List.of(auction);

        // Kích hoạt thông báo broadcast tới tất cả các observers
        subject.notifyObservers(auctionList);

        // Kiểm tra xem cả 2 observer đều nhận được đúng dữ liệu và số lần gọi là 1
        assertEquals(1, observer1.getUpdateCount());
        assertEquals(1, observer2.getUpdateCount());
        assertEquals(1, observer1.getReceivedAuctions().size());
        assertEquals(auction, observer1.getReceivedAuctions().get(0));
        assertEquals(auction, observer2.getReceivedAuctions().get(0));
    }
}

