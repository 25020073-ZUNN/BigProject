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

/**
 * Lớp kiểm thử đơn vị cho ItemFactory.
 * Đảm bảo Factory Design Pattern hoạt động chính xác khi khởi tạo các loại đối tượng
 * tài sản khác nhau (Vehicle, Art, Electronics) từ cấu hình Map động.
 */
class ItemFactoryTest {

    /**
     * Kiểm thử trường hợp: Khởi tạo thành công đối tượng Xe cộ (Vehicle)
     * từ các thuộc tính đầu vào (manufacturer, year, mileage).
     */
    @Test
    void createItemBuildsVehicleFromAttributes() {
        // Tạo đối tượng Item kiểu "vehicle" thông qua Factory
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

        // Xác nhận đối tượng tạo ra thuộc lớp Vehicle và thuộc danh mục "Vehicle"
        assertInstanceOf(Vehicle.class, item);
        assertEquals("Vehicle", item.getCategory());
    }

    /**
     * Kiểm thử trường hợp: Khởi tạo thành công đối tượng Tác phẩm nghệ thuật (Art)
     * từ các thuộc tính đầu vào (artist, yearCreated).
     */
    @Test
    void createItemBuildsArtFromAttributes() {
        // Tạo đối tượng Item kiểu "art" thông qua Factory
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

        // Xác nhận đối tượng tạo ra thuộc lớp Art và thuộc danh mục "Art"
        assertInstanceOf(Art.class, item);
        assertEquals("Art", item.getCategory());
    }

    /**
     * Kiểm thử trường hợp: Ném ngoại lệ IllegalArgumentException
     * khi Map thuộc tính bị thiếu thông tin bắt buộc đối với loại sản phẩm (ví dụ: thiếu brand của đồ điện tử).
     */
    @Test
    void createItemRejectsMissingAttributes() {
        // Xác nhận hàm tạo ném ra ngoại lệ khi thiếu thuộc tính bắt buộc của "electronics" (thiếu brand)
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

