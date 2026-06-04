package com.auction.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Lớp kiểm thử đơn vị cho AuctionImageLoader.
 * Kiểm tra các tính năng tối ưu hóa chuỗi URL hình ảnh của Cloudinary bằng cách thêm
 * hoặc thay thế các tham số chuyển đổi (transformation) cho ảnh, trong khi bỏ qua các URL không thuộc Cloudinary.
 */
class AuctionImageLoaderTest {

    /**
     * Kiểm thử trường hợp: Thêm tham số tối ưu hóa hình ảnh (như tỉ lệ scale, chất lượng tự động)
     * vào một URL Cloudinary gốc chưa có tham số chuyển đổi.
     */
    @Test
    void optimizeCloudinaryUrlAddsTransformationToOriginalUrl() {
        String url = "https://res.cloudinary.com/demo/image/upload/auction-items/photo.jpg";

        // Thêm tham số cấu hình: giới hạn scale, chất lượng tự động, chiều rộng 360px
        String optimized = AuctionImageLoader.optimizeCloudinaryUrl(url, "c_limit,q_auto,w_360");

        // Xác nhận URL được tối ưu chứa tham số cấu hình ở vị trí chính xác
        assertEquals("https://res.cloudinary.com/demo/image/upload/c_limit,q_auto,w_360/auction-items/photo.jpg",
                optimized);
    }

    /**
     * Kiểm thử trường hợp: Thay thế tham số tối ưu hóa cũ của Cloudinary bằng tham số mới
     * nếu URL đầu vào đã chứa sẵn cấu hình chuyển đổi trước đó.
     */
    @Test
    void optimizeCloudinaryUrlReplacesExistingFirstTransformation() {
        String url = "https://res.cloudinary.com/demo/image/upload/c_limit,q_auto,w_1200/auction-items/photo.jpg";

        // Yêu cầu chuyển đổi mới với độ rộng 360px thay vì 1200px
        String optimized = AuctionImageLoader.optimizeCloudinaryUrl(url, "c_limit,q_auto,w_360");

        // Xác nhận tham số w_1200 đã bị thay thế hoàn toàn bởi w_360
        assertEquals("https://res.cloudinary.com/demo/image/upload/c_limit,q_auto,w_360/auction-items/photo.jpg",
                optimized);
    }

    /**
     * Kiểm thử trường hợp: Không thay đổi bất kỳ ký tự nào đối với các URL ảnh cục bộ (Local URL)
     * hoặc các dịch vụ lưu trữ khác không thuộc hệ thống Cloudinary.
     */
    @Test
    void optimizeCloudinaryUrlLeavesNonCloudinaryUrlsAlone() {
        String url = "http://127.0.0.1:8081/images/photo.jpg";

        String optimized = AuctionImageLoader.optimizeCloudinaryUrl(url, "c_limit,q_auto,w_360");

        // Xác nhận URL cục bộ được giữ nguyên vẹn
        assertEquals(url, optimized);
    }
}

