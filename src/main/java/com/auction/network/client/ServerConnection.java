package com.auction.network.client;
import com.auction.network.Message;

import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Lớp ServerConnection chịu trách nhiệm thiết lập và quản lý kết nối Socket từ Client tới Server.
 * Nó sử dụng ObjectStream để truyền tải các đối tượng Message qua mạng.
 */
public class ServerConnection implements Closeable {

    private final Socket socket; // Socket kết nối đến server
    private final ObjectOutputStream outputStream; // Luồng xuất dữ liệu (gửi message)
    private final ObjectInputStream inputStream; // Luồng nhập dữ liệu (nhận phản hồi)
    private final Consumer<Message> messageHandler;
    private final Thread readerThread;

    /**
     * Khởi tạo một kết nối mới tới Server.
     * @param host Địa chỉ IP hoặc tên miền của Server
     * @param port Cổng dịch vụ của Server
     * @throws IOException Nếu không thể thiết lập kết nối hoặc khởi tạo luồng dữ liệu
     */
    public ServerConnection(String host, int port, Consumer<Message> messageHandler) throws IOException {
        this.socket = new Socket(host, port);
        // Lưu ý: Phải khởi tạo ObjectOutputStream trước ObjectInputStream để tránh tình trạng deadlock (treo luồng)
        this.outputStream = new ObjectOutputStream(socket.getOutputStream());
        this.inputStream = new ObjectInputStream(socket.getInputStream());
        this.messageHandler = messageHandler;
        this.readerThread = new Thread(this::readLoop, "auction-network-reader");
        this.readerThread.setDaemon(true);
        this.readerThread.start();
    }

    /**
     * Gửi một yêu cầu tới Server dựa trên loại Message và dữ liệu đi kèm.
     */
    public void send(Message.Type type, Map<String, Object> payload) throws IOException {
        send(new Message(type, payload));
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    /**
     * Gửi đối tượng Message tới Server và đợi nhận phản hồi.
     * @param message Đối tượng chứa thông tin yêu cầu
     * @return Message Phản hồi từ Server
     * @throws IOException Nếu có lỗi trong quá trình truyền tải dữ liệu
     * @throws ClassNotFoundException Nếu lớp của đối tượng nhận được không tồn tại ở phía Client
     */
    public synchronized void send(Message message) throws IOException {
        outputStream.writeObject(message);
        outputStream.flush();
    }

    /**
     * Đóng tất cả các tài nguyên kết nối (Luồng và Socket).
     */
    @Override
    public void close() throws IOException {
        if (socket != null) socket.close();
        if (inputStream != null) inputStream.close();
        if (outputStream != null) outputStream.close();
    }

    private void readLoop() {
        try {
            while (!socket.isClosed()) {
                Object response = inputStream.readObject();
                if (!(response instanceof Message serverMessage)) {
                    throw new IOException("Phản hồi từ server không đúng định dạng Message");
                }
                if (messageHandler != null) {
                    try {
                        messageHandler.accept(serverMessage);
                    } catch (Exception handlerException) {
                        System.err.println("[CLIENT] Lỗi xử lý message từ server: " + handlerException.getMessage());
                    }
                }
            }
        } catch (SocketException ignored) {
        } catch (Exception ignored) {
        }
    }
}
