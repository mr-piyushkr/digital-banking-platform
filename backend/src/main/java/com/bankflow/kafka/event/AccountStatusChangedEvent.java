package com.bankflow.kafka.event;

import java.time.Instant;

import com.bankflow.kafka.Topics;

public record AccountStatusChangedEvent(
        String eventId,
        Instant occurredAt,
        Long accountId,
        String accountNumber,
        Long ownerUserId,
        String previousStatus,
        String newStatus,
        Long actorUserId,
        String reason,
        String ipAddress,
        String userAgent)
        implements DomainEvent {

    @Override
    public String partitionKey() {
        return String.valueOf(accountId);
    }

    @Override
    public String topic() {
        return Topics.ACCOUNT_STATUS_CHANGED;
    }
}
