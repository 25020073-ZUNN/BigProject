package com.auction.factory;

import com.auction.model.item.Art;
import com.auction.model.item.Electronics;
import com.auction.model.item.Item;
import com.auction.model.item.Vehicle;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public final class ItemFactory {

    private ItemFactory() {
    }

    public static Item createItem(
            String itemType,
            String name,
            String description,
            BigDecimal startingPrice,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String sellerId,
            Map<String, Object> attributes
    ) {
        if (itemType == null || itemType.isBlank()) {
            throw new IllegalArgumentException("ITEM TYPE MUST NOT BE BLANK");
        }

        Map<String, Object> safeAttributes = attributes == null ? Map.of() : attributes;

        return switch (itemType.trim().toLowerCase()) {
            case "electronics", "electronic" -> createElectronics(
                    name,
                    description,
                    startingPrice,
                    startTime,
                    endTime,
                    sellerId,
                    getRequiredString(safeAttributes, "brand"),
                    getRequiredInt(safeAttributes, "warrantyMonths")
            );
            case "vehicle", "car" -> createVehicle(
                    name,
                    description,
                    startingPrice,
                    startTime,
                    endTime,
                    sellerId,
                    getRequiredString(safeAttributes, "manufacturer"),
                    getRequiredInt(safeAttributes, "year"),
                    getRequiredInt(safeAttributes, "mileage")
            );
            case "art" -> createArt(
                    name,
                    description,
                    startingPrice,
                    startTime,
                    endTime,
                    sellerId,
                    getRequiredString(safeAttributes, "artist"),
                    getRequiredInt(safeAttributes, "yearCreated")
            );
            default -> throw new IllegalArgumentException("UNSUPPORTED ITEM TYPE: " + itemType);
        };
    }

    public static Electronics createElectronics(
            String name,
            String description,
            BigDecimal startingPrice,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String sellerId,
            String brand,
            int warrantyMonths
    ) {
        return new Electronics(
                name,
                description,
                startingPrice,
                startTime,
                endTime,
                sellerId,
                brand,
                warrantyMonths
        );
    }

    public static Vehicle createVehicle(
            String name,
            String description,
            BigDecimal startingPrice,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String sellerId,
            String manufacturer,
            int year,
            int mileage
    ) {
        return new Vehicle(
                name,
                description,
                startingPrice,
                startTime,
                endTime,
                sellerId,
                manufacturer,
                year,
                mileage
        );
    }

    public static Art createArt(
            String name,
            String description,
            BigDecimal startingPrice,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String sellerId,
            String artist,
            int yearCreated
    ) {
        return new Art(
                name,
                description,
                startingPrice,
                startTime,
                endTime,
                sellerId,
                artist,
                yearCreated
        );
    }

    private static String getRequiredString(Map<String, Object> attributes, String key) {
        Object value = attributes.get(key);
        if (!(value instanceof String stringValue) || stringValue.isBlank()) {
            throw new IllegalArgumentException("MISSING OR INVALID ATTRIBUTE: " + key);
        }
        return stringValue;
    }

    private static int getRequiredInt(Map<String, Object> attributes, String key) {
        Object value = attributes.get(key);
        if (value instanceof Number numberValue) {
            return numberValue.intValue();
        }
        throw new IllegalArgumentException("MISSING OR INVALID ATTRIBUTE: " + key);
    }
}
