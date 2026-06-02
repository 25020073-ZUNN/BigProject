
package com.auction.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;
/**
 * PriceFormatter
 *
 * Lớp tiện ích dùng để định dạng tiền tệ trong toàn bộ hệ thống.
 *
 * Ví dụ:
 *
 * 1000000
 * ↓
 * 1,000,000 VND
 *
 * Giúp giao diện hiển thị thống nhất và dễ đọc hơn.
 */
public final class PriceFormatter {

    /**
     * Formatter dùng chung cho toàn hệ thống.
     *
     * Ví dụ:
     * 1234567
     * ↓
     * 1,234,567
     */
    private static final DecimalFormat PRICE_FORMAT =
            createPriceFormat();

    /**
     * Utility Class
     *
     * Không cho tạo object:
     * new PriceFormatter()
     */
    private PriceFormatter() {}

    /**
     * Định dạng tiền có đơn vị VND.
     *
     * Ví dụ:
     * 1500000
     * ↓
     * 1,500,000 VND
     */
    public static String formatPrice(BigDecimal amount) {

        // Tránh NullPointerException
        if (amount == null) {
            return "0 VND";
        }

        return PRICE_FORMAT.format(amount) + " VND";
    }

    /**
     * Định dạng số nhưng không thêm VND.
     *
     * Ví dụ:
     * 1500000
     * ↓
     * 1,500,000
     *
     * Thường dùng trong TextField hoặc nhập liệu.
     */
    public static String formatNumber(BigDecimal amount) {

        if (amount == null) {
            return "0";
        }

        return PRICE_FORMAT.format(amount);
    }

    /**
     * Chuyển chuỗi số thành định dạng tiền Việt Nam.
     *
     * Ví dụ:
     * "2000000"
     * ↓
     * 2.000.000
     *
     * Sử dụng Locale vi_VN.
     */
    public static String formatCurrency(String amount) {

        NumberFormat formatter =
                NumberFormat.getInstance(
                        new Locale("vi", "VN")
                );

        return formatter.format(
                new BigDecimal(amount)
        );
    }

    /**
     * Khởi tạo DecimalFormat cho hệ thống.
     *
     * Cấu hình:
     * - Dấu phân cách hàng nghìn = ','
     * - Có nhóm số
     *
     * Ví dụ:
     * 1000000
     * ↓
     * 1,000,000
     */
    private static DecimalFormat createPriceFormat() {

        // Cấu hình ký tự phân cách
        DecimalFormatSymbols symbols =
                DecimalFormatSymbols.getInstance();

        // Dấu phân cách hàng nghìn
        symbols.setGroupingSeparator(',');

        // Pattern hiển thị số
        DecimalFormat format =
                new DecimalFormat("#,##0", symbols);

        // Bật phân nhóm hàng nghìn
        format.setGroupingUsed(true);

        return format;
    }
}
/*PriceFormatter là lớp tiện ích dùng để chuẩn hóa việc hiển thị giá tiền trong hệ thống đấu giá.
Lớp sử dụng BigDecimal để đảm bảo độ chính xác của dữ liệu tiền tệ và dùng DecimalFormat để hiển thị số có dấu phân cách hàng nghìn.
Nhờ đó toàn bộ giao diện hiển thị giá sản phẩm, giá đấu và số dư tài khoản theo cùng một định dạng thống nhất*/