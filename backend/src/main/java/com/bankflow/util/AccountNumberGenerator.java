package com.bankflow.util;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

import com.bankflow.repository.AccountRepository;

import lombok.RequiredArgsConstructor;

/**
 * Produces account numbers of the form ACC + 10 digits.
 *
 * Random rather than sequential on purpose: a sequential number would let a
 * customer guess that ACC0000000123 exists, which is information they should
 * not have. The collision check keeps it correct; the unique index on
 * account_number is the real guarantee.
 */
@Component
@RequiredArgsConstructor
public class AccountNumberGenerator {

    private static final String PREFIX = "ACC";
    private static final int DIGITS = 10;
    private static final int MAX_ATTEMPTS = 20;

    private final SecureRandom random = new SecureRandom();
    private final AccountRepository accountRepository;

    public String next() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String candidate = PREFIX + randomDigits();
            if (!accountRepository.existsByAccountNumber(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException(
                "Could not allocate a unique account number after " + MAX_ATTEMPTS + " attempts");
    }

    private String randomDigits() {
        StringBuilder digits = new StringBuilder(DIGITS);
        // First digit is 1-9 so the number always renders at full width.
        digits.append(1 + random.nextInt(9));
        for (int i = 1; i < DIGITS; i++) {
            digits.append(random.nextInt(10));
        }
        return digits.toString();
    }
}
