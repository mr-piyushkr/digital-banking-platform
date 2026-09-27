package com.bankflow.util;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

/**
 * Transaction references in the shape TXN20260928K7F3Q2 — date-prefixed so a
 * support conversation can be narrowed by day, with random suffix characters
 * from an alphabet that excludes I, O, 0 and 1 to avoid misreads over a phone.
 */
@Component
public class ReferenceGenerator {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");
    private static final char[] ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int SUFFIX_LENGTH = 6;

    private final SecureRandom random = new SecureRandom();

    public String nextTransactionReference() {
        StringBuilder reference = new StringBuilder("TXN");
        reference.append(LocalDate.now(ZONE).format(DATE));
        for (int i = 0; i < SUFFIX_LENGTH; i++) {
            reference.append(ALPHABET[random.nextInt(ALPHABET.length)]);
        }
        return reference.toString();
    }

    /** Used when a client does not send an Idempotency-Key header. */
    public String nextIdempotencyKey() {
        return "auto-" + java.util.UUID.randomUUID();
    }

    public String nextEventId() {
        return java.util.UUID.randomUUID().toString();
    }
}
