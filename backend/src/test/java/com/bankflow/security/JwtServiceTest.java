package com.bankflow.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.bankflow.entity.Role;
import com.bankflow.entity.User;
import com.bankflow.entity.enums.RoleName;
import com.bankflow.exception.BusinessException;
import com.bankflow.exception.ErrorCode;

import io.jsonwebtoken.Claims;

class JwtServiceTest {

    private static final String SECRET =
            "dGVzdC1zZWNyZXQtdGhhdC1pcy1sb25nLWVub3VnaC1mb3ItaHMyNTYtc2lnbmluZw==";

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 15, 7);
        jwtService.initialiseKey();

        user = new User();
        user.setId(42L);
        user.setEmail("asha.mehta@example.com");
        user.setRoles(Set.of(new Role(RoleName.ROLE_CUSTOMER)));
    }

    @Test
    void accessTokenCarriesSubjectUserIdAndRoles() {
        Claims claims = jwtService.parseAccessToken(jwtService.generateAccessToken(user));

        assertThat(claims.getSubject()).isEqualTo("asha.mehta@example.com");
        assertThat(jwtService.extractUserId(claims)).isEqualTo(42L);
        assertThat(jwtService.extractRoles(claims)).containsExactly("ROLE_CUSTOMER");
    }

    @Test
    void refreshTokenCarriesAUniqueJti() {
        JwtService.RefreshToken first = jwtService.generateRefreshToken(user);
        JwtService.RefreshToken second = jwtService.generateRefreshToken(user);

        assertThat(first.jti()).isNotBlank().isNotEqualTo(second.jti());
        assertThat(jwtService.parseRefreshToken(first.token()).getId()).isEqualTo(first.jti());
    }

    /** Without the type claim, a 7-day refresh token would work as an access token. */
    @Test
    void refreshTokenIsRejectedWhereAnAccessTokenIsExpected() {
        String refreshToken = jwtService.generateRefreshToken(user).token();

        assertThatThrownBy(() -> jwtService.parseAccessToken(refreshToken))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ErrorCode.TOKEN_INVALID);
    }

    @Test
    void accessTokenIsRejectedWhereARefreshTokenIsExpected() {
        String accessToken = jwtService.generateAccessToken(user);

        assertThatThrownBy(() -> jwtService.parseRefreshToken(accessToken))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        JwtService other = new JwtService(
                "YW5vdGhlci1zZWNyZXQtdGhhdC1pcy1hbHNvLWxvbmctZW5vdWdoLWZvci1oczI1Ng==", 15, 7);
        other.initialiseKey();
        String foreignToken = other.generateAccessToken(user);

        assertThatThrownBy(() -> jwtService.parseAccessToken(foreignToken))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ErrorCode.TOKEN_INVALID);
    }

    @Test
    void expiredTokenReportsExpiryRatherThanInvalidity() {
        JwtService shortLived = new JwtService(SECRET, 0, 7);
        shortLived.initialiseKey();
        String token = shortLived.generateAccessToken(user);

        assertThatThrownBy(() -> shortLived.parseAccessToken(token))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ErrorCode.TOKEN_EXPIRED);
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = jwtService.generateAccessToken(user);
        String tampered = token.substring(0, token.length() - 4) + "AAAA";

        assertThatThrownBy(() -> jwtService.parseAccessToken(tampered))
                .isInstanceOf(BusinessException.class);
    }

    /** A short secret would make HS256 signatures cheap to brute force. */
    @Test
    void shortSecretIsRefusedAtStartup() {
        JwtService weak = new JwtService("dG9vLXNob3J0", 15, 7);

        assertThatThrownBy(weak::initialiseKey)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 bytes");
    }
}
