package com.uet.bidding.model.user;

import java.util.*;

public class Bidder extends User{

    //Attributes
    private List<String> wonAuctionIds;
    private boolean autoBidEnabled;   //kiem tra trang thai auto
    private long autoBidmaxPrice=0L;
    private long autoBidIncrement=0L;



    //Constructor
    protected Bidder(String username,String email,String passwordHash){
        super(username,email,passwordHash);
        this.autoBidEnabled=false;
        this.autoBidmaxPrice=0L;
        this.autoBidIncrement=0L;
    }



    //Getter
    public boolean isAutoBidEnabled() {
        return autoBidEnabled;
    }
    public long getAutoBidmaxPrice() {
        return autoBidmaxPrice;
    }
    public long getAutoBidIncrement() {
        return autoBidIncrement;
    }
    public List<String> getWonAuctionIds() {
        return new ArrayList<String>(wonAuctionIds);       //DEEP COPY
    }



    //Method
    @Override
    public String getRole(){return "BIDDER";
    }
    public void enableAutoBid(long maxPrice, long increment){
        if (maxPrice<=0 || increment <=0) throw new IllegalArgumentException("Autobid value must be positive!");
        else{
            this.autoBidEnabled=true;
            this.autoBidmaxPrice=maxPrice;
            this.autoBidIncrement=increment;
        }
    }
    public void disableAutoBid(){
        this.autoBidEnabled=false;
    }
    public void recordWin(String auctionId){
        wonAuctionIds.add(auctionId);
    }
    public int getTotalWins(){
        return wonAuctionIds.size();
    }
}
