package com.auction.model.item;

/**
 * Enum biểu diễn trạng thái của phiên đấu giá
 * Các trạng thái bao gồm:
 * * - OPEN : Phiên đấu giá vừa được tạo
 * * - RUNNING : Phiên đấu giá đang diễn ra
 * * - FINISHED : Phiên đấu giá đã kết thúc *
 * - PAID : Sản phẩm đã được thanh toán
 * * - CANCELED : Phiên đấu giá đã bị hủy
 */
public enum ItemStatus {
    OPEN,       // Mới tạo
    RUNNING,    // Đang đấu giá
    FINISHED,   // Đã kết thúc
    PAID,       // Đã thanh toán
    CANCELED    // Bị hủy
}
