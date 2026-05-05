package com.auction.model.item;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.logging.Logger;

/**
 * Item loại điện tử
 */
public class Electronics extends Item {
    private static final Logger LOGGER = Logger.getLogger(Electronics.class.getName());

    private String brand;          // Hãng sản xuất
    private int warrantyMonths;    // Thời gian bảo hành

    public Electronics(String name, String description, BigDecimal startingPrice,
                       LocalDateTime startTime, LocalDateTime endTime, String sellerId,
                       String brand, int warrantyMonths) {

        super(name, description, startingPrice, startTime, endTime, sellerId);
        this.brand = brand;
        this.warrantyMonths = warrantyMonths;
    }

    @Override
    public String getCategory() {
        return "Electronics";
    }

    @Override
    public void printInfo() {
        LOGGER.info(() -> "Electronics: " + name
                + " | Brand: " + brand
                + " | Price: " + currentPrice);
    }
}
