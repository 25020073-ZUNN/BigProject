package com.auction.factory;

import com.auction.model.item.Art;
import com.auction.model.item.Electronics;
import com.auction.model.item.Item;
import com.auction.model.item.Vehicle;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * ItemFactory - Lớp Factory (Mẫu thiết kế Factory) dùng để khởi tạo các đối tượng tài sản.
 * Dựa trên loại tài sản (Electronics, Vehicle, Art), lớp này sẽ tạo ra đối tượng cụ thể tương ứng.
 */
public final class ItemFactory {

    // Constructor riêng tư để ngăn việc khởi tạo đối tượng của lớp Factory này
    private ItemFactory() {
    }

    /**
     * Phương thức chính để tạo một đối tượng Item.
     * 
     * @param itemType Loại tài sản (Ví dụ: "Electronics", "Vehicle", "Art")
     * @param name Tên tài sản
     * @param description Mô tả tài sản
     * @param startingPrice Giá khởi điểm
     * @param startTime Thời gian bắt đầu đấu giá
     * @param endTime Thời gian kết thúc đấu giá
     * @param sellerId ID của người bán
     * @param attributes Map chứa các thuộc tính đặc thù tùy theo loại tài sản
     * @return Đối tượng cụ thể (Electronics, Vehicle, hoặc Art) kế thừa từ lớp Item
     */
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
        // Kiểm tra loại tài sản không được để trống
        if (itemType == null || itemType.isBlank()) {
            throw new IllegalArgumentException("LOẠI TÀI SẢN KHÔNG ĐƯỢC ĐỂ TRỐNG");
        }

        // Đảm bảo map thuộc tính không bị null để tránh lỗi NullPointerException
        Map<String, Object> safeAttributes = attributes == null ? Map.of() : attributes;

        // Sử dụng Switch expression (Java 14+) để quyết định loại đối tượng cần tạo
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
            default -> throw new IllegalArgumentException("LOẠI TÀI SẢN KHÔNG ĐƯỢC HỖ TRỢ: " + itemType);
        };
    }

    /**
     * Tạo đối tượng Đồ điện tử (Electronics).
     */
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

    /**
     * Tạo đối tượng Phương tiện (Vehicle).
     */
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

    /**
     * Tạo đối tượng Tác phẩm nghệ thuật (Art).
     */
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

    /**
     * Hàm tiện ích để lấy một chuỗi bắt buộc từ map thuộc tính.
     */
    private static String getRequiredString(Map<String, Object> attributes, String key) {
        Object value = attributes.get(key);
        if (!(value instanceof String stringValue) || stringValue.isBlank()) {
            throw new IllegalArgumentException("THUỘC TÍNH BẮT BUỘC BỊ THIẾU HOẶC KHÔNG HỢP LỆ: " + key);
        }
        return stringValue;
    }

    /**
     * Hàm tiện ích để lấy một số nguyên bắt buộc từ map thuộc tính.
     */
    private static int getRequiredInt(Map<String, Object> attributes, String key) {
        Object value = attributes.get(key);
        if (value instanceof Number numberValue) {
            return numberValue.intValue();
        }
        throw new IllegalArgumentException("THUỘC TÍNH BẮT BUỘC BỊ THIẾU HOẶC KHÔNG HỢP LỆ: " + key);
    }
}
/*ItemFactory áp dụng Factory Pattern để tập trung việc khởi tạo các loại tài sản đấu giá như Electronics, Vehicle và Art.
Thay vì Controller tạo object trực tiếp, toàn bộ logic tạo được gom vào Factory giúp giảm phụ thuộc, dễ mở rộng và tận dụng đa hình thông qua lớp cha Item.*/