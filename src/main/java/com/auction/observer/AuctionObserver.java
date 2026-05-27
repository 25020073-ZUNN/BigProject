package com.auction.observer;

import com.auction.model.Auction;

import java.util.List;

public interface AuctionObserver {
    void onAuctionsUpdated(List<Auction> auctions);
}
/*AuctionObserver là interface triển khai Observer Pattern để hỗ trợ cập nhật realtime trong hệ thống đấu giá.
Khi dữ liệu đấu giá thay đổi, AuctionService sẽ gọi onAuctionsUpdated() để thông báo cho các thành phần đang lắng nghe như Server hoặc UI.
Cách này giúp giảm phụ thuộc giữa các module và dễ mở rộng hệ thống.*/