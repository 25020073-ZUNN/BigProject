package com.auction.observer;

import com.auction.model.Auction;

import java.util.List;

public interface AuctionObserver {
    void onAuctionsUpdated(List<Auction> auctions);
}
