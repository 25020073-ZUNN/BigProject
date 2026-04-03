package com.uet.bidding.model.item;

/**
 * Enum biểu diễn trạng thái của phiên đấu giá
 */
public enum ItemStatus {
    OPEN,       // Mới tạo
    RUNNING,    // Đang đấu giá
    FINISHED,   // Đã kết thúc
    PAID,       // Đã thanh toán
    CANCELED    // Bị hủy
}