package com.auction.model.item;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Item loại phương tiện
 */
public class Vehicle extends Item {

    private String manufacturer;  // Hãng xe
    private int year;             // Năm sản xuất
    private int mileage;          // Số km đã đi

    public Vehicle(String name, String description, BigDecimal startingPrice,
                   LocalDateTime startTime, LocalDateTime endTime, String sellerId,
                   String manufacturer, int year, int mileage) {

        super(name, description, startingPrice, startTime, endTime, sellerId);
        this.manufacturer = manufacturer;
        this.year = year;
        this.mileage = mileage;
    }

    @Override
    public String getCategory() {
        return "Vehicle";
    }

    @Override
    public void printInfo() {
        System.out.println("🚗 Vehicle: " + name
                + " | Maker: " + manufacturer
                + " | Price: " + currentPrice);
    }
}