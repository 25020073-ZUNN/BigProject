package com.auction.util;

import javafx.scene.image.Image;

import java.util.LinkedHashMap;
import java.util.Map;

public final class AuctionImageLoader {
    private static final int MAX_CACHE_ENTRIES = 160;
    private static final String CLOUDINARY_UPLOAD_MARKER = "/image/upload/";

    private static final Map<String, Image> CACHE = new LinkedHashMap<>(MAX_CACHE_ENTRIES, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Image> eldest) {
            return size() > MAX_CACHE_ENTRIES;
        }
    };

    private AuctionImageLoader() {
        throw new IllegalStateException("Utility class");
    }

    public static Image thumbnail(String imageUrl) {
        return load(imageUrl, 360, 220, "c_limit,q_auto,w_360");
    }

    public static Image detail(String imageUrl) {
        return load(imageUrl, 1200, 700, "c_limit,q_auto,w_1200");
    }

    private static Image load(String imageUrl, double requestedWidth, double requestedHeight, String transformation) {
        String optimizedUrl = optimizeCloudinaryUrl(imageUrl, transformation);
        String cacheKey = optimizedUrl + "|" + (int) requestedWidth + "x" + (int) requestedHeight;
        synchronized (CACHE) {
            return CACHE.computeIfAbsent(cacheKey,
                    key -> new Image(optimizedUrl, requestedWidth, requestedHeight, true, true, true));
        }
    }

    static String optimizeCloudinaryUrl(String imageUrl, String transformation) {
        if (imageUrl == null || imageUrl.isBlank() || !imageUrl.contains(CLOUDINARY_UPLOAD_MARKER)) {
            return imageUrl;
        }

        int uploadIndex = imageUrl.indexOf(CLOUDINARY_UPLOAD_MARKER);
        int contentStart = uploadIndex + CLOUDINARY_UPLOAD_MARKER.length();
        String prefix = imageUrl.substring(0, contentStart);
        String rest = imageUrl.substring(contentStart);

        int firstSlash = rest.indexOf('/');
        if (firstSlash > 0 && looksLikeTransformation(rest.substring(0, firstSlash))) {
            rest = rest.substring(firstSlash + 1);
        }

        return prefix + transformation + "/" + rest;
    }

    private static boolean looksLikeTransformation(String segment) {
        return segment.contains(",")
                && (segment.contains("w_")
                || segment.contains("h_")
                || segment.contains("c_")
                || segment.contains("q_")
                || segment.contains("f_"));
    }
}
