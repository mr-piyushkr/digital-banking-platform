package com.bankflow.kafka.producer;

import com.bankflow.kafka.event.DomainEvent;

/**
 * Where a committed domain event goes. Kafka in dev and docker, in-process on
 * the local profile and in tests that do not need a broker.
 *
 * Services never depend on this directly — they publish a Spring application
 * event and DomainEventBridge forwards it here after the transaction commits.
 */
public interface EventPublisher {

    void publish(DomainEvent event);
}
