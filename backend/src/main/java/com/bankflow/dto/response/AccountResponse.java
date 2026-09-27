package com.bankflow.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

import com.bankflow.entity.Account;

public record AccountResponse(
        Long id,
        String accountNumber,
        String type,
        String status,
        BigDecimal balance,
        BigDecimal availableBalance,
        BigDecimal minimumBalance,
        BigDecimal dailyDebitLimit,
        String currency,
        Long ownerId,
        String ownerName,
        Instant createdAt) {

    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getType().name(),
                account.getStatus().name(),
                account.getBalance(),
                account.getAvailableBalance(),
                account.getType().getMinimumBalance(),
                account.getType().getDailyDebitLimit(),
                account.getCurrency(),
                account.getUser().getId(),
                account.getUser().getFullName(),
                account.getCreatedAt());
    }
}
