package com.auction.util;

import javafx.scene.image.Image;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AuctionImageLoader
 *
 * Chức năng:
 * - Tải ảnh sản phẩm đấu giá từ URL
 * - Tối ưu URL Cloudinary trước khi tải
 * - Cache ảnh để tránh tải lại nhiều lần
 * - Hỗ trợ thumbnail và ảnh chi tiết
 *
 * Lợi ích:
 * - Giảm băng thông
 * - Tăng tốc hiển thị giao diện JavaFX
 * - Tránh UI bị lag khi có nhiều sản phẩm
 */
public final class AuctionImageLoader {

    /** Số lượng ảnh tối đa lưu trong cache */
    private static final int MAX_CACHE_ENTRIES = 160;

    /**
     * Marker dùng để xác định URL Cloudinary
     *
     * Ví dụ:
     * https://res.cloudinary.com/.../image/upload/abc.jpg
     */
    private static final String CLOUDINARY_UPLOAD_MARKER = "/image/upload/";

    /**
     * Cache ảnh theo cơ chế LRU (Least Recently Used)
     *
     * accessOrder = true:
     * ảnh nào được truy cập gần nhất sẽ được giữ lại.
     */
    private static final Map<String, Image> CACHE =
            new LinkedHashMap<>(MAX_CACHE_ENTRIES, 0.75f, true) {

                /**
                 * Tự động xóa ảnh cũ nhất khi cache vượt quá giới hạn.
                 */
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Image> eldest) {
                    return size() > MAX_CACHE_ENTRIES;
                }
            };

    /**
     * Utility class
     * Không cho phép khởi tạo object.
     */
    private AuctionImageLoader() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Tải ảnh dạng thumbnail
     *
     * Dùng cho:
     * - Card sản phẩm
     * - Danh sách đấu giá
     */
    public static Image thumbnail(String imageUrl) {
        return load(imageUrl, 360, 220, "c_limit,q_auto,w_360");
    }

    /**
     * Tải ảnh kích thước lớn
     *
     * Dùng cho:
     * - Trang chi tiết sản phẩm
     */
    public static Image detail(String imageUrl) {
        return load(imageUrl, 1200, 700, "c_limit,q_auto,w_1200");
    }

    /**
     * Hàm tải ảnh chính
     *
     * Bước 1: Tối ưu URL Cloudinary
     * Bước 2: Tạo cache key
     * Bước 3: Kiểm tra cache
     * Bước 4: Nếu chưa có thì tải mới
     */
    private static Image load(
            String imageUrl,
            double requestedWidth,
            double requestedHeight,
            String transformation
    ) {

        // URL sau khi được Cloudinary tối ưu
        String optimizedUrl =
                optimizeCloudinaryUrl(imageUrl, transformation);

        // Khóa duy nhất của ảnh trong cache
        String cacheKey =
                optimizedUrl + "|" +
                        (int) requestedWidth +
                        "x" +
                        (int) requestedHeight;

        synchronized (CACHE) {

            /*
             * computeIfAbsent():
             *
             * Nếu ảnh đã có trong cache:
             *      trả luôn
             *
             * Nếu chưa có:
             *      tạo Image mới
             *      lưu cache
             *      trả kết quả
             */
            return CACHE.computeIfAbsent(
                    cacheKey,
                    key -> new Image(
                            optimizedUrl,
                            requestedWidth,
                            requestedHeight,

                            // Giữ đúng tỉ lệ ảnh
                            true,

                            // Làm mượt ảnh
                            true,

                            // Tải nền (không block UI)
                            true
                    )
            );
        }
    }

    /**
     * Chèn transformation của Cloudinary vào URL.
     *
     * Ví dụ:
     *
     * Gốc:
     * /image/upload/abc.jpg
     *
     * Sau xử lý:
     * /image/upload/c_limit,q_auto,w_360/abc.jpg
     */
    static String optimizeCloudinaryUrl(
            String imageUrl,
            String transformation
    ) {

        if (imageUrl == null
                || imageUrl.isBlank()
                || !imageUrl.contains(CLOUDINARY_UPLOAD_MARKER)) {
            return imageUrl;
        }

        int uploadIndex =
                imageUrl.indexOf(CLOUDINARY_UPLOAD_MARKER);

        int contentStart =
                uploadIndex + CLOUDINARY_UPLOAD_MARKER.length();

        String prefix =
                imageUrl.substring(0, contentStart);

        String rest =
                imageUrl.substring(contentStart);

        int firstSlash = rest.indexOf('/');

        /*
         * Nếu URL đã có transformation
         * thì bỏ transformation cũ
         */
        if (firstSlash > 0
                && looksLikeTransformation(
                rest.substring(0, firstSlash))) {

            rest = rest.substring(firstSlash + 1);
        }

        return prefix + transformation + "/" + rest;
    }

    /**
     * Kiểm tra xem đoạn URL có phải
     * transformation của Cloudinary không.
     *
     * Ví dụ:
     * w_300,h_200,c_fill
     */
    private static boolean looksLikeTransformation(String segment) {

        return segment.contains(",")
                && (
                segment.contains("w_")
                        || segment.contains("h_")
                        || segment.contains("c_")
                        || segment.contains("q_")
                        || segment.contains("f_")
        );
    }
}