package com.auction.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Stores uploaded images and returns a URL that every connected client can load.
 * Cloudinary is used when configured; local HTTP storage remains as a fallback.
 */
/**
 * ImageStorageService
 *
 * Chức năng:
 * - Lưu ảnh sản phẩm đấu giá.
 * - Upload ảnh lên Cloudinary.
 * - Fallback sang lưu local nếu Cloudinary không khả dụng.
 * - Tạo URL công khai cho client truy cập.
 * - Khởi tạo HTTP Server phục vụ ảnh local.
 * Kiến trúc:
 * Client->Server->ImageStorageService(Cloudinary+Local Storage)
 */
public class ImageStorageService {
    /**
     * Port mặc định của HTTP Image Server.
     * Ví dụ:
     * http://localhost:8081/images/abc.jpg
     */
    public static final int DEFAULT_PORT = Integer.getInteger("auction.media.port", 8081);

    private static final String DEFAULT_PUBLIC_HOST = System.getProperty("auction.server.host", "127.0.0.1");
    private static final Path DEFAULT_STORAGE_ROOT = Path.of(
            System.getProperty("auction.media.dir", "uploads/images"));
    /**
     * Thư mục lưu ảnh local.
     *
     * Mặc định:
     * uploads/images
     */
    private final Path storageRoot;
    private final String publicHost;
    private final int publicPort;
    /**
     * Đối tượng upload ảnh lên Cloudinary.
     * Nếu null:dùng local storage
     * Nếu tồn tại:upload lên Cloudinary
     */
    private final CloudImageUploader cloudImageUploader;

    private HttpServer httpServer;

    public ImageStorageService() {
        this(DEFAULT_STORAGE_ROOT, System.getProperty("auction.public.host", DEFAULT_PUBLIC_HOST), DEFAULT_PORT,
                CloudinaryImageUploader.fromEnvironment());
    }

    public ImageStorageService(Path storageRoot, String publicHost, int publicPort) {
        this(storageRoot, publicHost, publicPort, null);
    }

    ImageStorageService(Path storageRoot, String publicHost, int publicPort, CloudImageUploader cloudImageUploader) {
        this.storageRoot = storageRoot.toAbsolutePath().normalize();
        this.publicHost = normalizeHost(publicHost);
        this.publicPort = publicPort;
        this.cloudImageUploader = cloudImageUploader;
    }
    /**
     * Khởi động HTTP Server phục vụ ảnh local.
     * Chỉ chạy khi:Cloudinary không được cấu hình.
     * URL:http://host:port/images/file.jpg
     */
    public synchronized void start() throws IOException {
        if (isCloudinaryEnabled()) {
            return;
        }
        if (httpServer != null) {
            return;
        }
        Files.createDirectories(storageRoot);
        httpServer = HttpServer.create(new InetSocketAddress(publicPort), 0);
        httpServer.createContext("/images/", new ImageHttpHandler(storageRoot));
        httpServer.setExecutor(null);
        httpServer.start();
    }

    public synchronized void stop() {
        if (httpServer != null) {
            httpServer.stop(0);
            httpServer = null;
        }
    }

    public String storeImage(byte[] content, String originalFileName) throws IOException {
        if (content == null || content.length == 0) {
            throw new IOException("Anh tai len bi rong");
        }

        if (isCloudinaryEnabled()) {
            return cloudImageUploader.upload(content, originalFileName);
        }

        Files.createDirectories(storageRoot);
        String extension = resolveExtension(originalFileName);
        String storedFileName = UUID.randomUUID() + extension;
        Path target = storageRoot.resolve(storedFileName).normalize();
        if (!target.startsWith(storageRoot)) {
            throw new IOException("Duong dan luu anh khong hop le");
        }
        Files.write(target, content);
        return buildPublicUrl(storedFileName);
    }

    private boolean isCloudinaryEnabled() {
        return cloudImageUploader != null;
    }

