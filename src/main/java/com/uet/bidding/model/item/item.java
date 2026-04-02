package com.uet.bidding.model.item;

import com.uet.bidding.model.Entity;

public abstract class item extends Entity{


    //Attributes
    private String name;
    private String description;
    private final long startingPrice;
    private final String sellerId;
    private boolean active;
    private String imageURl;



    //Constructor
    protected item(String name,String description, long startingPrice, String sellerId){
        super();
        if (name==null || name.isBlank()) throw new IllegalArgumentException("name cannot be blank");
        if (startingPrice<0) throw new IllegalArgumentException("startingPrice must be positive");
        this.name=name;
        this.description=description;
        this.startingPrice=startingPrice;
        this.sellerId=sellerId;
        this.active=true;
    }



    //Getter
    public String getName() {
        return name;
    }
    public String getDescription() {
        return description;
    }
    public long getStartingPrice() {
        return startingPrice;
    }
    public String getSellerId() {
        return sellerId;
    }
    public boolean isActive() {
        return active;
    }
    public String getImageURl() {
        return imageURl;
    }


    //Setter

    public void setName(String name) {
        if (name==null || name.isBlank()) throw new IllegalArgumentException("name cannot be blank");
        this.name = name;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public void setActive(boolean active) {
        this.active = active;
    }
    public void setImageURl(String imageURl) {
        this.imageURl = imageURl;
    }


    //abstract method
    public abstract String getCategory();
    public abstract boolean isValidForListing();



    //method
    @Override
    public void printInfo() {
        System.out.println("=== ITEM: " + name + " [" + getCategory() + "] ===");
        System.out.println("ID          : " + getId());
        System.out.println("Description : " + description);
        System.out.println("Start Price : " + startingPrice + " VND");
        System.out.println("Seller ID   : " + sellerId);
        System.out.println("Active      : " + active);
    }


}
