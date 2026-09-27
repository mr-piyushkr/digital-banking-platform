package com.bankflow.entity.enums;

public enum TransactionStatus {
    PENDING,
    SUCCESS,
    FAILED,

    /** Committed, but a fraud rule matched. Visible to admins for review. */
    FLAGGED,

    REVERSED;

    public boolean isTerminal() {
        return this == SUCCESS || this == FAILED || this == REVERSED;
    }
}
