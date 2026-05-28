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

class AuctionObserverPatternTest {

    // Lớp stub/mock để kiểm thử
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

    @Test
    void notifyObserversTriggersCallbackOnAllRegisteredObservers() {
        AuctionSubject subject = new AuctionSubject();
        MockAuctionObserver observer1 = new MockAuctionObserver();
        MockAuctionObserver observer2 = new MockAuctionObserver();

        subject.addObserver(observer1);
        subject.addObserver(observer2);

        // Tạo dữ liệu giả lập
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

        // Kích hoạt thông báo
        subject.notifyObservers(auctionList);

        // Kiểm tra xem cả 2 observer đều nhận được đúng dữ liệu và số lần gọi là 1
        assertEquals(1, observer1.getUpdateCount());
        assertEquals(1, observer2.getUpdateCount());
        assertEquals(1, observer1.getReceivedAuctions().size());
        assertEquals(auction, observer1.getReceivedAuctions().get(0));
        assertEquals(auction, observer2.getReceivedAuctions().get(0));
    }
}
