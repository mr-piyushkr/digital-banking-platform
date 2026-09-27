package com.bankflow.entity.enums;

/**
 * A transfer is one row holding both sides, not two rows — the ledger stays
 * consistent because debit and credit are written in the same transaction.
 */
public enum TransactionType {
    DEPOSIT,
    WITHDRAWAL,
    TRANSFER
}
