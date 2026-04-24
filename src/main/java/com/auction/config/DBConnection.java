package com.auction.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DBConnection {
    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3307/auctions_db";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "123456";
    private static final Map<String, String> LOCAL_ENV = loadLocalEnv();

    private DBConnection() {
        throw new IllegalStateException("Utility class");
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(getUrl(), getUser(), getPassword());
    }

    public static String getConfiguredUrl() {
        return getUrl();
    }

    public static String getConfiguredUser() {
        return getUser();
    }

    private static String getUrl() {
        return getConfig("DB_URL", "db.url", DEFAULT_URL);
    }

    private static String getUser() {
        return getConfig("DB_USER", "db.user", DEFAULT_USER);
    }

    private static String getPassword() {
        return getConfig("DB_PASSWORD", "db.password", DEFAULT_PASSWORD);
    }

    private static String getConfig(String envKey, String propertyKey, String defaultValue) {
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }

        String localEnvValue = LOCAL_ENV.get(envKey);
        if (localEnvValue != null && !localEnvValue.isBlank()) {
            return localEnvValue;
        }

        String propertyValue = System.getProperty(propertyKey);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue;
        }

        return defaultValue;
    }

    private static Map<String, String> loadLocalEnv() {
        Path envFile = Path.of(".env.local");
        if (!Files.exists(envFile)) {
            return Map.of();
        }

        Map<String, String> values = new HashMap<>();
        try {
            List<String> lines = Files.readAllLines(envFile);
            for (String rawLine : lines) {
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
            System.err.println("[DB] Failed to read .env.local: " + e.getMessage());
        }

        return Map.copyOf(values);
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
}
