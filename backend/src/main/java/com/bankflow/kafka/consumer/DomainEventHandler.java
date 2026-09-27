package com.bankflow.kafka.consumer;

import com.bankflow.kafka.event.DomainEvent;

/**
 * One handler per concern — audit, fraud, notification.
 *
 * The same three implementations run whether events arrive over Kafka or
 * in-process, so the local profile exercises identical logic and the handlers
 * can be unit tested without a broker.
 *
 * Implementations must be idempotent: Kafka delivers at least once, so a
 * handler has to tolerate seeing the same eventId twice.
 */
public interface DomainEventHandler {

    void handle(DomainEvent event);

    /** Lets the in-process publisher skip handlers that do not care. */
    boolean supports(DomainEvent event);
}
