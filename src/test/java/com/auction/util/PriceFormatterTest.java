package com.auction.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PriceFormatterTest {

    @Test
    void formatPriceFormatsAmountWithVndSuffix() {
        assertEquals("1,000 VND", PriceFormatter.formatPrice(new BigDecimal("1000")));
        assertEquals("50,000,000 VND", PriceFormatter.formatPrice(new BigDecimal("50000000")));
    }

    @Test
    void formatPriceHandlesNullValuesGracefully() {
        assertEquals("0 VND", PriceFormatter.formatPrice(null));
    }

    @Test
    void formatNumberFormatsAmountWithoutVndSuffix() {
        assertEquals("1,000", PriceFormatter.formatNumber(new BigDecimal("1000")));
        assertEquals("50,000,000", PriceFormatter.formatNumber(new BigDecimal("50000000")));
    }

    @Test
    void formatNumberHandlesNullValuesGracefully() {
        assertEquals("0", PriceFormatter.formatNumber(null));
    }

    @Test
    void formatCurrencyFormatsVietnameseLocaleFormat() {
        assertEquals("1.000", PriceFormatter.formatCurrency("1000"));
        assertEquals("50.000.000", PriceFormatter.formatCurrency("50000000"));
    }

    @Test
    void formatCurrencyThrowsExceptionOnInvalidInputs() {
        assertThrows(NullPointerException.class, () -> PriceFormatter.formatCurrency(null));
        assertThrows(NumberFormatException.class, () -> PriceFormatter.formatCurrency(""));
        assertThrows(NumberFormatException.class, () -> PriceFormatter.formatCurrency("abc"));
    }
}
