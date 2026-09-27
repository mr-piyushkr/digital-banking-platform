package com.bankflow.entity.enums;

public enum AccountStatus {
    ACTIVE,

    /** Blocked by an admin. No debits, no credits. */
    BLOCKED,

    /** Credits allowed, debits refused — used while a fraud review is open. */
    FROZEN,

    CLOSED;

    public boolean allowsDebit() {
        return this == ACTIVE;
    }

    public boolean allowsCredit() {
        return this == ACTIVE || this == FROZEN;
    }
}
