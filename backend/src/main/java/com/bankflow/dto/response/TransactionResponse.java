package com.bankflow.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

import com.bankflow.entity.Account;
import com.bankflow.entity.Transaction;

public record TransactionResponse(
        Long id,
        String reference,
        String type,
        String status,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String fromAccountNumber,
        String toAccountNumber,
        String counterparty,
        /** CREDIT or DEBIT from the perspective of the account being viewed. */
        String direction,
        String description,
        String flagReason,
        String failureReason,
        Instant createdAt) {

    /**
     * A transfer is one row, so whether it reads as a credit or a debit depends
     * on which account the caller is looking at. The viewing account is passed
     * in rather than inferred, because both sides use the same row.
     */
    public static TransactionResponse from(Transaction txn, Long viewingAccountId) {
        boolean isCredit = txn.getToAccount() != null
                && txn.getToAccount().getId().equals(viewingAccountId);

        String counterparty = isCredit
                ? numberOf(txn.getFromAccount())
                : numberOf(txn.getToAccount());

        return new TransactionResponse(
                txn.getId(),
                txn.getReference(),
                txn.getType().name(),
                txn.getStatus().name(),
                txn.getAmount(),
                txn.getBalanceAfter(),
                numberOf(txn.getFromAccount()),
                numberOf(txn.getToAccount()),
                counterparty,
                isCredit ? "CREDIT" : "DEBIT",
                txn.getDescription(),
                txn.getFlagReason(),
                txn.getFailureReason(),
                txn.getCreatedAt());
    }

    /** Admin view, where there is no single viewing account. */
    public static TransactionResponse forAdmin(Transaction txn) {
        return new TransactionResponse(
                txn.getId(),
                txn.getReference(),
                txn.getType().name(),
                txn.getStatus().name(),
                txn.getAmount(),
                txn.getBalanceAfter(),
                numberOf(txn.getFromAccount()),
                numberOf(txn.getToAccount()),
                null,
                null,
                txn.getDescription(),
                txn.getFlagReason(),
                txn.getFailureReason(),
                txn.getCreatedAt());
    }

    private static String numberOf(Account account) {
        return account == null ? null : account.getAccountNumber();
    }
}
