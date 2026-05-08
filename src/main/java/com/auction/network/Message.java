package com.auction.network;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Lớp Message đại diện cho một thông điệp được trao đổi giữa Client và Server.
 * Lớp này hỗ trợ việc đóng gói yêu cầu và phản hồi thông qua cơ chế Serialization.
 */
public class Message implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Định nghĩa các loại thông điệp được hỗ trợ trong hệ thống.
     */
    public enum Type {
        /** Đăng nhập người dùng */
        LOGIN,
        /** Đăng ký tài khoản mới */
        REGISTER,
        /** Lấy danh sách các cuộc đấu giá */
        GET_AUCTIONS,
        /** Đặt giá thầu cho một vật phẩm */
        PLACE_BID,
        /** Kiểm tra trạng thái cơ sở dữ liệu */
        DB_STATUS,
        /** Kiểm tra kết nối (Ping) */
        PING,
        /** Thông báo lỗi */
        ERROR
    }

    // ID duy nhất cho mỗi yêu cầu để theo dõi phản hồi
    private final String requestId;
    // Loại thông điệp
    private final Type type;
    // Dữ liệu đi kèm thông điệp (payload)
    private final Map<String, Object> payload;
    // Trạng thái xử lý (thành công hoặc thất bại)
    private final boolean success;
    // Thông báo chi tiết (thường dùng cho lỗi)
    private final String message;

    /**
     * Khởi tạo một thông điệp mới (thường dùng cho các yêu cầu từ Client).
     * @param type Loại thông điệp
     * @param payload Dữ liệu gửi đi
     */
    public Message(Type type, Map<String, Object> payload) {
        this(UUID.randomUUID().toString(), type, payload, true, null);
    }

    /**
     * Khởi tạo đầy đủ các thuộc tính của một thông điệp.
     * @param requestId ID yêu cầu
     * @param type Loại thông điệp
     * @param payload Dữ liệu payload
     * @param success Trạng thái
     * @param message Thông báo đi kèm
     */
    public Message(String requestId, Type type, Map<String, Object> payload, boolean success, String message) {
        this.requestId = requestId == null ? UUID.randomUUID().toString() : requestId;
        this.type = type;
        this.payload = payload == null ? new HashMap<>() : new HashMap<>(payload);
        this.success = success;
        this.message = message;
    }

    /**
     * Tạo một thông điệp phản hồi thành công dựa trên yêu cầu trước đó.
     * @param request Thông điệp yêu cầu gốc
     * @param payload Dữ liệu phản hồi
     * @return Đối tượng Message phản hồi thành công
     */
    public static Message success(Message request, Map<String, Object> payload) {
        return new Message(request == null ? null : request.requestId, request == null ? Type.ERROR : request.type, payload, true, null);
    }

    /**
     * Tạo một thông điệp phản hồi thất bại.
     * @param request Thông điệp yêu cầu gốc
     * @param message Nội dung thông báo lỗi
     * @return Đối tượng Message phản hồi thất bại
     */
    public static Message failure(Message request, String message) {
        return new Message(request == null ? null : request.requestId, Type.ERROR, Map.of(), false, message);
    }

    /**
     * @return ID của yêu cầu
     */
    public String getRequestId() {
        return requestId;
    }

    /**
     * @return Loại của thông điệp
     */
    public Type getType() {
        return type;
    }

    /**
     * @return Bản sao của dữ liệu payload
     */
    public Map<String, Object> getPayload() {
        return new HashMap<>(payload);
    }

    /**
     * @return true nếu xử lý thành công
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * @return Thông báo đi kèm (thường là lỗi)
     */
    public String getMessage() {
        return message;
    }
}
