package com.bankflow.kafka.event;

import java.time.Instant;

/**
 * Every event carries its own id and a partition key.
 *
 * The id exists because Kafka delivers at least once — consumers store it and
 * skip a message they have already handled. The key decides the partition, and
 * keying by account means all events for one account stay in order relative to
 * each other, which matters when an audit trail is read back.
 */
public sealed interface DomainEvent
        permits UserRegisteredEvent,
                TransactionCreatedEvent,
                TransactionFlaggedEvent,
                AccountStatusChangedEvent {

    String eventId();

    Instant occurredAt();

    /** Kafka partition key. */
    String partitionKey();

    String topic();
}
