package com.auction.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Lớp kiểm thử đơn vị cho dịch vụ lưu trữ hình ảnh (ImageStorageService).
 * Kiểm thử cả hai cơ chế: lưu trữ hình ảnh cục bộ (trong thư mục tạm thời hệ thống)
 * và tải hình ảnh lên Cloudinary khi cấu hình API uploader được cung cấp.
 */
class ImageStorageServiceTest {

    // Tạo thư mục tạm thời tự động xóa sau khi chạy xong mỗi test case
    @TempDir
    Path tempDir;

    /**
     * Kiểm thử trường hợp: Lưu trữ hình ảnh cục bộ (Local Storage).
     * Ghi tệp ảnh vào thư mục tạm và trả về đường dẫn URL công khai có dạng http.
     */
    @Test
    void storeImageWritesFileAndReturnsPublicUrl() throws Exception {
        ImageStorageService storageService = new ImageStorageService(tempDir, "100.64.0.10", 8081);

        // Thực hiện ghi dữ liệu mảng byte (1, 2, 3) giả lập ảnh photo.JPG
        String imageUrl = storageService.storeImage(new byte[] {1, 2, 3}, "photo.JPG");

        // Xác nhận URL trả về trỏ đúng IP/cổng và tệp tin đích đã được chuyển đổi phần mở rộng thành .jpg viết thường
        assertTrue(imageUrl.startsWith("http://100.64.0.10:8081/images/"));
        assertTrue(imageUrl.endsWith(".jpg"));

        // Xác nhận tệp tin thực sự được tạo ra trên đĩa cứng và có kích thước đúng bằng 3 bytes
        String storedFileName = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);
        Path storedFile = tempDir.resolve(storedFileName);
        assertTrue(Files.exists(storedFile));
        assertEquals(3L, Files.size(storedFile));
    }

    /**
     * Kiểm thử trường hợp: Tải lên Cloudinary thành công
     * khi uploader chức năng được định nghĩa (Cloudinary integration).
     */
    @Test
    void storeImageReturnsCloudinaryUrlWhenUploaderIsConfigured() throws Exception {
        // Giả lập hàm uploader của Cloudinary bằng Lambda trả về một URL tĩnh cố định
        ImageStorageService storageService = new ImageStorageService(
                tempDir, "100.64.0.10", 8081,
                (content, originalFileName) -> "https://res.cloudinary.com/demo/image/upload/auction-items/photo.jpg");

        // Gửi lệnh lưu ảnh
        String imageUrl = storageService.storeImage(new byte[] {1, 2, 3}, "photo.JPG");

        // Xác nhận URL trả về chính là URL giả lập do Cloudinary uploader cung cấp
        assertEquals("https://res.cloudinary.com/demo/image/upload/auction-items/photo.jpg", imageUrl);
    }
}
