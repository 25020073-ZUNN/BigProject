package com.auction.network;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Message implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public enum Type {
        LOGIN,
        REGISTER,
        GET_AUCTIONS,
        PLACE_BID,
        PING,
        ERROR
    }

    private final String requestId;
    private final Type type;
    private final Map<String, Object> payload;
    private final boolean success;
    private final String message;

    public Message(Type type, Map<String, Object> payload) {
        this(UUID.randomUUID().toString(), type, payload, true, null);
    }

    public Message(String requestId, Type type, Map<String, Object> payload, boolean success, String message) {
        this.requestId = requestId == null ? UUID.randomUUID().toString() : requestId;
        this.type = type;
        this.payload = payload == null ? new HashMap<>() : new HashMap<>(payload);
        this.success = success;
        this.message = message;
    }

    public static Message success(Message request, Map<String, Object> payload) {
        return new Message(request == null ? null : request.requestId, request == null ? Type.ERROR : request.type, payload, true, null);
    }

    public static Message failure(Message request, String message) {
        return new Message(request == null ? null : request.requestId, Type.ERROR, Map.of(), false, message);
    }

    public String getRequestId() {
        return requestId;
    }

    public Type getType() {
        return type;
    }

    public Map<String, Object> getPayload() {
        return new HashMap<>(payload);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}
