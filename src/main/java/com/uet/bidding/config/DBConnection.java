package com.uet.bidding.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DBConnection {
    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3307/auctions_db";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "123456";

    private DBConnection() {
        throw new IllegalStateException("Utility class");
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(getUrl(), getUser(), getPassword());
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

        String propertyValue = System.getProperty(propertyKey);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue;
        }

        return defaultValue;
    }
}
