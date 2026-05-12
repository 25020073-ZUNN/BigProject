package com.auction.network.client;

import java.util.Map;

/**
 * Interface cho các đối tượng muốn lắng nghe cập nhật real-time từ Server.
 */
public interface AuctionUpdateListener {
    /**
     * Được gọi khi có một phiên đấu giá cập nhật (giá mới, kết thúc, vv).
     * @param auctionData Dữ liệu phiên đấu giá mới
     */
    void onAuctionUpdated(Map<String, Object> auctionData);

    /**
     * Được gọi khi có phiên đấu giá mới được tạo.
     * @param auctionData Dữ liệu phiên đấu giá mới
     */
    default void onAuctionCreated(Map<String, Object> auctionData) {}
}
