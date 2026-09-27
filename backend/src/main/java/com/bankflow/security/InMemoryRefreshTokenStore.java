package com.bankflow.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Default store until Redis arrives in Phase 6. Backed by a concurrent map, so
 * it is correct for a single instance but does not survive a restart and is not
 * shared across instances — which is exactly why Redis replaces it.
 *
 * Expired entries are dropped on read rather than by a scheduler; the map only
 * ever holds one entry per active session.
 */
@Component
@ConditionalOnProperty(
        name = "bankflow.security.refresh-token-store",
        havingValue = "memory",
        matchIfMissing = true)
public class InMemoryRefreshTokenStore implements RefreshTokenStore {

    private final Map<String, Entry> tokens = new ConcurrentHashMap<>();

    @Override
    public void store(String jti, Long userId, Duration ttl) {
        tokens.put(jti, new Entry(userId, Instant.now().plus(ttl)));
    }

    @Override
    public boolean isValid(String jti) {
        Entry entry = tokens.get(jti);
        if (entry == null) {
            return false;
        }
        if (entry.expiresAt().isBefore(Instant.now())) {
            tokens.remove(jti);
            return false;
        }
        return true;
    }

    @Override
    public void revoke(String jti) {
        tokens.remove(jti);
    }

    @Override
    public void revokeAllForUser(Long userId) {
        tokens.entrySet().removeIf(entry -> entry.getValue().userId().equals(userId));
    }

    private record Entry(Long userId, Instant expiresAt) {}
}
