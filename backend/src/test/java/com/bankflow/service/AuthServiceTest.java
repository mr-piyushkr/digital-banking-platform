package com.bankflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.bankflow.dto.request.ChangePasswordRequest;
import com.bankflow.dto.request.LoginRequest;
import com.bankflow.dto.request.RegisterRequest;
import com.bankflow.dto.response.AuthResponse;
import com.bankflow.entity.Role;
import com.bankflow.entity.User;
import com.bankflow.entity.enums.RoleName;
import com.bankflow.exception.BusinessException;
import com.bankflow.exception.ErrorCode;
import com.bankflow.repository.RoleRepository;
import com.bankflow.repository.UserRepository;
import com.bankflow.security.JwtService;
import com.bankflow.security.RefreshTokenStore;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;
    @Mock private RefreshTokenStore refreshTokenStore;

    @InjectMocks private AuthService authService;

    private User existingUser;

    @BeforeEach
    void setUp() {
        existingUser = new User();
        existingUser.setId(7L);
        existingUser.setFirstName("Asha");
        existingUser.setLastName("Mehta");
        existingUser.setEmail("asha.mehta@example.com");
        existingUser.setPhone("9812345670");
        existingUser.setPasswordHash("$2a$12$storedhash");
        existingUser.setRoles(Set.of(new Role(RoleName.ROLE_CUSTOMER)));
    }

    // ---------- registration ----------

    @Test
    void registrationHashesThePasswordAndAssignsTheCustomerRole() {
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(userRepository.existsByPhone(anyString())).thenReturn(false);
        when(roleRepository.findByName(RoleName.ROLE_CUSTOMER))
                .thenReturn(Optional.of(new Role(RoleName.ROLE_CUSTOMER)));
        when(passwordEncoder.encode("Str0ngPassword")).thenReturn("$2a$12$newhash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        stubTokenIssuance();

        authService.register(registerRequest("New.User@Example.com", "9812345671"));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());

        assertThat(saved.getValue().getPasswordHash()).isEqualTo("$2a$12$newhash");
        assertThat(saved.getValue().hasRole(RoleName.ROLE_CUSTOMER)).isTrue();
        assertThat(saved.getValue().hasRole(RoleName.ROLE_ADMIN)).isFalse();
    }

    /** Case-insensitive, or the same person could register twice. */
    @Test
    void registrationLowercasesTheEmail() {
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(userRepository.existsByPhone(anyString())).thenReturn(false);
        when(roleRepository.findByName(RoleName.ROLE_CUSTOMER))
                .thenReturn(Optional.of(new Role(RoleName.ROLE_CUSTOMER)));
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$newhash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        stubTokenIssuance();

        authService.register(registerRequest("New.User@Example.COM", "9812345671"));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("new.user@example.com");
    }

    @Test
    void duplicateEmailIsRejected() {
        when(userRepository.existsByEmailIgnoreCase("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest("taken@example.com", "9812345671")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ErrorCode.DUPLICATE_EMAIL);

        verify(userRepository, never()).save(any());
    }

    @Test
    void duplicatePhoneIsRejected() {
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(userRepository.existsByPhone("9812345670")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest("fresh@example.com", "9812345670")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ErrorCode.DUPLICATE_PHONE);
    }

    // ---------- login ----------

    @Test
    void loginIssuesTokensAndRegistersTheRefreshJti() {
        when(userRepository.findByEmailIgnoreCase("asha.mehta@example.com"))
                .thenReturn(Optional.of(existingUser));
        stubTokenIssuance();

        AuthResponse response =
                authService.login(new LoginRequest("asha.mehta@example.com", "correct"));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        verify(refreshTokenStore).store(eq("jti-1"), eq(7L), any(Duration.class));
    }

    @Test
    void unknownEmailAndWrongPasswordReturnTheSameCode() {
        when(userRepository.findByEmailIgnoreCase("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("ghost@example.com", "whatever")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    void wrongPasswordIncrementsTheFailureCounter() {
        when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(existingUser));
        rejectCredentials();

        assertThatThrownBy(() -> authService.login(new LoginRequest("asha.mehta@example.com", "wrong")))
                .isInstanceOf(BusinessException.class);

        assertThat(existingUser.getFailedLoginAttempts()).isEqualTo(1);
        assertThat(existingUser.getLockedUntil()).isNull();
    }

    @Test
    void fifthConsecutiveFailureLocksTheAccount() {
        existingUser.setFailedLoginAttempts(4);
        when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(existingUser));
        rejectCredentials();

        assertThatThrownBy(() -> authService.login(new LoginRequest("asha.mehta@example.com", "wrong")))
                .isInstanceOf(BusinessException.class);

        assertThat(existingUser.getFailedLoginAttempts()).isEqualTo(5);
        assertThat(existingUser.isLocked()).isTrue();
    }

    @Test
    void lockedAccountIsRefusedEvenWithTheRightPassword() {
        existingUser.setLockedUntil(Instant.now().plusSeconds(600));
        when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(existingUser));

        assertThatThrownBy(() -> authService.login(new LoginRequest("asha.mehta@example.com", "correct")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ErrorCode.ACCOUNT_LOCKED);

        verify(authenticationManager, never()).authenticate(any(Authentication.class));
    }

    @Test
    void disabledAccountCannotLogIn() {
        existingUser.setEnabled(false);
        when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(existingUser));

        assertThatThrownBy(() -> authService.login(new LoginRequest("asha.mehta@example.com", "correct")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ErrorCode.ACCOUNT_DISABLED);
    }

    @Test
    void successfulLoginClearsAPreviousFailureCount() {
        existingUser.setFailedLoginAttempts(3);
        when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(existingUser));
        stubTokenIssuance();

        authService.login(new LoginRequest("asha.mehta@example.com", "correct"));

        assertThat(existingUser.getFailedLoginAttempts()).isZero();
        assertThat(existingUser.getLockedUntil()).isNull();
    }

    // ---------- password change ----------

    @Test
    void changingThePasswordRevokesEverySession() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("current", "$2a$12$storedhash")).thenReturn(true);
        when(passwordEncoder.matches("Str0ngNewPass", "$2a$12$storedhash")).thenReturn(false);
        when(passwordEncoder.encode("Str0ngNewPass")).thenReturn("$2a$12$rotated");

        authService.changePassword(7L, new ChangePasswordRequest("current", "Str0ngNewPass"));

        assertThat(existingUser.getPasswordHash()).isEqualTo("$2a$12$rotated");
        verify(refreshTokenStore).revokeAllForUser(7L);
    }

    @Test
    void changingThePasswordRequiresTheCurrentOne() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("guess", "$2a$12$storedhash")).thenReturn(false);

        assertThatThrownBy(() ->
                        authService.changePassword(7L, new ChangePasswordRequest("guess", "Str0ngNewPass")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);

        verify(refreshTokenStore, never()).revokeAllForUser(anyLong());
    }

    @Test
    void reusingTheCurrentPasswordIsRejected() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("current", "$2a$12$storedhash")).thenReturn(true);

        assertThatThrownBy(() ->
                        authService.changePassword(7L, new ChangePasswordRequest("current", "current")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ErrorCode.INVALID_OPERATION);
    }

    private void rejectCredentials() {
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("bad credentials"));
    }

    private void stubTokenIssuance() {
        when(jwtService.generateAccessToken(any(User.class))).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any(User.class)))
                .thenReturn(new JwtService.RefreshToken("refresh-token", "jti-1", Duration.ofDays(7)));
        when(jwtService.getAccessTtlSeconds()).thenReturn(900L);
    }

    private RegisterRequest registerRequest(String email, String phone) {
        return new RegisterRequest(
                "New", "User", email, phone, "Str0ngPassword", null, null, null, null);
    }
}
