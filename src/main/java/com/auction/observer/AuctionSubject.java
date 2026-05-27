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
/*Quản lý danh sách các observer và gửi thông báo khi dữ liệu đấu giá thay đổi.*/
/*AuctionSubject là lớp Subject trong Observer Pattern, chịu trách nhiệm quản lý danh sách observer và gửi thông báo khi dữ liệu đấu giá thay đổi.
Lớp sử dụng CopyOnWriteArrayList để đảm bảo thread-safe khi nhiều client cùng đăng ký hoặc nhận cập nhật realtime.
Việc dùng Observer giúp giảm phụ thuộc giữa nghiệp vụ đấu giá và giao diện hiển thị.*/