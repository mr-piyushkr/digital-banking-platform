package com.bankflow.kafka.event;

import java.math.BigDecimal;
import java.time.Instant;

import com.bankflow.kafka.Topics;

public record TransactionCreatedEvent(
        String eventId,
        Instant occurredAt,
        Long transactionId,
        String reference,
        String type,
        String status,
        BigDecimal amount,
        Long fromAccountId,
        String fromAccountNumber,
        Long toAccountId,
        String toAccountNumber,
        Long ownerUserId,
        Long actorUserId,
        String description,
        String ipAddress,
        String userAgent)
        implements DomainEvent {

    @Override
    public String partitionKey() {
        // Debits key on the source account; a deposit has no source, so the
        // destination is used. Either way every event for one account lands on
        // the same partition and stays ordered.
        Long key = fromAccountId != null ? fromAccountId : toAccountId;
        return String.valueOf(key);
    }

    @Override
    public String topic() {
        return Topics.TRANSACTION_CREATED;
    }
}
