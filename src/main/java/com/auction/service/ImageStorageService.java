package com.auction.service;

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
import java.util.Locale;
import java.util.UUID;

/**
 * Lưu ảnh người dùng upload vào thư mục dùng chung và public qua HTTP.
 */
public class ImageStorageService {

    public static final int DEFAULT_PORT = Integer.getInteger("auction.media.port", 8081);

    private static final String DEFAULT_PUBLIC_HOST = System.getProperty("auction.server.host", "127.0.0.1");
    private static final Path DEFAULT_STORAGE_ROOT = Path.of(
            System.getProperty("auction.media.dir", "uploads/images"));

    private final Path storageRoot;
    private final String publicHost;
    private final int publicPort;

    private HttpServer httpServer;

    public ImageStorageService() {
        this(DEFAULT_STORAGE_ROOT, System.getProperty("auction.public.host", DEFAULT_PUBLIC_HOST), DEFAULT_PORT);
    }

    public ImageStorageService(Path storageRoot, String publicHost, int publicPort) {
        this.storageRoot = storageRoot.toAbsolutePath().normalize();
        this.publicHost = normalizeHost(publicHost);
        this.publicPort = publicPort;
    }

    public synchronized void start() throws IOException {
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
            throw new IOException("Ảnh tải lên bị rỗng");
        }

        Files.createDirectories(storageRoot);
        String extension = resolveExtension(originalFileName);
        String storedFileName = UUID.randomUUID() + extension;
        Path target = storageRoot.resolve(storedFileName).normalize();
        if (!target.startsWith(storageRoot)) {
            throw new IOException("Đường dẫn lưu ảnh không hợp lệ");
        }
        Files.write(target, content);
        return buildPublicUrl(storedFileName);
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
