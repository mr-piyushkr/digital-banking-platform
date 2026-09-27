package com.bankflow.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bankflow.dto.response.DashboardResponse;
import com.bankflow.entity.enums.AccountStatus;
import com.bankflow.entity.enums.TransactionStatus;
import com.bankflow.repository.AccountRepository;
import com.bankflow.repository.TransactionRepository;
import com.bankflow.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Admin dashboard figures. Every number is a COUNT or SUM computed in SQL —
 * loading rows to count them in Java would not survive a real data volume.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public DashboardResponse snapshot() {
        Instant startOfDay = LocalDate.now(ZONE).atStartOfDay(ZONE).toInstant();

        return new DashboardResponse(
                userRepository.count(),
                userRepository.countByEnabledTrue(),
                accountRepository.count(),
                accountRepository.countByStatus(AccountStatus.BLOCKED),
                accountRepository.sumActiveBalances(),
                transactionRepository.countByCreatedAtAfter(startOfDay),
                transactionRepository.sumSuccessfulAmountSince(startOfDay),
                transactionRepository.countByStatus(TransactionStatus.FLAGGED),
                transactionRepository.countByStatus(TransactionStatus.FAILED));
    }
}
