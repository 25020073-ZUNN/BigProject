package model;
public class Item extends Entity {
    private String name;
    private double startPrice;

    public Item(String id, String name, double startPrice) {
        super(id);
        this.name = name;
        this.startPrice = startPrice;
    }
    public double getStartPrice() {
        return startPrice;
    }
}