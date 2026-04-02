package com.uet.bidding.model.user;

public class Admin extends User{

    //Attributes
    private int adminLevel;


    //Constructor
    protected Admin(String username,String email,String passwordHash,int adminLevel){
        super(username,email,passwordHash);
        if (adminLevel<1||adminLevel>3) throw new IllegalArgumentException("PasswordLevel must be from 1 to 3");
        this.adminLevel=adminLevel;
    }

    //Getter
    @Override
    public String getRole(){
        return "ADMIN";
    }
    public int getAdminLevel(){
        return this.adminLevel;
    }
    public boolean canDeleteAdmin(){
        return adminLevel==3;
    }
    @Override
    public void printInfo() {
        super.printInfo();
        System.out.println("Admin Lvl: " + adminLevel);
    }

}
