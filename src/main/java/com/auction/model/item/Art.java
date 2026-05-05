package com.auction.model.item;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.logging.Logger;

/**
 * Item loại nghệ thuật
 */
public class Art extends Item {
    private static final Logger LOGGER = Logger.getLogger(Art.class.getName());

    private String artist;     // Tác giả
    private int yearCreated;   // Năm sáng tác

    public Art(String name, String description, BigDecimal startingPrice,
               LocalDateTime startTime, LocalDateTime endTime, String sellerId,
               String artist, int yearCreated) {

        super(name, description, startingPrice, startTime, endTime, sellerId);
        this.artist = artist;
        this.yearCreated = yearCreated;
    }

    @Override
    public String getCategory() {
        return "Art";
    }

    @Override
    public void printInfo() {
        LOGGER.info(() -> "Art: " + name
                + " | Artist: " + artist
                + " | Price: " + currentPrice);
    }
}
