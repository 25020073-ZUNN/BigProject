package com.auction.network.client;
import com.auction.network.Message;

import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Map;

/**
 * Lớp ServerConnection chịu trách nhiệm thiết lập và quản lý kết nối Socket từ Client tới Server.
 * Nó sử dụng ObjectStream để truyền tải các đối tượng Message qua mạng.
 */
public class ServerConnection implements Closeable {

    private final Socket socket; // Socket kết nối đến server
    private final ObjectOutputStream outputStream; // Luồng xuất dữ liệu (gửi message)
    private final ObjectInputStream inputStream; // Luồng nhập dữ liệu (nhận phản hồi)

    /**
     * Khởi tạo một kết nối mới tới Server.
     * @param host Địa chỉ IP hoặc tên miền của Server
     * @param port Cổng dịch vụ của Server
     * @throws IOException Nếu không thể thiết lập kết nối hoặc khởi tạo luồng dữ liệu
     */
    public ServerConnection(String host, int port) throws IOException {
        this.socket = new Socket(host, port);
        // Lưu ý: Phải khởi tạo ObjectOutputStream trước ObjectInputStream để tránh tình trạng deadlock (treo luồng)
        this.outputStream = new ObjectOutputStream(socket.getOutputStream());
        this.inputStream = new ObjectInputStream(socket.getInputStream());
    }

    /**
     * Gửi một yêu cầu tới Server dựa trên loại Message và dữ liệu đi kèm.
     */
    public Message send(Message.Type type, Map<String, Object> payload) throws IOException, ClassNotFoundException {
        return send(new Message(type, payload));
    }

    /**
     * Gửi đối tượng Message tới Server và đợi nhận phản hồi.
     * @param message Đối tượng chứa thông tin yêu cầu
     * @return Message Phản hồi từ Server
     * @throws IOException Nếu có lỗi trong quá trình truyền tải dữ liệu
     * @throws ClassNotFoundException Nếu lớp của đối tượng nhận được không tồn tại ở phía Client
     */
    public Message send(Message message) throws IOException, ClassNotFoundException {
        // Ghi đối tượng vào luồng xuất
        outputStream.writeObject(message);
        outputStream.flush(); // Đẩy dữ liệu đi ngay lập tức

        // Đọc phản hồi từ luồng nhập
        Object response = inputStream.readObject();
        if (!(response instanceof Message serverMessage)) {
            throw new IOException("Phản hồi từ server không đúng định dạng Message");
        }
        return serverMessage;
    }

    /**
     * Đóng tất cả các tài nguyên kết nối (Luồng và Socket).
     */
    @Override
    public void close() throws IOException {
        if (inputStream != null) inputStream.close();
        if (outputStream != null) outputStream.close();
        if (socket != null) socket.close();
    }
}