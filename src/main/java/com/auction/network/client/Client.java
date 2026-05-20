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
 * Lớp hỗ trợ Client để kết nối tới Auction Server sử dụng Java Object streams.
 * Cách dùng: Client.getInstance().connect(host, port); Client.getInstance().sendRequest(msg);
 */
public class Client implements Closeable {

    // Instance duy nhất của Client (Singleton Pattern)
    private static Client instance;

    /**
     * Lấy instance duy nhất của lớp Client.
     * 
     * @return Instance của Client.
     */
    public static synchronized Client getInstance() {
        if (instance == null) instance = new Client();
        return instance;
    }

    private Socket socket;                      // Socket để kết nối mạng
    private ObjectOutputStream output;          // Luồng ghi đối tượng ra server
    private ObjectInputStream input;            // Luồng đọc đối tượng từ server
    
    // Executor dịch vụ để chạy một luồng riêng biệt lắng nghe phản hồi từ server
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "client-listener"));
    
    private volatile boolean running = false;   // Cờ đánh dấu trạng thái đang chạy
    
    // Hàm xử lý các tin nhắn đẩy (push messages) từ server
    private Consumer<Message> pushHandler = m -> {};

    // Constructor riêng tư để thực thi Singleton
    private Client() {
    }

    /**
     * Kết nối tới server thông qua host và port.
     * 
     * @param host Địa chỉ máy chủ.
     * @param port Cổng kết nối.
     * @throws IOException Nếu có lỗi xảy ra trong quá trình kết nối.
     */
    public synchronized void connect(String host, int port) throws IOException {
        if (running) return; // Nếu đang chạy thì không kết nối lại
        Objects.requireNonNull(host, "host");
        socket = new Socket(host, port);

        // Tạo luồng output trước để ghi header của stream, sau đó mới tạo luồng input
        output = new ObjectOutputStream(socket.getOutputStream());
        output.flush();
        input = new ObjectInputStream(socket.getInputStream());

        running = true;
        startListener(); // Bắt đầu lắng nghe dữ liệu từ server
    }

    /**
     * Khởi chạy luồng lắng nghe các đối tượng Message gửi về từ server.
     */
    private void startListener() {
        executor.submit(() -> {
            try {
                while (running && socket != null && !socket.isClosed()) {
                    Object obj = input.readObject();
                    if (obj instanceof Message msg) {
                        // Thông báo cho pushHandler khi nhận được Message
                        try {
                            pushHandler.accept(msg);
                        } catch (Exception ignored) {
                            // Bỏ qua lỗi trong trình xử lý đẩy
                        }
                    }
                }
            } catch (Exception e) {
                // Luồng lắng nghe kết thúc khi có lỗi hoặc socket đóng
            } finally {
                running = false;
            }
        });
    }

    /**
     * Gửi yêu cầu một cách đồng bộ và đợi một phản hồi duy nhất.
     * Phương thức này gây chặn (blocking) và không nên gọi từ luồng UI của JavaFX.
     * 
     * @param request Tin nhắn yêu cầu gửi đi.
     * @return Tin nhắn phản hồi từ server.
     * @throws IOException, ClassNotFoundException Nếu có lỗi truyền thông hoặc parse dữ liệu.
     */
    public synchronized Message sendRequest(Message request) throws IOException, ClassNotFoundException {
        ensureConnected(); // Đảm bảo đã kết nối
        output.writeObject(request);
        output.flush();

        Object resp = input.readObject();
        if (resp instanceof Message) return (Message) resp;
        throw new IOException("Phản hồi không hợp lệ từ server");
    }

    /**
     * Gửi yêu cầu một cách bất đồng bộ. Hoàn thành với phản hồi hoặc ngoại lệ.
     * 
     * @param request Tin nhắn yêu cầu gửi đi.
     * @return CompletableFuture chứa kết quả phản hồi.
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

    /**
     * Thiết lập trình xử lý cho các tin nhắn được đẩy từ server.
     * 
     * @param handler Hàm xử lý tin nhắn.
     */
    public void setPushHandler(Consumer<Message> handler) {
        this.pushHandler = handler == null ? m -> {} : handler;
    }

    /**
     * Kiểm tra xem client đã được kết nối và đang hoạt động hay chưa.
     */
    private void ensureConnected() {
        if (!running || socket == null || socket.isClosed()) throw new IllegalStateException("Client chưa được kết nối");
    }

    /**
     * Đóng kết nối và giải phóng tài nguyên.
     */
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
        executor.shutdownNow(); // Dừng luồng lắng nghe ngay lập tức
    }
}
