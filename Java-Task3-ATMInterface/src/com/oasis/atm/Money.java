package com.oasis.atm;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Money helpers. BigDecimal is used everywhere to avoid floating point errors.
 */
public final class Money {

    // Indian digit grouping (1,00,000.00). "Rs." is used instead of the rupee
    // glyph so output renders correctly on every Windows/Linux console.
    private static final NumberFormat INR = NumberFormat.getNumberInstance(new Locale("en", "IN"));
    static {
        INR.setMinimumFractionDigits(2);
        INR.setMaximumFractionDigits(2);
    }

    private Money() {}

    public static String format(BigDecimal value) {
        return "Rs." + INR.format(value.setScale(2, RoundingMode.HALF_UP));
    }

    /** Parses user input; returns null if invalid, non-positive or more than 2 decimals. */
    public static BigDecimal parse(String raw) {
        try {
            BigDecimal v = new BigDecimal(raw.trim().replace(",", ""));
            if (v.signum() <= 0 || v.scale() > 2) return null;
            return v.setScale(2, RoundingMode.UNNECESSARY);
        } catch (NumberFormatException | ArithmeticException e) {
            return null;
        }
    }
}
