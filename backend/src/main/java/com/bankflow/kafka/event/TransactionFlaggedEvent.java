package com.bankflow.kafka.event;

import java.math.BigDecimal;
import java.time.Instant;

import com.bankflow.kafka.Topics;

public record TransactionFlaggedEvent(
        String eventId,
        Instant occurredAt,
        Long transactionId,
        String reference,
        BigDecimal amount,
        Long accountId,
        Long ownerUserId,
        String reason)
        implements DomainEvent {

    @Override
    public String partitionKey() {
        return String.valueOf(accountId);
    }

    @Override
    public String topic() {
        return Topics.TRANSACTION_FLAGGED;
    }
}
