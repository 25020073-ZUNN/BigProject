package com.auction.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuctionImageLoaderTest {

    @Test
    void optimizeCloudinaryUrlAddsTransformationToOriginalUrl() {
        String url = "https://res.cloudinary.com/demo/image/upload/auction-items/photo.jpg";

        String optimized = AuctionImageLoader.optimizeCloudinaryUrl(url, "c_limit,q_auto,w_360");

        assertEquals("https://res.cloudinary.com/demo/image/upload/c_limit,q_auto,w_360/auction-items/photo.jpg",
                optimized);
    }

    @Test
    void optimizeCloudinaryUrlReplacesExistingFirstTransformation() {
        String url = "https://res.cloudinary.com/demo/image/upload/c_limit,q_auto,w_1200/auction-items/photo.jpg";

        String optimized = AuctionImageLoader.optimizeCloudinaryUrl(url, "c_limit,q_auto,w_360");

        assertEquals("https://res.cloudinary.com/demo/image/upload/c_limit,q_auto,w_360/auction-items/photo.jpg",
                optimized);
    }

    @Test
    void optimizeCloudinaryUrlLeavesNonCloudinaryUrlsAlone() {
        String url = "http://127.0.0.1:8081/images/photo.jpg";

        String optimized = AuctionImageLoader.optimizeCloudinaryUrl(url, "c_limit,q_auto,w_360");

        assertEquals(url, optimized);
    }
}
