package com.auction.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Lớp kiểm thử đơn vị cho PriceFormatter.
 * Kiểm tra việc định dạng tiền tệ thầu thành các chuỗi hiển thị tương ứng trên UI
 * (ví dụ: thêm hậu tố VND, phân tách hàng nghìn bằng dấu phẩy hoặc dấu chấm theo quy chuẩn Tiếng Việt).
 */
class PriceFormatterTest {

    /**
     * Kiểm thử trường hợp: Định dạng giá tiền bằng cách thêm dấu phân tách hàng nghìn
     * và chèn hậu tố đơn vị tiền tệ " VND" vào cuối chuỗi.
     */
    @Test
    void formatPriceFormatsAmountWithVndSuffix() {
        assertEquals("1,000 VND", PriceFormatter.formatPrice(new BigDecimal("1000")));
        assertEquals("50,000,000 VND", PriceFormatter.formatPrice(new BigDecimal("50000000")));
    }

    /**
     * Kiểm thử trường hợp: Xử lý an toàn khi đầu vào bị null,
     * mặc định trả về chuỗi "0 VND" để tránh lỗi màn hình UI.
     */
    @Test
    void formatPriceHandlesNullValuesGracefully() {
        assertEquals("0 VND", PriceFormatter.formatPrice(null));
    }

    /**
     * Kiểm thử trường hợp: Định dạng tiền thành chuỗi số thô phân tách hàng nghìn
     * bằng dấu phẩy nhưng không chèn hậu tố đơn vị.
     */
    @Test
    void formatNumberFormatsAmountWithoutVndSuffix() {
        assertEquals("1,000", PriceFormatter.formatNumber(new BigDecimal("1000")));
        assertEquals("50,000,000", PriceFormatter.formatNumber(new BigDecimal("50000000")));
    }

    /**
     * Kiểm thử trường hợp: Xử lý an toàn khi định dạng số thô đầu vào bị null,
     * trả về mặc định chuỗi "0".
     */
    @Test
    void formatNumberHandlesNullValuesGracefully() {
        assertEquals("0", PriceFormatter.formatNumber(null));
    }

    /**
     * Kiểm thử trường hợp: Định dạng chuỗi số đầu vào dạng thô sang quy chuẩn phân tách Việt Nam
     * sử dụng dấu chấm (ví dụ: "1000" thành "1.000").
     */
    @Test
    void formatCurrencyFormatsVietnameseLocaleFormat() {
        assertEquals("1.000", PriceFormatter.formatCurrency("1000"));
        assertEquals("50.000.000", PriceFormatter.formatCurrency("50000000"));
    }

    /**
     * Kiểm thử trường hợp: Ném ngoại lệ khi truyền vào các giá trị định dạng tiền tệ không hợp lệ
     * (ví dụ: truyền null, chuỗi rỗng, hoặc các ký tự không phải chữ số).
     */
    @Test
    void formatCurrencyThrowsExceptionOnInvalidInputs() {
        assertThrows(NullPointerException.class, () -> PriceFormatter.formatCurrency(null));
        assertThrows(NumberFormatException.class, () -> PriceFormatter.formatCurrency(""));
        assertThrows(NumberFormatException.class, () -> PriceFormatter.formatCurrency("abc"));
    }
}

