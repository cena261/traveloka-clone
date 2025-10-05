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

    public AuthResponse login(LoginRequest request, String ipAddress, String userAgent) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    recordFailedLogin(null, request.getEmail(), "User not found", ipAddress, userAgent);
                    return new RuntimeException("User not found with email: " + request.getEmail());
                });

        if (user.getAccountLocked() && user.getLockedUntil() != null) {
            if (user.getLockedUntil().isAfter(OffsetDateTime.now())) {
                recordFailedLogin(user, request.getEmail(), "Account is locked", ipAddress, userAgent);
                throw new RuntimeException("Account is locked until: " + user.getLockedUntil());
            } else {
                unlockAccount(user);
            }
        }


        if (user.getStatus() == Status.suspended) {
            recordFailedLogin(user, request.getEmail(), "Account is suspended", ipAddress, userAgent);
            throw new RuntimeException("Account is suspended");
        }

        handleSuccessfulLogin(user, ipAddress, userAgent);

        Authentication authentication = createAuthentication(user);
        String accessToken = jwtTokenProvider.generateAccessToken(
                authentication,
                user.getId().toString(),
                user.getEmail()
        );
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId().toString());

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

    public UserDto register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getPasswordConfirmation())) {
            throw new RuntimeException("Passwords do not match");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists: " + request.getEmail());
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists: " + request.getUsername());
        }

        User user = userMapper.toEntity(request);
        user.setStatus(Status.pending);
        user.setEmailVerified(false);
        user.setTermsAcceptedAt(OffsetDateTime.now());
        user.setPrivacyAcceptedAt(OffsetDateTime.now());
        user.setCreatedAt(OffsetDateTime.now());

        User savedUser = userRepository.save(user);

        log.info("User registered successfully: {}", savedUser.getEmail());

        return userMapper.toDto(savedUser);
    }

    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }

        String userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new RuntimeException("User not found"));

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

    private void handleSuccessfulLogin(User user, String ipAddress, String userAgent) {
        user.setFailedLoginAttempts(0);
        user.setLastLoginAt(OffsetDateTime.now());
        user.setLastLoginIp(ipAddress);
        user.setLoginCount(user.getLoginCount() + 1);
        userRepository.save(user);

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

    private void handleFailedLogin(User user, String email, String ipAddress, String userAgent) {
        int failedAttempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(failedAttempts);

        if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
            user.setAccountLocked(true);
            user.setLockedUntil(OffsetDateTime.now().plusMinutes(LOCKOUT_DURATION_MINUTES));
            user.setLockReason("Account locked due to 5 failed login attempts");
            log.warn("Account locked for user: {} - Too many failed attempts", email);
        }

        userRepository.save(user);
        recordFailedLogin(user, email, "Invalid password", ipAddress, userAgent);
    }

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

    private void unlockAccount(User user) {
        user.setAccountLocked(false);
        user.setLockedUntil(null);
        user.setLockReason(null);
        user.setFailedLoginAttempts(0);
        userRepository.save(user);
        log.info("Account unlocked for user: {}", user.getEmail());
    }

    public void forgotPassword(com.cena.traveloka.iam.dto.request.PasswordResetRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found with email: " + request.getEmail()));

        log.info("Password reset requested for user: {}", user.getEmail());
    }

    public void resetPassword(com.cena.traveloka.iam.dto.request.PasswordResetRequest request) {
        log.info("Password reset with token");
    }

    public void verifyEmail(com.cena.traveloka.iam.dto.request.VerifyEmailRequest request) {
        log.info("Email verification with token");
    }

    public void changePassword(com.cena.traveloka.iam.dto.request.ChangePasswordRequest request, String token) {
        log.info("Password change request");
    }

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