    String buildPublicUrl(String storedFileName) {
        return "http://" + publicHost + ":" + publicPort + "/images/" + storedFileName;
    }

    private String resolveExtension(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            return ".bin";
        }
        int dotIndex = originalFileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == originalFileName.length() - 1) {
            return ".bin";
        }
        String extension = originalFileName.substring(dotIndex).toLowerCase(Locale.ROOT);
        return extension.matches("\\.[a-z0-9]{1,10}") ? extension : ".bin";
    }

    private static String normalizeHost(String host) {
        if (host == null || host.isBlank()) {
            return DEFAULT_PUBLIC_HOST;
        }
        return host.trim();
    }

    @FunctionalInterface
    interface CloudImageUploader {
        String upload(byte[] content, String originalFileName) throws IOException;
    }

    private static final class CloudinaryImageUploader implements CloudImageUploader {
        private static final String DEFAULT_FOLDER = "auction-items";
        private static final int MAX_DELIVERY_WIDTH = 1200;

        private final Cloudinary cloudinary;
        private final String folder;

        private CloudinaryImageUploader(Cloudinary cloudinary, String folder) {
            this.cloudinary = cloudinary;
            this.folder = folder;
        }

        private static CloudinaryImageUploader fromEnvironment() {
            Map<String, String> config = loadLocalEnv();
            String cloudinaryUrl = getConfig(config, "CLOUDINARY_URL", "cloudinary.url");
            String cloudName = getConfig(config, "CLOUDINARY_CLOUD_NAME", "cloudinary.cloudName");
            String apiKey = getConfig(config, "CLOUDINARY_API_KEY", "cloudinary.apiKey");
            String apiSecret = getConfig(config, "CLOUDINARY_API_SECRET", "cloudinary.apiSecret");
            String folder = getConfig(config, "CLOUDINARY_FOLDER", "cloudinary.folder");
            if (folder == null || folder.isBlank()) {
                folder = DEFAULT_FOLDER;
            }

            if (cloudinaryUrl != null && !cloudinaryUrl.isBlank()) {
                return new CloudinaryImageUploader(new Cloudinary(cloudinaryUrl), folder);
            }
            if (cloudName != null && !cloudName.isBlank()
                    && apiKey != null && !apiKey.isBlank()
                    && apiSecret != null && !apiSecret.isBlank()) {
                return new CloudinaryImageUploader(new Cloudinary(ObjectUtils.asMap(
                        "cloud_name", cloudName,
                        "api_key", apiKey,
                        "api_secret", apiSecret,
                        "secure", true)), folder);
            }
            return null;
        }

        @Override
        @SuppressWarnings("rawtypes")
        public String upload(byte[] content, String originalFileName) throws IOException {
            try {
                Transformation deliveryTransformation = optimizedDeliveryTransformation();
                Map result = cloudinary.uploader().upload(content, ObjectUtils.asMap(
                        "folder", folder,
                        "resource_type", "image",
                        "use_filename", true,
                        "unique_filename", true,
                        "filename_override", originalFileName,
                        "eager", List.of(deliveryTransformation)));
                return buildOptimizedDeliveryUrl(result, deliveryTransformation);
            } catch (IOException e) {
                throw e;
            } catch (Exception e) {
                throw new IOException("Upload anh len Cloudinary that bai: " + e.getMessage(), e);
            }
        }

        private String buildOptimizedDeliveryUrl(Map result, Transformation deliveryTransformation) throws IOException {
            Object publicId = result.get("public_id");
            if (publicId != null && !String.valueOf(publicId).isBlank()) {
                return cloudinary.url()
                        .secure(true)
                        .transformation(deliveryTransformation)
                        .generate(String.valueOf(publicId));
            }

            Object secureUrl = result.get("secure_url");
            if (secureUrl == null || String.valueOf(secureUrl).isBlank()) {
                throw new IOException("Cloudinary khong tra ve secure_url");
            }
            return String.valueOf(secureUrl);
        }

        private Transformation optimizedDeliveryTransformation() {
            return new Transformation()
                    .width(MAX_DELIVERY_WIDTH)
                    .crop("limit")
                    .quality("auto");
        }
    }

    private static String getConfig(Map<String, String> localEnv, String envKey, String propertyKey) {
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }
        String localValue = localEnv.get(envKey);
        if (localValue != null && !localValue.isBlank()) {
            return localValue;
        }
        String propertyValue = System.getProperty(propertyKey);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue;
        }
        return null;
    }

    private static Map<String, String> loadLocalEnv() {
        for (Path envFile : getLocalEnvCandidates()) {
            if (Files.isRegularFile(envFile)) {
                return readLocalEnv(envFile);
            }
        }
        return Map.of();
    }

    private static Map<String, String> readLocalEnv(Path envFile) {
        Map<String, String> values = new HashMap<>();
        try {
            for (String rawLine : Files.readAllLines(envFile)) {
                String line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int separatorIndex = line.indexOf('=');
                if (separatorIndex <= 0) {
                    continue;
                }
                String key = line.substring(0, separatorIndex).trim();
                String value = stripQuotes(line.substring(separatorIndex + 1).trim());
                if (!key.isEmpty() && !value.isEmpty()) {
                    values.put(key, value);
                }
            }
        } catch (IOException e) {
            System.err.println("[ImageStorage] Khong the doc file " + envFile + ": " + e.getMessage());
        }
        return Map.copyOf(values);
    }

    private static Iterable<Path> getLocalEnvCandidates() {
        Map<Path, Boolean> candidates = new LinkedHashMap<>();
        addLocalEnvCandidates(candidates, Path.of(System.getProperty("user.dir", ".")));
        addLocalEnvCandidates(candidates, Path.of("").toAbsolutePath());
        return candidates.keySet();
    }

    private static void addLocalEnvCandidates(Map<Path, Boolean> candidates, Path start) {
        Path current = start.toAbsolutePath().normalize();
        if (Files.isRegularFile(current)) {
            current = current.getParent();
        }

        while (current != null) {
            candidates.put(current.resolve(".env.local"), true);
            current = current.getParent();
        }
    }

    private static String stripQuotes(String value) {
        if (value.length() >= 2) {
            boolean wrappedInDoubleQuotes = value.startsWith("\"") && value.endsWith("\"");
            boolean wrappedInSingleQuotes = value.startsWith("'") && value.endsWith("'");
            if (wrappedInDoubleQuotes || wrappedInSingleQuotes) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }

    private static final class ImageHttpHandler implements HttpHandler {
        private final Path storageRoot;

        private ImageHttpHandler(Path storageRoot) {
            this.storageRoot = storageRoot;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                exchange.close();
                return;
            }

            String requestPath = exchange.getRequestURI().getPath();
            String fileName = requestPath.substring("/images/".length());
            if (fileName.isBlank()) {
                exchange.sendResponseHeaders(404, -1);
                exchange.close();
                return;
            }

            Path target = storageRoot.resolve(fileName).normalize();
            if (!target.startsWith(storageRoot) || !Files.exists(target) || Files.isDirectory(target)) {
                exchange.sendResponseHeaders(404, -1);
                exchange.close();
                return;
            }

            String contentType = Files.probeContentType(target);
            if (contentType == null) {
                contentType = URLConnection.guessContentTypeFromName(target.getFileName().toString());
            }
            if (contentType != null) {
                exchange.getResponseHeaders().set("Content-Type", contentType);
            }
            long size = Files.size(target);
            exchange.sendResponseHeaders(200, size);
            try (InputStream in = Files.newInputStream(target);
                 OutputStream out = exchange.getResponseBody()) {
                in.transferTo(out);
            }
        }
    }
}
