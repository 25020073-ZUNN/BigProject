package com.auction.factory;

import com.auction.model.item.Art;
import com.auction.model.item.Item;
import com.auction.model.item.Vehicle;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ItemFactoryTest {

    @Test
    void createItemBuildsVehicleFromAttributes() {
        Item item = ItemFactory.createItem(
                "vehicle",
                "Mazda 3",
                "Used car",
                new BigDecimal("500000000"),
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(2),
                "seller-001",
                Map.of(
                        "manufacturer", "Mazda",
                        "year", 2021,
                        "mileage", 35000
                )
        );

        assertInstanceOf(Vehicle.class, item);
        assertEquals("Vehicle", item.getCategory());
    }

    @Test
    void createItemBuildsArtFromAttributes() {
        Item item = ItemFactory.createItem(
                "art",
                "Painting",
                "Oil on canvas",
                new BigDecimal("10000000"),
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(2),
                "seller-001",
                Map.of(
                        "artist", "Unknown",
                        "yearCreated", 2020
                )
        );

        assertInstanceOf(Art.class, item);
        assertEquals("Art", item.getCategory());
    }

    @Test
    void createItemRejectsMissingAttributes() {
        assertThrows(IllegalArgumentException.class, () -> ItemFactory.createItem(
                "electronics",
                "Phone",
                "Missing brand",
                new BigDecimal("1000"),
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusHours(1),
                "seller-001",
                Map.of("warrantyMonths", 12)
        ));
    }
}
