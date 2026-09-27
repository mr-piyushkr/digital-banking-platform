package com.bankflow.entity.enums;

import java.math.BigDecimal;

public enum AccountType {
    /** Minimum balance applies; interest-bearing in a real bank. */
    SAVINGS(new BigDecimal("500.00"), new BigDecimal("100000.00")),

    /** No minimum balance, higher daily limit — for business use. */
    CURRENT(BigDecimal.ZERO, new BigDecimal("500000.00"));

    private final BigDecimal minimumBalance;
    private final BigDecimal dailyDebitLimit;

    AccountType(BigDecimal minimumBalance, BigDecimal dailyDebitLimit) {
        this.minimumBalance = minimumBalance;
        this.dailyDebitLimit = dailyDebitLimit;
    }

    public BigDecimal getMinimumBalance() {
        return minimumBalance;
    }

    public BigDecimal getDailyDebitLimit() {
        return dailyDebitLimit;
    }
}
