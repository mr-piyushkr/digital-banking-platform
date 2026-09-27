package com.bankflow.kafka;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.bankflow.kafka.event.DomainEvent;
import com.bankflow.kafka.producer.EventPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Publishes a domain event only once its transaction has committed.
 *
 * This is the whole reason the bridge exists. If a service sent to Kafka inside
 * its transaction and that transaction then rolled back, consumers would have
 * already written an audit row and sent a notification for a transfer that
 * never happened — an event describing a state the database never reached.
 *
 * AFTER_COMMIT also means a broker outage cannot roll back a committed
 * transfer: the money movement stands, and the publish failure is logged for
 * replay rather than thrown at the customer.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DomainEventBridge {

    private final EventPublisher eventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDomainEvent(DomainEvent event) {
        try {
            eventPublisher.publish(event);
        } catch (RuntimeException e) {
            log.error(
                    "Failed to publish {} (eventId {}) after commit; database state is committed",
                    event.getClass().getSimpleName(),
                    event.eventId(),
                    e);
        }
    }
}
