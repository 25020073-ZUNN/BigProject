package com.auction.observer;

import com.auction.model.Auction;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class AuctionSubject {

    private final CopyOnWriteArrayList<AuctionObserver> observers = new CopyOnWriteArrayList<>();

    public void addObserver(AuctionObserver observer) {
        if (observer != null) {
            observers.addIfAbsent(observer);
        }
    }

    public void removeObserver(AuctionObserver observer) {
        observers.remove(observer);
    }

    public boolean hasObservers() {
        return !observers.isEmpty();
    }

    protected void notifyObservers(List<Auction> auctions) {
        for (AuctionObserver observer : observers) {
            observer.onAuctionsUpdated(auctions);
        }
    }
}
