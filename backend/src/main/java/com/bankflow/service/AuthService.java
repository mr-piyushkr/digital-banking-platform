package com.bankflow.service;

import java.time.Duration;
import java.time.Instant;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bankflow.dto.request.ChangePasswordRequest;
import com.bankflow.dto.request.LoginRequest;
import com.bankflow.dto.request.RefreshTokenRequest;
import com.bankflow.dto.request.RegisterRequest;
import com.bankflow.dto.response.AuthResponse;
import com.bankflow.dto.response.UserResponse;
import com.bankflow.entity.Role;
import com.bankflow.entity.User;
import com.bankflow.entity.enums.RoleName;
import com.bankflow.exception.BusinessException;
import com.bankflow.exception.DuplicateResourceException;
import com.bankflow.exception.ErrorCode;
import com.bankflow.exception.ResourceNotFoundException;
import com.bankflow.kafka.event.UserRegisteredEvent;
import com.bankflow.repository.RoleRepository;
import com.bankflow.repository.UserRepository;
import com.bankflow.security.JwtService;
import com.bankflow.security.RefreshTokenStore;
import com.bankflow.util.ReferenceGenerator;
import com.bankflow.util.RequestContext;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    /** Five wrong passwords, then a cool-off. Slows credential stuffing without locking a user out permanently. */
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenStore refreshTokenStore;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ReferenceGenerator referenceGenerator;
    private final RequestContext requestContext;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException(
                    ErrorCode.DUPLICATE_EMAIL, "An account with this email already exists.");
        }
        if (userRepository.existsByPhone(request.phone())) {
            throw new DuplicateResourceException(
                    ErrorCode.DUPLICATE_PHONE, "An account with this mobile number already exists.");
        }

        Role customerRole = roleRepository
                .findByName(RoleName.ROLE_CUSTOMER)
                .orElseThrow(() -> new IllegalStateException(
                        "ROLE_CUSTOMER missing — migration V2 has not been applied"));

        User user = new User();
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setEmail(email);
        user.setPhone(request.phone());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setAddressLine(blankToNull(request.addressLine()));
        user.setCity(blankToNull(request.city()));
        user.setState(blankToNull(request.state()));
        user.setPincode(blankToNull(request.pincode()));
        user.addRole(customerRole);

        User saved = userRepository.save(user);
        log.info("Registered user {} (id {})", saved.getEmail(), saved.getId());

        applicationEventPublisher.publishEvent(new UserRegisteredEvent(
                referenceGenerator.nextEventId(),
                Instant.now(),
                saved.getId(),
                saved.getEmail(),
                saved.getFullName(),
                requestContext.clientIp(),
                requestContext.userAgent()));

        return issueTokens(saved);
    }

    /**
     * The password comparison itself is delegated to Spring Security's
     * AuthenticationManager, so the encoder, the UserDetailsService and the
     * provider chain are all genuinely in the path.
     *
     * The user row is loaded first anyway, because the lockout counter lives on
     * it — and checking lock and enabled state here produces a specific error
     * code instead of the provider's generic one.
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();

        User user = userRepository
                .findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.INVALID_CREDENTIALS, "Email or password is incorrect."));

        if (user.isLocked()) {
            throw new BusinessException(
                    ErrorCode.ACCOUNT_LOCKED,
                    "Too many failed attempts. Try again after " + LOCK_DURATION.toMinutes() + " minutes.");
        }
        if (!user.isEnabled()) {
            throw new BusinessException(
                    ErrorCode.ACCOUNT_DISABLED, "This account has been disabled. Contact support.");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password()));
        } catch (AuthenticationException e) {
            registerFailedAttempt(user);
            throw new BusinessException(
                    ErrorCode.INVALID_CREDENTIALS, "Email or password is incorrect.");
        }

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);

        return issueTokens(user);
    }

    /**
     * Rotates the refresh token: the presented jti is revoked and a new one
     * issued. If a stolen token is replayed after the legitimate client has
     * refreshed, the jti is already gone and the replay fails.
     */
    @Transactional(readOnly = true)
    public AuthResponse refresh(RefreshTokenRequest request) {
        Claims claims = jwtService.parseRefreshToken(request.refreshToken());
        String jti = claims.getId();

        if (jti == null || !refreshTokenStore.isValid(jti)) {
            throw new BusinessException(
                    ErrorCode.TOKEN_INVALID, "This session is no longer valid. Sign in again.");
        }

        User user = userRepository
                .findById(jwtService.extractUserId(claims))
                .orElseThrow(() -> new ResourceNotFoundException("User", claims.getSubject()));

        if (!user.isEnabled()) {
            refreshTokenStore.revoke(jti);
            throw new BusinessException(
                    ErrorCode.ACCOUNT_DISABLED, "This account has been disabled. Contact support.");
        }

        refreshTokenStore.revoke(jti);
        return issueTokens(user);
    }

    public void logout(String refreshToken) {
        try {
            Claims claims = jwtService.parseRefreshToken(refreshToken);
            if (claims.getId() != null) {
                refreshTokenStore.revoke(claims.getId());
            }
        } catch (BusinessException e) {
            // An expired or malformed token means the session is already gone.
            log.debug("Logout with unusable token: {}", e.getMessage());
        }
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException(
                    ErrorCode.INVALID_CREDENTIALS, "Current password is incorrect.");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new BusinessException(
                    ErrorCode.INVALID_OPERATION, "New password must differ from the current one.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));

        // Every other device must be signed out, or a leaked password stays
        // usable through an existing refresh token.
        refreshTokenStore.revokeAllForUser(userId);
        log.info("Password changed for user id {}; all sessions revoked", userId);
    }

    private void registerFailedAttempt(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setLockedUntil(Instant.now().plus(LOCK_DURATION));
            log.warn("Locked user id {} after {} failed attempts", user.getId(), attempts);
        }
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        JwtService.RefreshToken refresh = jwtService.generateRefreshToken(user);
        refreshTokenStore.store(refresh.jti(), user.getId(), refresh.ttl());

        return AuthResponse.of(
                accessToken,
                refresh.token(),
                jwtService.getAccessTtlSeconds(),
                UserResponse.from(user));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
