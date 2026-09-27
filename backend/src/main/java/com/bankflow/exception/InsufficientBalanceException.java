package com.bankflow.exception;

import java.math.BigDecimal;

public class InsufficientBalanceException extends BusinessException {

    public InsufficientBalanceException(BigDecimal available, BigDecimal requested) {
        super(
                ErrorCode.INSUFFICIENT_BALANCE,
                "Insufficient balance. Available " + available.toPlainString()
                        + ", requested " + requested.toPlainString() + ".");
    }
}
