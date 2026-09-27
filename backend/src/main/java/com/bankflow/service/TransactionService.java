package com.bankflow.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bankflow.dto.request.DepositRequest;
import com.bankflow.dto.request.WithdrawRequest;
import com.bankflow.dto.response.TransactionResponse;
import com.bankflow.entity.Account;
import com.bankflow.entity.Transaction;
import com.bankflow.entity.enums.TransactionStatus;
import com.bankflow.entity.enums.TransactionType;
import com.bankflow.exception.AccountBlockedException;
import com.bankflow.exception.BusinessException;
import com.bankflow.exception.ErrorCode;
import com.bankflow.exception.InsufficientBalanceException;
import com.bankflow.exception.ResourceNotFoundException;
import com.bankflow.kafka.event.TransactionCreatedEvent;
import com.bankflow.repository.AccountRepository;
import com.bankflow.repository.TransactionRepository;
import com.bankflow.util.MoneyUtils;
import com.bankflow.util.ReferenceGenerator;
import com.bankflow.util.RequestContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Single-account money movement: deposits and withdrawals.
 *
 * Both take a pessimistic row lock before reading the balance. Two concurrent
 * withdrawals that each read the same balance and then each subtract from it
 * would let the account go overdrawn — the lock makes the read-modify-write
 * sequence serialise at the database.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final ReferenceGenerator referenceGenerator;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final RequestContext requestContext;

    @Transactional
    public TransactionResponse deposit(Long userId, DepositRequest request, String idempotencyKey) {
        String key = resolveKey(idempotencyKey);

        Transaction replay = findReplay(key);
        if (replay != null) {
            return TransactionResponse.from(replay, replay.getToAccount().getId());
        }

        BigDecimal amount = MoneyUtils.normalise(request.amount());

        // Lock first, then read the balance. Reading before locking would make
        // the value stale by the time it is used.
        Account account = lockOwnedAccount(request.accountNumber(), userId);

        if (!account.getStatus().allowsCredit()) {
            throw new AccountBlockedException(account.getAccountNumber(), account.getStatus());
        }

        account.credit(amount);

        Transaction txn = new Transaction();
        txn.setReference(referenceGenerator.nextTransactionReference());
        txn.setIdempotencyKey(key);
        txn.setType(TransactionType.DEPOSIT);
        txn.setStatus(TransactionStatus.SUCCESS);
        txn.setAmount(amount);
        txn.setToAccount(account);
        txn.setBalanceAfter(account.getBalance());
        txn.setDescription(request.description());

        Transaction saved = transactionRepository.save(txn);
        publishCreated(saved, account.getUser().getId());

        log.info("Deposit {} into {} ({})", amount, account.getAccountNumber(), saved.getReference());
        return TransactionResponse.from(saved, account.getId());
    }

    @Transactional
    public TransactionResponse withdraw(Long userId, WithdrawRequest request, String idempotencyKey) {
        String key = resolveKey(idempotencyKey);

        Transaction replay = findReplay(key);
        if (replay != null) {
            return TransactionResponse.from(replay, replay.getFromAccount().getId());
        }

        BigDecimal amount = MoneyUtils.normalise(request.amount());
        Account account = lockOwnedAccount(request.accountNumber(), userId);

        if (!account.getStatus().allowsDebit()) {
            throw new AccountBlockedException(account.getAccountNumber(), account.getStatus());
        }
        if (!account.canDebit(amount)) {
            throw new InsufficientBalanceException(account.getAvailableBalance(), amount);
        }
        assertWithinDailyLimit(account, amount);

        account.debit(amount);

        Transaction txn = new Transaction();
        txn.setReference(referenceGenerator.nextTransactionReference());
        txn.setIdempotencyKey(key);
        txn.setType(TransactionType.WITHDRAWAL);
        txn.setStatus(TransactionStatus.SUCCESS);
        txn.setAmount(amount);
        txn.setFromAccount(account);
        txn.setBalanceAfter(account.getBalance());
        txn.setDescription(request.description());

        Transaction saved = transactionRepository.save(txn);
        publishCreated(saved, account.getUser().getId());

        log.info("Withdrawal {} from {} ({})", amount, account.getAccountNumber(), saved.getReference());
        return TransactionResponse.from(saved, account.getId());
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> history(Long userId, Long accountId, Pageable pageable) {
        Account account = accountRepository
                .findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountId));

        return transactionRepository
                .findByAccount(account.getId(), pageable)
                .map(txn -> TransactionResponse.from(txn, account.getId()));
    }

    /**
     * Looked up by reference, then checked against the caller's accounts. A
     * reference is guessable in principle, so ownership is verified rather than
     * assumed.
     */
    @Transactional(readOnly = true)
    public TransactionResponse getOwnTransaction(Long userId, String reference) {
        Transaction txn = transactionRepository
                .findByReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", reference));

        Long viewingAccountId = resolveViewingAccount(txn, userId);
        if (viewingAccountId == null) {
            // Deliberately a 404, not a 403: confirming the reference exists
            // would leak that another customer made this transaction.
            throw new ResourceNotFoundException("Transaction", reference);
        }
        return TransactionResponse.from(txn, viewingAccountId);
    }

    private Long resolveViewingAccount(Transaction txn, Long userId) {
        if (txn.getFromAccount() != null
                && txn.getFromAccount().getUser().getId().equals(userId)) {
            return txn.getFromAccount().getId();
        }
        if (txn.getToAccount() != null
                && txn.getToAccount().getUser().getId().equals(userId)) {
            return txn.getToAccount().getId();
        }
        return null;
    }

    private Account lockOwnedAccount(String accountNumber, Long userId) {
        Account account = accountRepository
                .findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountNumber));

        if (!account.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Account", accountNumber);
        }
        return account;
    }

    private void assertWithinDailyLimit(Account account, BigDecimal amount) {
        Instant startOfDay = LocalDate.now(ZONE).atStartOfDay(ZONE).toInstant();
        BigDecimal alreadyDebited = transactionRepository.sumDebitsSince(account.getId(), startOfDay);
        BigDecimal limit = account.getType().getDailyDebitLimit();

        if (alreadyDebited.add(amount).compareTo(limit) > 0) {
            throw new BusinessException(
                    ErrorCode.DAILY_LIMIT_EXCEEDED,
                    "This would exceed the daily limit of " + limit.toPlainString()
                            + ". Already debited today: " + alreadyDebited.toPlainString() + ".");
        }
    }

    /**
     * A replay returns the original result instead of moving money again. The
     * unique index on idempotency_key is what makes this safe under concurrency:
     * if two replays race past this check, one of them fails at the database.
     */
    private Transaction findReplay(String idempotencyKey) {
        return transactionRepository
                .findByIdempotencyKey(idempotencyKey)
                .map(existing -> {
                    log.info(
                            "Idempotent replay of key {} returning {}",
                            idempotencyKey,
                            existing.getReference());
                    return existing;
                })
                .orElse(null);
    }

    private String resolveKey(String idempotencyKey) {
        return idempotencyKey == null || idempotencyKey.isBlank()
                ? referenceGenerator.nextIdempotencyKey()
                : idempotencyKey.trim();
    }

    private void publishCreated(Transaction txn, Long ownerUserId) {
        applicationEventPublisher.publishEvent(new TransactionCreatedEvent(
                referenceGenerator.nextEventId(),
                Instant.now(),
                txn.getId(),
                txn.getReference(),
                txn.getType().name(),
                txn.getStatus().name(),
                txn.getAmount(),
                txn.getFromAccount() == null ? null : txn.getFromAccount().getId(),
                txn.getFromAccount() == null ? null : txn.getFromAccount().getAccountNumber(),
                txn.getToAccount() == null ? null : txn.getToAccount().getId(),
                txn.getToAccount() == null ? null : txn.getToAccount().getAccountNumber(),
                ownerUserId,
                requestContext.currentUserId().orElse(ownerUserId),
                txn.getDescription(),
                requestContext.clientIp(),
                requestContext.userAgent()));
    }
}
