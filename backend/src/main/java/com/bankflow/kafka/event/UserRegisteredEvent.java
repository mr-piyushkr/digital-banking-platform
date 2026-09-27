package com.bankflow.kafka.event;

import java.time.Instant;

import com.bankflow.kafka.Topics;

public record UserRegisteredEvent(
        String eventId,
        Instant occurredAt,
        Long userId,
        String email,
        String fullName,
        String ipAddress,
        String userAgent)
        implements DomainEvent {

    @Override
    public String partitionKey() {
        return String.valueOf(userId);
    }

    @Override
    public String topic() {
        return Topics.USER_REGISTERED;
    }
}
