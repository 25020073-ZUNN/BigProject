package com.uet.bidding.model.user;

import java.util.*;

public class Seller extends User{


    //Attributes
    private List<String> listItemIds;
    private double rating;
    private int totalRatings;


    //Constructor
    protected Seller(String username,String email,String passwordHash){
        super(username,email,passwordHash);
        this.rating=0.0;
        this.totalRatings=0;
    }


    //Getter
    public List<String> getListItemIds() {
        return new ArrayList<String>(getListItemIds());
    }
    public double getRating() {
        return rating;
    }
    public int getTotalRatings() {
        return totalRatings;
    }


    //Method
    @Override
    public String getRole(){
        return "SELLER";
    }
    public void addListedItem(String itemId){
        this.getListItemIds().add(itemId);
    }
    public void addRating(double newRating){
        if (newRating<0 || newRating>5) throw new IllegalArgumentException("Rating must be 0-5");
        else{
            this.rating=(this.rating*totalRatings+newRating)/(totalRatings+1);
            this.totalRatings+=1;
        }
    }

}
