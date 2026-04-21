package model;
public class BidTransaction {
    private Bidder bidder;
    private double amount;
    private long timestamp;

    public BidTransaction(Bidder bidder, double amount) {
        this.bidder = bidder;
        this.amount = amount;
        this.timestamp = System.currentTimeMillis();
    }
    public double getAmount() {
        return amount;
    }
    public Bidder getBidder() {
        return bidder;
    }
}