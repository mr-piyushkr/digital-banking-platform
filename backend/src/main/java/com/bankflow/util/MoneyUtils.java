package com.bankflow.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * All money enters the domain through here, normalised to the column's scale.
 *
 * Without a single normalisation point, a request carrying 100.00 and one
 * carrying 100.0000 would produce BigDecimals that are equal in value but not
 * by equals(), and comparisons elsewhere would quietly diverge.
 */
public final class MoneyUtils {

    /** Matches DECIMAL(19,4) in the schema. */
    public static final int SCALE = 4;

    private MoneyUtils() {
    }

    public static BigDecimal normalise(BigDecimal amount) {
        return amount == null ? null : amount.setScale(SCALE, RoundingMode.HALF_EVEN);
    }

    public static boolean isPositive(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public static boolean isGreaterThan(BigDecimal left, BigDecimal right) {
        return left.compareTo(right) > 0;
    }
}
