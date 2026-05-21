package com.auction.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void storeImageWritesFileAndReturnsPublicUrl() throws Exception {
        ImageStorageService storageService = new ImageStorageService(tempDir, "100.64.0.10", 8081);

        String imageUrl = storageService.storeImage(new byte[] {1, 2, 3}, "photo.JPG");

        assertTrue(imageUrl.startsWith("http://100.64.0.10:8081/images/"));
        assertTrue(imageUrl.endsWith(".jpg"));

        String storedFileName = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);
        Path storedFile = tempDir.resolve(storedFileName);
        assertTrue(Files.exists(storedFile));
        assertEquals(3L, Files.size(storedFile));
    }

    @Test
    void storeImageReturnsCloudinaryUrlWhenUploaderIsConfigured() throws Exception {
        ImageStorageService storageService = new ImageStorageService(
                tempDir, "100.64.0.10", 8081,
                (content, originalFileName) -> "https://res.cloudinary.com/demo/image/upload/auction-items/photo.jpg");

        String imageUrl = storageService.storeImage(new byte[] {1, 2, 3}, "photo.JPG");

        assertEquals("https://res.cloudinary.com/demo/image/upload/auction-items/photo.jpg", imageUrl);
    }
}
