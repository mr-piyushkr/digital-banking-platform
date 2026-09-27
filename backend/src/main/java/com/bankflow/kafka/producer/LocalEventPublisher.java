package com.bankflow.kafka.producer;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.bankflow.kafka.consumer.DomainEventHandler;
import com.bankflow.kafka.event.DomainEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Runs the handlers in-process. Default until Kafka is configured, and what the
 * test suite uses when a broker would add nothing.
 *
 * Each handler is isolated in its own try/catch to reproduce the property that
 * matters about separate consumer groups: a failing fraud rule must not stop
 * the audit row from being written.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "bankflow.events.publisher", havingValue = "local", matchIfMissing = true)
@RequiredArgsConstructor
public class LocalEventPublisher implements EventPublisher {

    private final List<DomainEventHandler> handlers;

    @Override
    public void publish(DomainEvent event) {
        log.debug("Dispatching {} in-process", event.getClass().getSimpleName());
        for (DomainEventHandler handler : handlers) {
            if (!handler.supports(event)) {
                continue;
            }
            try {
                handler.handle(event);
            } catch (RuntimeException e) {
                log.error(
                        "{} failed on {} (eventId {})",
                        handler.getClass().getSimpleName(),
                        event.getClass().getSimpleName(),
                        event.eventId(),
                        e);
            }
        }
    }
}
