package com.auction.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public final class LoggingConfig {

    private static final Logger LOGGER = Logger.getLogger(LoggingConfig.class.getName());
    private static final Path LOG_DIR = Path.of("logs");
    private static final String LOG_PATTERN = LOG_DIR.resolve("auction-app.%g.log").toString();

    private static volatile boolean configured;

    private LoggingConfig() {
    }

    public static synchronized void configure() {
        if (configured) {
            return;
        }

        try {
            Files.createDirectories(LOG_DIR);

            Logger rootLogger = Logger.getLogger("");
            rootLogger.setLevel(Level.INFO);
            rootLogger.setUseParentHandlers(false);

            for (var handler : rootLogger.getHandlers()) {
                rootLogger.removeHandler(handler);
            }

            Formatter formatter = new PlainTextFormatter();

            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setLevel(Level.INFO);
            consoleHandler.setFormatter(formatter);
            rootLogger.addHandler(consoleHandler);

            FileHandler fileHandler = new FileHandler(LOG_PATTERN, 1_000_000, 5, true);
            fileHandler.setLevel(Level.ALL);
            fileHandler.setFormatter(formatter);
            rootLogger.addHandler(fileHandler);

            configured = true;
            LOGGER.info(() -> "Logging configured at " + LOG_DIR.toAbsolutePath());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to configure logging", e);
        }
    }

    private static final class PlainTextFormatter extends Formatter {
        @Override
        public String format(LogRecord record) {
            String throwable = "";
            if (record.getThrown() != null) {
                throwable = "\n" + stackTrace(record.getThrown());
            }
            return String.format(
                    "%1$tF %1$tT [%2$s] %3$s - %4$s%5$s%n",
                    record.getMillis(),
                    record.getLevel().getName(),
                    record.getLoggerName(),
                    formatMessage(record),
                    throwable
            );
        }

        private String stackTrace(Throwable throwable) {
            StringBuilder builder = new StringBuilder(throwable.toString());
            for (StackTraceElement element : throwable.getStackTrace()) {
                builder.append("\n\tat ").append(element);
            }
            return builder.toString();
        }
    }
}
