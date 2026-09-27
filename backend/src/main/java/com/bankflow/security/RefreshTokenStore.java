package com.bankflow.security;

import java.time.Duration;

/**
 * Tracks which refresh tokens are still valid, so logout can revoke a token
 * that has not expired yet.
 *
 * JWTs are stateless by design, which means a stolen refresh token would stay
 * usable for its full lifetime. Keeping a small allow-list of jtis is the
 * standard trade-off: access tokens remain stateless and fast, and only the
 * infrequent refresh path touches the store.
 *
 * Phase 6 replaces the in-memory implementation with Redis, at which point the
 * list is shared across instances and survives a restart.
 */
public interface RefreshTokenStore {

    void store(String jti, Long userId, Duration ttl);

    boolean isValid(String jti);

    void revoke(String jti);

    /** Used when a password changes — every existing session must die. */
    void revokeAllForUser(Long userId);
}
