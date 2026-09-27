package com.bankflow.service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.bankflow.entity.Notification;
import com.bankflow.entity.enums.TransactionType;
import com.bankflow.kafka.consumer.DomainEventHandler;
import com.bankflow.kafka.event.AccountStatusChangedEvent;
import com.bankflow.kafka.event.DomainEvent;
import com.bankflow.kafka.event.TransactionCreatedEvent;
import com.bankflow.kafka.event.TransactionFlaggedEvent;
import com.bankflow.kafka.event.UserRegisteredEvent;
import com.bankflow.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Turns events into in-app notifications. A real deployment would hand these to
 * an email or SMS provider; the row is written the same way either case.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService implements DomainEventHandler {

    private static final NumberFormat INR = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

    private final NotificationRepository notificationRepository;

    @Override
    public boolean supports(DomainEvent event) {
        return true;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(DomainEvent event) {
        if (notificationRepository.existsByEventId(event.eventId())) {
            return;
        }

        switch (event) {
            case UserRegisteredEvent e -> save(
                    e.userId(),
                    "Welcome to BankFlow",
                    "Your account is ready, " + e.fullName() + ". Open a bank account to get started.",
                    e.eventId());

            case TransactionCreatedEvent e -> {
                if (e.ownerUserId() != null) {
                    save(e.ownerUserId(), titleFor(e), bodyFor(e), e.eventId());
                }
            }

            case TransactionFlaggedEvent e -> {
                if (e.ownerUserId() != null) {
                    save(
                            e.ownerUserId(),
                            "Transaction under review",
                            "Transaction " + e.reference() + " has been held for review. "
                                    + "Our team will contact you if anything is needed.",
                            e.eventId());
                }
            }

            case AccountStatusChangedEvent e -> save(
                    e.ownerUserId(),
                    "Account status changed",
                    "Account " + e.accountNumber() + " is now " + e.newStatus().toLowerCase() + ".",
                    e.eventId());
        }
    }

    @Transactional(readOnly = true)
    public Page<Notification> forUser(Long userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public int markAllRead(Long userId) {
        return notificationRepository.markAllRead(userId);
    }

    private void save(Long userId, String title, String message, String eventId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setEventId(eventId);
        notificationRepository.save(notification);
    }

    private static String titleFor(TransactionCreatedEvent event) {
        return switch (TransactionType.valueOf(event.type())) {
            case DEPOSIT -> "Money credited";
            case WITHDRAWAL -> "Money debited";
            case TRANSFER -> "Transfer sent";
        };
    }

    private static String bodyFor(TransactionCreatedEvent event) {
        String amount = format(event.amount());
        return switch (TransactionType.valueOf(event.type())) {
            case DEPOSIT -> amount + " credited to " + event.toAccountNumber()
                    + ". Reference " + event.reference() + ".";
            case WITHDRAWAL -> amount + " debited from " + event.fromAccountNumber()
                    + ". Reference " + event.reference() + ".";
            case TRANSFER -> amount + " sent from " + event.fromAccountNumber()
                    + " to " + event.toAccountNumber() + ". Reference " + event.reference() + ".";
        };
    }

    private static String format(BigDecimal amount) {
        return INR.format(amount);
    }
}
