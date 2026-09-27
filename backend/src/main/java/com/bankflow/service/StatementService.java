package com.bankflow.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bankflow.dto.response.AccountResponse;
import com.bankflow.dto.response.StatementResponse;
import com.bankflow.dto.response.TransactionResponse;
import com.bankflow.entity.Account;
import com.bankflow.entity.Transaction;
import com.bankflow.exception.BusinessException;
import com.bankflow.exception.ErrorCode;
import com.bankflow.exception.ResourceNotFoundException;
import com.bankflow.repository.AccountRepository;
import com.bankflow.repository.TransactionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StatementService {

    /** Bounded so one request cannot ask the database for years of rows. */
    private static final long MAX_RANGE_DAYS = 366;

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public StatementResponse generate(Long userId, Long accountId, Instant from, Instant to) {
        if (from.isAfter(to)) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_FAILED, "Start date must be before end date.");
        }
        if (ChronoUnit.DAYS.between(from, to) > MAX_RANGE_DAYS) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_FAILED,
                    "A statement can cover at most " + MAX_RANGE_DAYS + " days.");
        }

        Account account = accountRepository
                .findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountId));

        List<Transaction> transactions =
                transactionRepository.findForStatement(account.getId(), from, to);

        BigDecimal credits = BigDecimal.ZERO;
        BigDecimal debits = BigDecimal.ZERO;
        for (Transaction txn : transactions) {
            if (isCredit(txn, account.getId())) {
                credits = credits.add(txn.getAmount());
            } else {
                debits = debits.add(txn.getAmount());
            }
        }

        // The current balance is the closing balance for the period; the opening
        // balance is derived by unwinding the period's movements. This avoids
        // depending on a historical snapshot the schema does not keep.
        BigDecimal closing = account.getBalance();
        BigDecimal opening = closing.subtract(credits).add(debits);

        return new StatementResponse(
                AccountResponse.from(account),
                from,
                to,
                opening,
                closing,
                credits,
                debits,
                transactions.size(),
                transactions.stream()
                        .map(txn -> TransactionResponse.from(txn, account.getId()))
                        .toList());
    }

    private static boolean isCredit(Transaction txn, Long accountId) {
        return txn.getToAccount() != null && txn.getToAccount().getId().equals(accountId);
    }
}
