package com.bankflow.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.bankflow.entity.Role;
import com.bankflow.entity.User;
import com.bankflow.exception.BusinessException;
import com.bankflow.exception.ErrorCode;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;

/**
 * Issues and verifies both token types.
 *
 * Access tokens are short-lived and stateless — no lookup happens on a normal
 * request. Refresh tokens carry a jti that is tracked in a store, which is what
 * makes logout able to revoke a token that has not yet expired.
 */
@Service
public class JwtService {

    public static final String CLAIM_USER_ID = "uid";
    public static final String CLAIM_ROLES = "roles";
    public static final String CLAIM_TOKEN_TYPE = "typ";

    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final String secret;
    private final Duration accessTtl;
    private final Duration refreshTtl;
    private SecretKey signingKey;

    public JwtService(
            @Value("${JWT_SECRET}") String secret,
            @Value("${JWT_ACCESS_TTL_MINUTES:15}") long accessTtlMinutes,
            @Value("${JWT_REFRESH_TTL_DAYS:7}") long refreshTtlDays) {
        this.secret = secret;
        this.accessTtl = Duration.ofMinutes(accessTtlMinutes);
        this.refreshTtl = Duration.ofDays(refreshTtlDays);
    }

    @PostConstruct
    void initialiseKey() {
        byte[] keyBytes = decodeSecret(secret);
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET must decode to at least 32 bytes for HS256; got " + keyBytes.length);
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /** Accepts a Base64 secret, falling back to raw bytes for convenience. */
    private static byte[] decodeSecret(String value) {
        try {
            return Decoders.BASE64.decode(value);
        } catch (RuntimeException e) {
            return value.getBytes(StandardCharsets.UTF_8);
        }
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getEmail())
                .claim(CLAIM_USER_ID, user.getId())
                .claim(CLAIM_ROLES, user.getRoles().stream().map(Role::getName).map(Enum::name).toList())
                .claim(CLAIM_TOKEN_TYPE, TYPE_ACCESS)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTtl)))
                .signWith(signingKey)
                .compact();
    }

    /** The returned jti must be registered in the store by the caller. */
    public RefreshToken generateRefreshToken(User user) {
        Instant now = Instant.now();
        String jti = UUID.randomUUID().toString();
        String token = Jwts.builder()
                .id(jti)
                .subject(user.getEmail())
                .claim(CLAIM_USER_ID, user.getId())
                .claim(CLAIM_TOKEN_TYPE, TYPE_REFRESH)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(refreshTtl)))
                .signWith(signingKey)
                .compact();
        return new RefreshToken(token, jti, refreshTtl);
    }

    public Claims parse(String token) {
        try {
            return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
        } catch (ExpiredJwtException e) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED, "Session expired. Sign in again.");
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID, "Token is not valid.");
        }
    }

    /**
     * Rejects a refresh token presented as a bearer token, and vice versa. Without
     * this check a long-lived refresh token would work as an access token.
     */
    public Claims parseAccessToken(String token) {
        Claims claims = parse(token);
        if (!TYPE_ACCESS.equals(claims.get(CLAIM_TOKEN_TYPE, String.class))) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID, "Expected an access token.");
        }
        return claims;
    }

    public Claims parseRefreshToken(String token) {
        Claims claims = parse(token);
        if (!TYPE_REFRESH.equals(claims.get(CLAIM_TOKEN_TYPE, String.class))) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID, "Expected a refresh token.");
        }
        return claims;
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(Claims claims) {
        Object roles = claims.get(CLAIM_ROLES);
        return roles instanceof List<?> list ? (List<String>) list : List.of();
    }

    public Long extractUserId(Claims claims) {
        Number uid = claims.get(CLAIM_USER_ID, Number.class);
        return uid == null ? null : uid.longValue();
    }

    public long getAccessTtlSeconds() {
        return accessTtl.toSeconds();
    }

    public record RefreshToken(String token, String jti, Duration ttl) {}
}
