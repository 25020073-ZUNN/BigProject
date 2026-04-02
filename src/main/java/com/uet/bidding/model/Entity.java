package com.uet.bidding.model;
import java.util.UUID;
public  abstract class Entity{



    //THuoc tinh
    private final String id;
    private final long createAt;




    //CONSTRUCTOR
    protected Entity(){
        this.id=UUID.randomUUID().toString(); //ID NGAU NHIEN O DANG STRING
        this.createAt=System.currentTimeMillis();  //THOI GIAN THUC;
    }



    //GETTER
    public String getId() {
        return id;
    }
    public long getCreateAt() {
        return createAt;
    }



    //METHOD
    public abstract void printInfo();
    @Override
    public String toString(){
        return this.getId();
    }
}