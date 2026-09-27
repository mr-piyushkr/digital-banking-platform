package com.bankflow.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.bankflow.entity.AuditLog;
import com.bankflow.kafka.consumer.DomainEventHandler;
import com.bankflow.kafka.event.AccountStatusChangedEvent;
import com.bankflow.kafka.event.DomainEvent;
import com.bankflow.kafka.event.TransactionCreatedEvent;
import com.bankflow.kafka.event.TransactionFlaggedEvent;
import com.bankflow.kafka.event.UserRegisteredEvent;
import com.bankflow.repository.AuditLogRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Writes the audit trail. Every event type is audited — this is the record a
 * bank has to be able to produce.
 *
 * REQUIRES_NEW so an audit write commits on its own. It runs after the business
 * transaction has already committed, and the audit row must land even if a
 * sibling handler in the same dispatch fails.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService implements DomainEventHandler {

    private final AuditLogRepository auditLogRepository;

    @Override
    public boolean supports(DomainEvent event) {
        return true;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(DomainEvent event) {
        // At-least-once delivery means this exact event may arrive again.
        if (auditLogRepository.existsByEventId(event.eventId())) {
            log.debug("Audit row for event {} already exists; skipping", event.eventId());
            return;
        }

        AuditLog entry = switch (event) {
            case UserRegisteredEvent e -> AuditLog.builder()
                    .actorUserId(e.userId())
                    .action("USER_REGISTERED")
                    .entityType("User")
                    .entityId(String.valueOf(e.userId()))
                    .details("Registered " + e.email())
                    .ipAddress(e.ipAddress())
                    .userAgent(e.userAgent())
                    .eventId(e.eventId())
                    .build();

            case TransactionCreatedEvent e -> AuditLog.builder()
                    .actorUserId(e.actorUserId())
                    .action("TRANSACTION_" + e.type())
                    .entityType("Transaction")
                    .entityId(e.reference())
                    .details(describe(e))
                    .ipAddress(e.ipAddress())
                    .userAgent(e.userAgent())
                    .eventId(e.eventId())
                    .build();

            case TransactionFlaggedEvent e -> AuditLog.builder()
                    .action("TRANSACTION_FLAGGED")
                    .entityType("Transaction")
                    .entityId(e.reference())
                    .details("Flagged: " + e.reason())
                    .eventId(e.eventId())
                    .build();

            case AccountStatusChangedEvent e -> AuditLog.builder()
                    .actorUserId(e.actorUserId())
                    .action("ACCOUNT_STATUS_CHANGED")
                    .entityType("Account")
                    .entityId(e.accountNumber())
                    .details(e.previousStatus() + " -> " + e.newStatus()
                            + (e.reason() == null ? "" : " (" + e.reason() + ")"))
                    .ipAddress(e.ipAddress())
                    .userAgent(e.userAgent())
                    .eventId(e.eventId())
                    .build();
        };

        auditLogRepository.save(entry);
    }

    private static String describe(TransactionCreatedEvent e) {
        StringBuilder text = new StringBuilder();
        text.append(e.type()).append(' ').append(e.amount());
        if (e.fromAccountNumber() != null) {
            text.append(" from ").append(e.fromAccountNumber());
        }
        if (e.toAccountNumber() != null) {
            text.append(" to ").append(e.toAccountNumber());
        }
        text.append(" [").append(e.status()).append(']');
        return text.toString();
    }
}
