package com.uet.bidding.model;

public class Bidder{
    private double balance;
    private String bidHistory;
    public Bidder(double balance,String bidHistory) {
        this.balance = balance;
        this.bidHistory=bidHistory;
    }
    public String getBidHistory() {
        return bidHistory;
    }
    public double getBalance() {
        return balance;
    }
}