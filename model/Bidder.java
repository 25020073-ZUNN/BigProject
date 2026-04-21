package model;
public class Bidder extends User {
    private double balance;
    public Bidder(String id, String name, String email, double balance) {
        super(id, name, email);
        this.balance = balance;
    }
    public double getBalance() {
        return balance;
    }
}