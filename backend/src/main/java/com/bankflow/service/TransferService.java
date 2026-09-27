package com.bankflow.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bankflow.dto.request.TransferRequest;
import com.bankflow.dto.response.TransactionResponse;
import com.bankflow.entity.Account;
import com.bankflow.entity.Beneficiary;
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
import com.bankflow.repository.BeneficiaryRepository;
import com.bankflow.repository.TransactionRepository;
import com.bankflow.util.MoneyUtils;
import com.bankflow.util.ReferenceGenerator;
import com.bankflow.util.RequestContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Account-to-account transfer. Debit and credit happen in one transaction, so
 * money can never exist in neither account or in both.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    private final AccountRepository accountRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final TransactionRepository transactionRepository;
    private final ReferenceGenerator referenceGenerator;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final RequestContext requestContext;

    @Transactional
    public TransactionResponse transfer(Long userId, TransferRequest request, String idempotencyKey) {
        String key = resolveKey(idempotencyKey);

        Transaction replay = transactionRepository.findByIdempotencyKey(key).orElse(null);
        if (replay != null) {
            log.info("Idempotent replay of key {} returning {}", key, replay.getReference());
            return TransactionResponse.from(replay, replay.getFromAccount().getId());
        }

        if (request.fromAccountNumber().equals(request.toAccountNumber())) {
            throw new BusinessException(
                    ErrorCode.SAME_ACCOUNT_TRANSFER, "Source and destination cannot be the same account.");
        }

        BigDecimal amount = MoneyUtils.normalise(request.amount());

        // Resolve ids before locking so the lock order can be decided without
        // holding a lock while doing another lookup.
        Account sourcePreview = accountRepository
                .findByAccountNumberAndUserId(request.fromAccountNumber(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", request.fromAccountNumber()));

        Account destinationPreview = accountRepository
                .findByAccountNumber(request.toAccountNumber())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Account", request.toAccountNumber()));

        // A transfer to someone else's account requires a verified beneficiary.
        // Own-account transfers skip it — there is nobody to verify.
        if (!destinationPreview.getUser().getId().equals(userId)) {
            requireVerifiedBeneficiary(userId, request.toAccountNumber());
        }

        LockedPair locked = lockInDeterministicOrder(
                sourcePreview.getId(), destinationPreview.getId());
        Account source = locked.source();
        Account destination = locked.destination();

        if (!source.getStatus().allowsDebit()) {
            throw new AccountBlockedException(source.getAccountNumber(), source.getStatus());
        }
        if (!destination.getStatus().allowsCredit()) {
            throw new AccountBlockedException(destination.getAccountNumber(), destination.getStatus());
        }
        if (!source.canDebit(amount)) {
            throw new InsufficientBalanceException(source.getAvailableBalance(), amount);
        }
        assertWithinDailyLimit(source, amount);

        source.debit(amount);
        destination.credit(amount);

        Transaction txn = new Transaction();
        txn.setReference(referenceGenerator.nextTransactionReference());
        txn.setIdempotencyKey(key);
        txn.setType(TransactionType.TRANSFER);
        txn.setStatus(TransactionStatus.SUCCESS);
        txn.setAmount(amount);
        txn.setFromAccount(source);
        txn.setToAccount(destination);
        // Recorded from the sender's perspective, which is whose statement this
        // row primarily appears on.
        txn.setBalanceAfter(source.getBalance());
        txn.setDescription(request.description());

        Transaction saved = transactionRepository.save(txn);

        applicationEventPublisher.publishEvent(new TransactionCreatedEvent(
                referenceGenerator.nextEventId(),
                Instant.now(),
                saved.getId(),
                saved.getReference(),
                saved.getType().name(),
                saved.getStatus().name(),
                saved.getAmount(),
                source.getId(),
                source.getAccountNumber(),
                destination.getId(),
                destination.getAccountNumber(),
                userId,
                requestContext.currentUserId().orElse(userId),
                saved.getDescription(),
                requestContext.clientIp(),
                requestContext.userAgent()));

        log.info(
                "Transfer {} from {} to {} ({})",
                amount,
                source.getAccountNumber(),
                destination.getAccountNumber(),
                saved.getReference());

        return TransactionResponse.from(saved, source.getId());
    }

    /**
     * Both rows are locked in ascending id order, never in request order.
     *
     * If A->B locked A then B while B->A locked B then A, each would hold the
     * lock the other needs and the database would kill one with a deadlock
     * error. A consistent global order makes that impossible: the second
     * transaction simply waits.
     */
    private LockedPair lockInDeterministicOrder(Long sourceId, Long destinationId) {
        Long firstId = Math.min(sourceId, destinationId);
        Long secondId = Math.max(sourceId, destinationId);

        Account first = accountRepository
                .findByIdForUpdate(firstId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", firstId));
        Account second = accountRepository
                .findByIdForUpdate(secondId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", secondId));

        boolean sourceWasFirst = firstId.equals(sourceId);
        return sourceWasFirst
                ? new LockedPair(first, second)
                : new LockedPair(second, first);
    }

    private void requireVerifiedBeneficiary(Long userId, String destinationAccountNumber) {
        Beneficiary beneficiary = beneficiaryRepository
                .findByOwnerIdAndBeneficiaryAccountNumber(userId, destinationAccountNumber)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.BENEFICIARY_NOT_VERIFIED,
                        "Add " + destinationAccountNumber + " as a beneficiary before transferring."));

        if (!beneficiary.isVerified()) {
            throw new BusinessException(
                    ErrorCode.BENEFICIARY_NOT_VERIFIED,
                    "Verify this beneficiary before transferring to it.");
        }
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

    private String resolveKey(String idempotencyKey) {
        return idempotencyKey == null || idempotencyKey.isBlank()
                ? referenceGenerator.nextIdempotencyKey()
                : idempotencyKey.trim();
    }

    /** Locked accounts, re-labelled back to their roles in the transfer. */
    private record LockedPair(Account source, Account destination) {}
}
