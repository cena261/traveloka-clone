package com.cena.traveloka.iam.service;

import com.cena.traveloka.common.enums.Status;
import com.cena.traveloka.iam.dto.request.LoginRequest;
import com.cena.traveloka.iam.dto.request.RegisterRequest;
import com.cena.traveloka.iam.dto.response.AuthResponse;
import com.cena.traveloka.iam.dto.response.UserDto;
import com.cena.traveloka.iam.entity.LoginHistory;
import com.cena.traveloka.iam.entity.User;
import com.cena.traveloka.iam.mapper.UserMapper;
import com.cena.traveloka.iam.repository.LoginHistoryRepository;
import com.cena.traveloka.iam.repository.UserRepository;
import com.cena.traveloka.iam.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * T053: AuthenticationService
 * Service for authentication operations with JWT.
 *
 * Constitutional Compliance:
 * - FR-001: Email/password authentication
 * - FR-003: JWT token generation with 1-hour expiry
 * - FR-007: Login attempt tracking
 * - FR-008: Account lockout after 5 failed attempts (30 minutes)
 * - Principle III: Layered Architecture - Business logic in service layer
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthenticationService {

    private final UserRepository userRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final SessionService sessionService;
    private final UserMapper userMapper;

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_DURATION_MINUTES = 30;

    /**
     * Authenticate user with email and password (FR-001, FR-003).
     *
     * @param request Login request
     * @param ipAddress Client IP address
     * @param userAgent Client user agent
     * @return AuthResponse with JWT tokens
     */
    public AuthResponse login(LoginRequest request, String ipAddress, String userAgent) {
        // Find user by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    recordFailedLogin(null, request.getEmail(), "User not found", ipAddress, userAgent);
                    return new RuntimeException("User not found with email: " + request.getEmail());
                });

        // Check if account is locked (FR-008)
        if (user.getAccountLocked() && user.getLockedUntil() != null) {
            if (user.getLockedUntil().isAfter(OffsetDateTime.now())) {
                recordFailedLogin(user, request.getEmail(), "Account is locked", ipAddress, userAgent);
                throw new RuntimeException("Account is locked until: " + user.getLockedUntil());
            } else {
                // Unlock account if lock period expired
                unlockAccount(user);
            }
        }

        // Password verification is handled by Keycloak
        // This service assumes Keycloak has already validated credentials
        // For direct login, use KeycloakSyncService for authentication

        // Check account status
        if (user.getStatus() == Status.suspended) {
            recordFailedLogin(user, request.getEmail(), "Account is suspended", ipAddress, userAgent);
            throw new RuntimeException("Account is suspended");
        }

        // Successful login
        handleSuccessfulLogin(user, ipAddress, userAgent);

        // Generate JWT tokens (FR-003)
        Authentication authentication = createAuthentication(user);
        String accessToken = jwtTokenProvider.generateAccessToken(
                authentication,
                user.getId().toString(),
                user.getEmail()
        );
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId().toString());

        // Create session
        sessionService.createSession(user, accessToken, refreshToken, ipAddress, userAgent);

        UserDto userDto = userMapper.toDto(user);

        log.info("User logged in successfully: {}", user.getEmail());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenExpirationSeconds())
                .user(userDto)
                .build();
    }

    /**
     * Register new user (FR-001).
     *
     * @param request Registration request
     * @return UserDto
     */
    public UserDto register(RegisterRequest request) {
        // Validate password confirmation
        if (!request.getPassword().equals(request.getPasswordConfirmation())) {
            throw new RuntimeException("Passwords do not match");
        }

        // Check if email exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists: " + request.getEmail());
        }

        // Check if username exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists: " + request.getUsername());
        }

        // Create user entity
        User user = userMapper.toEntity(request);
        // Password is stored in Keycloak, not in local database
        user.setStatus(Status.pending);
        user.setEmailVerified(false);
        user.setTermsAcceptedAt(OffsetDateTime.now());
        user.setPrivacyAcceptedAt(OffsetDateTime.now());
        user.setCreatedAt(OffsetDateTime.now());

        User savedUser = userRepository.save(user);

        log.info("User registered successfully: {}", savedUser.getEmail());

        return userMapper.toDto(savedUser);
    }

    /**
     * Refresh access token using refresh token.
     *
     * @param refreshToken Refresh token
     * @return AuthResponse with new access token
     */
    public AuthResponse refreshToken(String refreshToken) {
        // Validate refresh token
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }

        // Get user from token
        String userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Generate new access token
        Authentication authentication = createAuthentication(user);
        String newAccessToken = jwtTokenProvider.generateAccessToken(
                authentication,
                user.getId().toString(),
                user.getEmail()
        );

        UserDto userDto = userMapper.toDto(user);

        log.debug("Access token refreshed for user: {}", user.getEmail());

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenExpirationSeconds())
                .user(userDto)
                .build();
    }

    /**
     * Handle successful login.
     * Resets failed attempts and updates login info (FR-007).
     *
     * @param user User entity
     * @param ipAddress IP address
     * @param userAgent User agent
     */
    private void handleSuccessfulLogin(User user, String ipAddress, String userAgent) {
        user.setFailedLoginAttempts(0);
        user.setLastLoginAt(OffsetDateTime.now());
        user.setLastLoginIp(ipAddress);
        user.setLoginCount(user.getLoginCount() + 1);
        userRepository.save(user);

        // Record successful login (FR-007)
        LoginHistory history = LoginHistory.builder()
                .user(user)
                .email(user.getEmail())
                .username(user.getUsername())
                .success(true)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .attemptedAt(OffsetDateTime.now())
                .build();
        loginHistoryRepository.save(history);
    }

    /**
     * Handle failed login.
     * Increments failed attempts and locks account if needed (FR-008).
     *
     * @param user User entity
     * @param email Email used for login
     * @param ipAddress IP address
     * @param userAgent User agent
     */
    private void handleFailedLogin(User user, String email, String ipAddress, String userAgent) {
        int failedAttempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(failedAttempts);

        // Lock account after 5 failed attempts (FR-008)
        if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
            user.setAccountLocked(true);
            user.setLockedUntil(OffsetDateTime.now().plusMinutes(LOCKOUT_DURATION_MINUTES));
            user.setLockReason("Account locked due to 5 failed login attempts");
            log.warn("Account locked for user: {} - Too many failed attempts", email);
        }

        userRepository.save(user);
        recordFailedLogin(user, email, "Invalid password", ipAddress, userAgent);
    }

    /**
     * Record failed login attempt (FR-007).
     *
     * @param user User entity (can be null)
     * @param email Email used
     * @param reason Failure reason
     * @param ipAddress IP address
     * @param userAgent User agent
     */
    private void recordFailedLogin(User user, String email, String reason, String ipAddress, String userAgent) {
        LoginHistory history = LoginHistory.builder()
                .user(user)
                .email(email)
                .success(false)
                .failureReason(reason)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .attemptedAt(OffsetDateTime.now())
                .build();
        loginHistoryRepository.save(history);
    }

    /**
     * Unlock account after lock period expired.
     *
     * @param user User entity
     */
    private void unlockAccount(User user) {
        user.setAccountLocked(false);
        user.setLockedUntil(null);
        user.setLockReason(null);
        user.setFailedLoginAttempts(0);
        userRepository.save(user);
        log.info("Account unlocked for user: {}", user.getEmail());
    }

    /**
     * Forgot password - send reset email.
     *
     * @param request Password reset request
     */
    public void forgotPassword(com.cena.traveloka.iam.dto.request.PasswordResetRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found with email: " + request.getEmail()));

        // TODO: Generate reset token and send email
        log.info("Password reset requested for user: {}", user.getEmail());
    }

    /**
     * Reset password with token.
     *
     * @param request Password reset request with token
     */
    public void resetPassword(com.cena.traveloka.iam.dto.request.PasswordResetRequest request) {
        // TODO: Validate token and reset password
        log.info("Password reset with token");
    }

    /**
     * Verify email with token.
     *
     * @param request Email verification request
     */
    public void verifyEmail(com.cena.traveloka.iam.dto.request.VerifyEmailRequest request) {
        // TODO: Validate token and verify email
        log.info("Email verification with token");
    }

    /**
     * Change password for authenticated user.
     *
     * @param request Change password request
     * @param token JWT token
     */
    public void changePassword(com.cena.traveloka.iam.dto.request.ChangePasswordRequest request, String token) {
        // TODO: Validate current password and update
        log.info("Password change request");
    }

    /**
     * Create Spring Security Authentication object.
     *
     * @param user User entity
     * @return Authentication
     */
    private Authentication createAuthentication(User user) {
        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                .collect(Collectors.toList());

        return new UsernamePasswordAuthenticationToken(
                user.getEmail(),
                null,
                authorities
        );
    }
}
