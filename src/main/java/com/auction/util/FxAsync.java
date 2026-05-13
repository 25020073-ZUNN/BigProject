package com.auction.util;

import javafx.application.Platform;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Tiện ích chạy tác vụ nặng (mạng, DB) ngoài luồng UI của JavaFX.
 * Kết quả hoặc lỗi được đẩy về luồng FX qua Platform.runLater.
 */
public final class FxAsync {

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "fx-async-worker");
        t.setDaemon(true);
        return t;
    });

    private FxAsync() {}

    /**
     * Interface cho tác vụ có thể ném checked exception.
     */
    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Exception;
    }

    /**
     * Chạy tác vụ trả về kết quả trên thread pool, callback trên FX thread.
     */
    public static <T> void run(Callable<T> task, Consumer<T> onSuccess, Consumer<String> onError) {
        EXECUTOR.submit(() -> {
            try {
                T result = task.call();
                Platform.runLater(() -> onSuccess.accept(result));
            } catch (Exception e) {
                String msg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
                Platform.runLater(() -> onError.accept(msg != null ? msg : "Lỗi không xác định"));
            }
        });
    }

    /**
     * Chạy tác vụ không có kết quả trả về trên thread pool (hỗ trợ checked exceptions).
     */
    public static void run(ThrowingRunnable task, Runnable onSuccess, Consumer<String> onError) {
        EXECUTOR.submit(() -> {
            try {
                task.run();
                Platform.runLater(onSuccess);
            } catch (Exception e) {
                String msg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
                Platform.runLater(() -> onError.accept(msg != null ? msg : "Lỗi không xác định"));
            }
        });
    }
}
