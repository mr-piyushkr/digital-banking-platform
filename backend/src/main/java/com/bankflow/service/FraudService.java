package com.bankflow.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.bankflow.entity.Beneficiary;
import com.bankflow.entity.Transaction;
import com.bankflow.entity.enums.TransactionStatus;
import com.bankflow.entity.enums.TransactionType;
import com.bankflow.kafka.consumer.DomainEventHandler;
import com.bankflow.kafka.event.DomainEvent;
import com.bankflow.kafka.event.TransactionCreatedEvent;
import com.bankflow.kafka.event.TransactionFlaggedEvent;
import com.bankflow.repository.BeneficiaryRepository;
import com.bankflow.repository.TransactionRepository;
import com.bankflow.util.ReferenceGenerator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Post-commit fraud screening.
 *
 * Runs after the money has moved, which is the deliberate trade-off: screening
 * inside the transaction would put rule evaluation on the latency path of every
 * payment. A match marks the transaction for admin review rather than reversing
 * it — reversing a customer's money on a heuristic is worse than flagging it.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FraudService implements DomainEventHandler {

    /** A single transaction above this is always reviewed. */
    private static final BigDecimal HIGH_VALUE_THRESHOLD = new BigDecimal("200000.00");

    /** More debits than this inside the window looks automated. */
    private static final int VELOCITY_LIMIT = 5;
    private static final Duration VELOCITY_WINDOW = Duration.ofMinutes(1);

    /** A sizeable transfer to a beneficiary added this recently is the classic takeover pattern. */
    private static final BigDecimal NEW_BENEFICIARY_THRESHOLD = new BigDecimal("50000.00");
    private static final Duration NEW_BENEFICIARY_WINDOW = Duration.ofHours(24);

    private final TransactionRepository transactionRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ReferenceGenerator referenceGenerator;

    @Override
    public boolean supports(DomainEvent event) {
        return event instanceof TransactionCreatedEvent;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(DomainEvent event) {
        TransactionCreatedEvent created = (TransactionCreatedEvent) event;

        Optional<Transaction> found = transactionRepository.findById(created.transactionId());
        if (found.isEmpty()) {
            log.warn("Fraud check skipped: transaction {} not found", created.transactionId());
            return;
        }

        Transaction transaction = found.get();

        // Idempotency: a redelivered message must not re-flag a transaction that
        // has already been screened or reviewed.
        if (transaction.getStatus() != TransactionStatus.SUCCESS) {
            return;
        }

        List<String> reasons = evaluate(created, transaction);
        if (reasons.isEmpty()) {
            return;
        }

        String reason = String.join("; ", reasons);
        transaction.setStatus(TransactionStatus.FLAGGED);
        transaction.setFlagReason(reason);
        log.warn("Flagged transaction {}: {}", transaction.getReference(), reason);

        applicationEventPublisher.publishEvent(new TransactionFlaggedEvent(
                referenceGenerator.nextEventId(),
                Instant.now(),
                transaction.getId(),
                transaction.getReference(),
                transaction.getAmount(),
                created.fromAccountId() != null ? created.fromAccountId() : created.toAccountId(),
                created.ownerUserId(),
                reason));
    }

    List<String> evaluate(TransactionCreatedEvent event, Transaction transaction) {
        List<String> reasons = new ArrayList<>();

        if (transaction.getAmount().compareTo(HIGH_VALUE_THRESHOLD) > 0) {
            reasons.add("Amount exceeds the " + HIGH_VALUE_THRESHOLD.toPlainString() + " review threshold");
        }

        if (event.fromAccountId() != null) {
            long recentDebits = transactionRepository.countDebitsSince(
                    event.fromAccountId(), Instant.now().minus(VELOCITY_WINDOW));
            if (recentDebits > VELOCITY_LIMIT) {
                reasons.add(recentDebits + " debits within " + VELOCITY_WINDOW.toSeconds() + " seconds");
            }
        }

        if (isTransferToNewBeneficiary(event, transaction)) {
            reasons.add("Large transfer to a beneficiary added in the last 24 hours");
        }

        return reasons;
    }

    private boolean isTransferToNewBeneficiary(
            TransactionCreatedEvent event, Transaction transaction) {

        if (!TransactionType.TRANSFER.name().equals(event.type())
                || event.toAccountNumber() == null
                || event.ownerUserId() == null
                || transaction.getAmount().compareTo(NEW_BENEFICIARY_THRESHOLD) <= 0) {
            return false;
        }

        return beneficiaryRepository
                .findByOwnerIdAndBeneficiaryAccountNumber(event.ownerUserId(), event.toAccountNumber())
                .map(Beneficiary::getCreatedAt)
                .filter(addedAt -> addedAt.isAfter(Instant.now().minus(NEW_BENEFICIARY_WINDOW)))
                .isPresent();
    }
}
