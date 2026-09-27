package com.bankflow.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record StatementResponse(
        AccountResponse account,
        Instant from,
        Instant to,
        BigDecimal openingBalance,
        BigDecimal closingBalance,
        BigDecimal totalCredits,
        BigDecimal totalDebits,
        int transactionCount,
        List<TransactionResponse> transactions) {
}
