package com.auction.network.client;

import com.auction.network.Message;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * Kết nối TCP tới Server sử dụng giao thức JSON (newline-delimited).
 * Mỗi message là một dòng JSON. Luồng đọc riêng xử lý phản hồi và broadcast.
 */
public class ServerConnection implements Closeable {

    private final Socket socket;
    private final BufferedReader reader;
    private final PrintWriter writer;
    private final Consumer<Message> messageHandler;
    private final Thread readerThread;

    public ServerConnection(String host, int port, Consumer<Message> messageHandler) throws IOException {
        this.socket = new Socket(host, port);
        this.writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.messageHandler = messageHandler;
        this.readerThread = new Thread(this::readLoop, "auction-network-reader");
        this.readerThread.setDaemon(true);
        this.readerThread.start();
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    /**
     * Gửi message dưới dạng một dòng JSON. Thread-safe.
     */
    public synchronized void send(Message message) throws IOException {
        writer.println(message.toJson());
        if (writer.checkError()) {
            throw new IOException("Không thể gửi dữ liệu tới server");
        }
    }

    @Override
    public void close() throws IOException {
        if (socket != null) socket.close();
    }

    /**
     * Vòng lặp đọc: nhận từng dòng JSON từ server, parse và dispatch.
     */
    private void readLoop() {
        try {
            String line;
            while (!socket.isClosed() && (line = reader.readLine()) != null) {
                try {
                    Message message = Message.fromJson(line);
                    if (message != null && messageHandler != null) {
                        messageHandler.accept(message);
                    }
                } catch (Exception e) {
                    System.err.println("[CLIENT] Lỗi parse JSON từ server: " + e.getMessage());
                }
            }
        } catch (IOException ignored) {
            // Socket closed
        }
    }
}
