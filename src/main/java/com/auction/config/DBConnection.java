package com.auction.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class DBConnection {
    private static final Logger LOGGER = Logger.getLogger(DBConnection.class.getName());
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
            LOGGER.log(Level.WARNING, "[DB] Failed to read .env.local", e);
        }

        return Map.copyOf(values);
    }

    private static Set<Path> getLocalEnvCandidates() {
        Set<Path> candidates = new LinkedHashSet<>();
        addLocalEnvCandidates(candidates, Path.of(System.getProperty("user.dir", ".")));
        addLocalEnvCandidates(candidates, Path.of("").toAbsolutePath());

        try {
            Path codeSource = Path.of(DBConnection.class.getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI());
            addLocalEnvCandidates(candidates, codeSource);
        } catch (Exception ignored) {
            // Fall back to user.dir candidates.
        }

        return candidates;
    }

    private static void addLocalEnvCandidates(Set<Path> candidates, Path start) {
        Path current = start.toAbsolutePath().normalize();
        if (Files.isRegularFile(current)) {
            current = current.getParent();
        }

        while (current != null) {
            candidates.add(current.resolve(".env.local"));
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
}
