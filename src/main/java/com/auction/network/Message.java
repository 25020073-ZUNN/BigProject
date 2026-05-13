package com.auction.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Lớp Message đại diện cho một thông điệp được trao đổi giữa Client và Server.
 * Sử dụng Gson để tuần tự hóa/giải tuần tự hóa dưới dạng JSON qua TCP.
 */
public class Message {

    private static final Gson GSON = new GsonBuilder()
            .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
            .create();

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
        /** Tạo phiên đấu giá mới */
        CREATE_AUCTION,
        /** Cập nhật thông tin hồ sơ người dùng */
        UPDATE_PROFILE,
        /** Xóa tài khoản người dùng */
        DELETE_ACCOUNT,
        /** Kiểm tra trạng thái cơ sở dữ liệu */
        DB_STATUS,
        /** Đồng bộ snapshot phiên đấu giá từ server xuống client */
        AUCTION_SYNC,
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
     */
    public Message(Type type, Map<String, Object> payload) {
        this(UUID.randomUUID().toString(), type, payload, true, null);
    }

    /**
     * Khởi tạo đầy đủ các thuộc tính của một thông điệp.
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
     */
    public static Message success(Message request, Map<String, Object> payload) {
        return new Message(request == null ? null : request.requestId, request == null ? Type.ERROR : request.type, payload, true, null);
    }

    /**
     * Tạo một thông điệp phản hồi thất bại.
     */
    public static Message failure(Message request, String message) {
        return new Message(request == null ? null : request.requestId, Type.ERROR, Map.of(), false, message);
    }

    // ===== Gson JSON serialization =====

    public String toJson() {
        return GSON.toJson(this);
    }

    public static Message fromJson(String json) {
        return GSON.fromJson(json, Message.class);
    }

    // ===== Getters =====

    public String getRequestId() {
        return requestId;
    }

    public Type getType() {
        return type;
    }

    public Map<String, Object> getPayload() {
        return payload == null ? new HashMap<>() : new HashMap<>(payload);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}
