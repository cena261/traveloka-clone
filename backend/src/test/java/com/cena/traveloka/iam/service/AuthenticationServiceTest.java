package com.cena.traveloka.iam.service;

import com.cena.traveloka.common.enums.Status;
import com.cena.traveloka.iam.dto.request.LoginRequest;
import com.cena.traveloka.iam.dto.request.RegisterRequest;
import com.cena.traveloka.iam.dto.response.AuthResponse;
import com.cena.traveloka.iam.dto.response.UserDto;
import com.cena.traveloka.iam.entity.LoginHistory;
import com.cena.traveloka.iam.entity.User;
import com.cena.traveloka.iam.repository.LoginHistoryRepository;
import com.cena.traveloka.iam.repository.UserRepository;
import com.cena.traveloka.iam.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * T010: AuthenticationServiceTest
 * Service layer tests for authentication operations.
 *
 * TDD Phase: RED - These tests MUST fail before implementing AuthenticationService
 *
 * Constitutional Compliance:
 * - Principle VII: Test-First Development - Tests written before service implementation
 * - Tests FR-001: Email/password authentication
 * - Tests FR-003: JWT token generation with 1-hour expiry
 * - Tests FR-007: Login attempt tracking
 * - Tests FR-008: Account lockout after 5 failed attempts
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationService Tests")
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private LoginHistoryRepository loginHistoryRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private SessionService sessionService;

    @InjectMocks
    private AuthenticationService authenticationService;

    private User testUser;
    private LoginRequest loginRequest;
    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .username("johndoe")
                .email("john.doe@example.com")
                .firstName("John")
                .lastName("Doe")
                .status(Status.active)
                .emailVerified(true)
                .accountLocked(false)
                .failedLoginAttempts(0)
                .createdAt(OffsetDateTime.now())
                .build();

        loginRequest = LoginRequest.builder()
                .email("john.doe@example.com")
                .password("Password123!")
                .build();

        registerRequest = RegisterRequest.builder()
                .username("johndoe")
                .email("john.doe@example.com")
                .password("Password123!")
                .passwordConfirmation("Password123!")
                .firstName("John")
                .lastName("Doe")
                .acceptTerms(true)
                .build();
    }

    @Test
    @DisplayName("Should authenticate user with valid credentials (FR-001)")
    void shouldAuthenticateUserWithValidCredentials() {
        // Given
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));
        // Password validation handled by Keycloak
        when(jwtTokenProvider.generateAccessToken(any(), anyString(), anyString())).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(anyString())).thenReturn("refresh-token");
        when(jwtTokenProvider.getAccessTokenExpirationSeconds()).thenReturn(3600L);

        // When
        AuthResponse response = authenticationService.login(loginRequest, "192.168.1.1", "Mozilla/5.0");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(3600L);
        assertThat(response.getUser()).isNotNull();
        assertThat(response.getUser().getEmail()).isEqualTo(testUser.getEmail());

        verify(loginHistoryRepository).save(any(LoginHistory.class));
        verify(userRepository).save(argThat(user ->
                user.getFailedLoginAttempts() == 0 &&
                user.getLastLoginAt() != null
        ));
    }

    @Test
    @DisplayName("Should throw exception for invalid credentials")
    void shouldThrowExceptionForInvalidCredentials() {
        // Given
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));
        // Password validation handled by Keycloak

        // When/Then
        assertThatThrownBy(() -> authenticationService.login(loginRequest, "192.168.1.1", "Mozilla/5.0"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid credentials");

        verify(loginHistoryRepository).save(argThat(history ->
                !history.getSuccess() &&
                history.getFailureReason().contains("Invalid password")
        ));
    }

    @Test
    @DisplayName("Should throw exception for non-existent user")
    void shouldThrowExceptionForNonExistentUser() {
        // Given
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> authenticationService.login(loginRequest, "192.168.1.1", "Mozilla/5.0"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");

        verify(loginHistoryRepository).save(argThat(history ->
                !history.getSuccess() &&
                history.getFailureReason().contains("User not found")
        ));
    }

    @Test
    @DisplayName("Should increment failed login attempts on invalid password")
    void shouldIncrementFailedLoginAttempts() {
        // Given
        testUser.setFailedLoginAttempts(2);
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));
        // Password validation handled by Keycloak

        // When
        assertThatThrownBy(() -> authenticationService.login(loginRequest, "192.168.1.1", "Mozilla/5.0"))
                .isInstanceOf(RuntimeException.class);

        // Then
        verify(userRepository).save(argThat(user ->
                user.getFailedLoginAttempts() == 3
        ));
    }

    @Test
    @DisplayName("Should lock account after 5 failed attempts (FR-008)")
    void shouldLockAccountAfterFiveFailedAttempts() {
        // Given
        testUser.setFailedLoginAttempts(4);
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));
        // Password validation handled by Keycloak

        // When
        assertThatThrownBy(() -> authenticationService.login(loginRequest, "192.168.1.1", "Mozilla/5.0"))
                .isInstanceOf(RuntimeException.class);

        // Then
        verify(userRepository).save(argThat(user ->
                user.getFailedLoginAttempts() == 5 &&
                user.getAccountLocked() &&
                user.getLockedUntil() != null &&
                user.getLockReason().contains("5 failed login attempts")
        ));
    }

    @Test
    @DisplayName("Should throw exception for locked account")
    void shouldThrowExceptionForLockedAccount() {
        // Given
        testUser.setAccountLocked(true);
        testUser.setLockedUntil(OffsetDateTime.now().plusMinutes(30));
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));

        // When/Then
        assertThatThrownBy(() -> authenticationService.login(loginRequest, "192.168.1.1", "Mozilla/5.0"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Account is locked");
    }

    @Test
    @DisplayName("Should reset failed attempts on successful login")
    void shouldResetFailedAttemptsOnSuccessfulLogin() {
        // Given
        testUser.setFailedLoginAttempts(3);
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));
        // Password validation handled by Keycloak
        when(jwtTokenProvider.generateAccessToken(any(), anyString(), anyString())).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(anyString())).thenReturn("refresh-token");

        // When
        authenticationService.login(loginRequest, "192.168.1.1", "Mozilla/5.0");

        // Then
        verify(userRepository).save(argThat(user ->
                user.getFailedLoginAttempts() == 0
        ));
    }

    @Test
    @DisplayName("Should register new user (FR-001)")
    void shouldRegisterNewUser() {
        // Given
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserDto result = authenticationService.register(registerRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo(registerRequest.getEmail());
        assertThat(result.getUsername()).isEqualTo(registerRequest.getUsername());
        verify(userRepository).save(argThat(user ->
                user.getStatus() == Status.pending &&
                !user.getEmailVerified() &&
                user.getTermsAcceptedAt() != null
        ));
    }

    @Test
    @DisplayName("Should throw exception for duplicate email")
    void shouldThrowExceptionForDuplicateEmail() {
        // Given
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> authenticationService.register(registerRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Email already exists");
    }

    @Test
    @DisplayName("Should throw exception for duplicate username")
    void shouldThrowExceptionForDuplicateUsername() {
        // Given
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> authenticationService.register(registerRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Username already exists");
    }

    @Test
    @DisplayName("Should throw exception for password mismatch")
    void shouldThrowExceptionForPasswordMismatch() {
        // Given
        registerRequest.setPasswordConfirmation("DifferentPassword123!");

        // When/Then
        assertThatThrownBy(() -> authenticationService.register(registerRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Passwords do not match");
    }

    @Test
    @DisplayName("Should track login history (FR-007)")
    void shouldTrackLoginHistory() {
        // Given
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));
        // Password validation handled by Keycloak
        when(jwtTokenProvider.generateAccessToken(any(), anyString(), anyString())).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(anyString())).thenReturn("refresh-token");

        // When
        authenticationService.login(loginRequest, "192.168.1.100", "Mozilla/5.0 (Windows NT 10.0)");

        // Then
        verify(loginHistoryRepository).save(argThat(history ->
                history.getSuccess() &&
                history.getEmail().equals(loginRequest.getEmail()) &&
                history.getIpAddress().equals("192.168.1.100") &&
                history.getUserAgent().equals("Mozilla/5.0 (Windows NT 10.0)")
        ));
    }

    @Test
    @DisplayName("Should generate JWT tokens with 1-hour expiry (FR-003)")
    void shouldGenerateJwtTokensWithOneHourExpiry() {
        // Given
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));
        // Password validation handled by Keycloak
        when(jwtTokenProvider.generateAccessToken(any(), anyString(), anyString())).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(anyString())).thenReturn("refresh-token");
        when(jwtTokenProvider.getAccessTokenExpirationSeconds()).thenReturn(3600L); // 1 hour

        // When
        AuthResponse response = authenticationService.login(loginRequest, "192.168.1.1", "Mozilla/5.0");

        // Then
        assertThat(response.getExpiresIn()).isEqualTo(3600L);
        verify(jwtTokenProvider).generateAccessToken(any(), eq(testUser.getId().toString()), eq(testUser.getEmail()));
        verify(jwtTokenProvider).generateRefreshToken(testUser.getId().toString());
    }

    @Test
    @DisplayName("Should refresh access token")
    void shouldRefreshAccessToken() {
        // Given
        String refreshToken = "valid-refresh-token";
        String userId = testUser.getId().toString();

        when(jwtTokenProvider.validateToken(refreshToken)).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken(refreshToken)).thenReturn(userId);
        when(userRepository.findById(UUID.fromString(userId))).thenReturn(Optional.of(testUser));
        when(jwtTokenProvider.generateAccessToken(any(), anyString(), anyString())).thenReturn("new-access-token");
        when(jwtTokenProvider.getAccessTokenExpirationSeconds()).thenReturn(3600L);

        // When
        AuthResponse response = authenticationService.refreshToken(refreshToken);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo(refreshToken);
        verify(jwtTokenProvider).generateAccessToken(any(), eq(userId), eq(testUser.getEmail()));
    }

    @Test
    @DisplayName("Should throw exception for invalid refresh token")
    void shouldThrowExceptionForInvalidRefreshToken() {
        // Given
        String invalidToken = "invalid-token";
        when(jwtTokenProvider.validateToken(invalidToken)).thenReturn(false);

        // When/Then
        assertThatThrownBy(() -> authenticationService.refreshToken(invalidToken))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid refresh token");
    }
}
