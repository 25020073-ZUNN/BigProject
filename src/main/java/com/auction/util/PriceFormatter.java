package com.auction.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Tiện ích định dạng tiền tệ cho toàn bộ ứng dụng.
 */
public final class PriceFormatter {

    private static final DecimalFormat PRICE_FORMAT = createPriceFormat();

    private PriceFormatter() {}

    /**
     * Định dạng BigDecimal sang chuỗi có dấu phân cách hàng nghìn + " VND".
     */
    public static String formatPrice(BigDecimal amount) {
        if (amount == null) return "0 VND";
        return PRICE_FORMAT.format(amount) + " VND";
    }

    /**
     * Định dạng BigDecimal sang chuỗi số (không có " VND").
     */
    public static String formatNumber(BigDecimal amount) {
        if (amount == null) return "0";
        return PRICE_FORMAT.format(amount);
    }

    /**
     * Định dạng chuỗi số sang kiểu tiền tệ Việt Nam.
     */
    public static String formatCurrency(String amount) {
        NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
        return formatter.format(new BigDecimal(amount));
    }

    private static DecimalFormat createPriceFormat() {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance();
        symbols.setGroupingSeparator(',');
        DecimalFormat format = new DecimalFormat("#,##0", symbols);
        format.setGroupingUsed(true);
        return format;
    }
}
