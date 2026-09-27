package com.bankflow.dto.response;

import java.math.BigDecimal;

public record DashboardResponse(
        long totalUsers,
        long activeUsers,
        long totalAccounts,
        long blockedAccounts,
        BigDecimal totalHoldings,
        long transactionsToday,
        BigDecimal volumeToday,
        long flaggedTransactions,
        long failedTransactions) {
}
