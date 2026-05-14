package com.auction.network.client;

import com.auction.network.Message;

import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Client helper to connect to the Auction Server using Java Object streams.
 * Usage: Client.getInstance().connect(host, port); Client.getInstance().sendRequest(msg);
 */
public class Client implements Closeable {

    private static Client instance;

    public static synchronized Client getInstance() {
        if (instance == null) instance = new Client();
        return instance;
    }

    private Socket socket;
    private ObjectOutputStream output;
    private ObjectInputStream input;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "client-listener"));
    private volatile boolean running = false;
    private Consumer<Message> pushHandler = m -> {};

    private Client() {
    }

    public synchronized void connect(String host, int port) throws IOException {
        if (running) return;
        Objects.requireNonNull(host, "host");
        socket = new Socket(host, port);

        // Create output first to write stream header, then input
        output = new ObjectOutputStream(socket.getOutputStream());
        output.flush();
        input = new ObjectInputStream(socket.getInputStream());

        running = true;
        startListener();
    }

    private void startListener() {
        executor.submit(() -> {
            try {
                while (running && socket != null && !socket.isClosed()) {
                    Object obj = input.readObject();
                    if (obj instanceof Message msg) {
                        // notify push handler
                        try {
                            pushHandler.accept(msg);
                        } catch (Exception ignored) {
                        }
                    }
                }
            } catch (Exception e) {
                // listener ends on error/close
            } finally {
                running = false;
            }
        });
    }

    /**
     * Send a request synchronously and wait for a single response.
     * This method is blocking and should not be called from JavaFX UI thread.
     */
    public synchronized Message sendRequest(Message request) throws IOException, ClassNotFoundException {
        ensureConnected();
        output.writeObject(request);
        output.flush();

        Object resp = input.readObject();
        if (resp instanceof Message) return (Message) resp;
        throw new IOException("Invalid response from server");
    }

    /**
     * Send a request asynchronously. Completes with the response or exception.
     */
    public CompletableFuture<Message> sendRequestAsync(Message request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return sendRequest(request);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executor);
    }

    public void setPushHandler(Consumer<Message> handler) {
        this.pushHandler = handler == null ? m -> {} : handler;
    }

    private void ensureConnected() {
        if (!running || socket == null || socket.isClosed()) throw new IllegalStateException("Client not connected");
    }

    @Override
    public synchronized void close() {
        running = false;
        try {
            if (output != null) output.close();
        } catch (Exception ignored) {
        }
        try {
            if (input != null) input.close();
        } catch (Exception ignored) {
        }
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (Exception ignored) {
        }
        executor.shutdownNow();
    }
}
