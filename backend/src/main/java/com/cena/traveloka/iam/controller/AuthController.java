package com.cena.traveloka.iam.controller;

import com.cena.traveloka.common.dto.ApiResponse;
import com.cena.traveloka.iam.dto.request.ChangePasswordRequest;
import com.cena.traveloka.iam.dto.request.LoginRequest;
import com.cena.traveloka.iam.dto.request.PasswordResetRequest;
import com.cena.traveloka.iam.dto.request.RefreshTokenRequest;
import com.cena.traveloka.iam.dto.request.RegisterRequest;
import com.cena.traveloka.iam.dto.request.VerifyEmailRequest;
import com.cena.traveloka.iam.dto.response.AuthResponse;
import com.cena.traveloka.iam.dto.response.UserDto;
import com.cena.traveloka.iam.service.AuthenticationService;
import com.cena.traveloka.iam.service.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * T061-T068: AuthController
 * REST API controller for authentication operations.
 *
 * Endpoints:
 * - POST /api/v1/auth/register (T061)
 * - POST /api/v1/auth/login (T062)
 * - POST /api/v1/auth/logout (T063)
 * - POST /api/v1/auth/refresh (T064)
 * - POST /api/v1/auth/forgot-password (T065)
 * - POST /api/v1/auth/reset-password (T066)
 * - POST /api/v1/auth/verify-email (T067)
 * - POST /api/v1/auth/change-password (T068)
 *
 * Constitutional Compliance:
 * - Principle III: Layered Architecture - Controller delegates to service layer
 * - Principle IV: Entity Immutability - Uses DTOs for API contracts
 * - FR-001: User registration and login
 * - FR-003: JWT-based authentication (1-hour expiry)
 * - FR-006: Password reset
 * - FR-010: Email verification
 * - NFR-001: Password complexity validation
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authenticationService;
    private final SessionService sessionService;

    /**
     * T061: User registration endpoint (FR-001).
     *
     * @param request Registration request with user details
     * @return ApiResponse with AuthResponse containing JWT tokens
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserDto> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest
    ) {
        log.info("Registration request for email: {}", request.getEmail());

        UserDto userDto = authenticationService.register(request);

        return ApiResponse.success(
                "User registered successfully. Please verify your email.",
                userDto
        );
    }

    /**
     * T062: User login endpoint (FR-001, FR-003).
     *
     * @param request Login credentials
     * @param httpRequest HTTP request for IP and user agent extraction
     * @return ApiResponse with AuthResponse containing JWT tokens
     */
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        log.info("Login attempt for email: {}", request.getEmail());

        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        AuthResponse authResponse = authenticationService.login(request, ipAddress, userAgent);

        return ApiResponse.success(
                "Login successful",
                authResponse
        );
    }

    /**
     * T063: User logout endpoint (FR-013).
     *
     * @param authHeader Authorization header with JWT token
     * @return ApiResponse with success message
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @RequestHeader("Authorization") String authHeader
    ) {
        String token = extractToken(authHeader);
        log.info("Logout request with token");

        sessionService.terminateSessionByToken(token);

        return ApiResponse.success("Logout successful", null);
    }

    /**
     * T064: Refresh access token endpoint (NFR-002).
     *
     * @param request Refresh token request
     * @return ApiResponse with new AuthResponse
     */
    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        log.info("Token refresh request");

        AuthResponse authResponse = authenticationService.refreshToken(request.getRefreshToken());

        return ApiResponse.success(
                "Token refreshed successfully",
                authResponse
        );
    }

    /**
     * T065: Forgot password endpoint (FR-006).
     * Sends password reset email to user.
     *
     * @param request Password reset request with email
     * @return ApiResponse with success message
     */
    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(
            @Valid @RequestBody PasswordResetRequest request
    ) {
        log.info("Forgot password request for email: {}", request.getEmail());

        authenticationService.forgotPassword(request);

        return ApiResponse.success(
                "Password reset email sent. Please check your inbox.",
                null
        );
    }

    /**
     * T066: Reset password endpoint (FR-006).
     * Resets password using token from email.
     *
     * @param request Password reset request with token and new password
     * @return ApiResponse with success message
     */
    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(
            @Valid @RequestBody PasswordResetRequest request
    ) {
        log.info("Password reset request with token");

        authenticationService.resetPassword(request);

        return ApiResponse.success(
                "Password reset successful. You can now login with your new password.",
                null
        );
    }

    /**
     * T067: Verify email endpoint (FR-010).
     * Verifies user email using token from verification email.
     *
     * @param request Email verification request with token
     * @return ApiResponse with success message
     */
    @PostMapping("/verify-email")
    public ApiResponse<Void> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request
    ) {
        log.info("Email verification request with token");

        authenticationService.verifyEmail(request);

        return ApiResponse.success(
                "Email verified successfully. You can now proceed with bookings.",
                null
        );
    }

    /**
     * T068: Change password endpoint (FR-004).
     * Allows authenticated user to change their password.
     *
     * @param request Change password request
     * @param authHeader Authorization header with JWT token
     * @return ApiResponse with success message
     */
    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @RequestHeader("Authorization") String authHeader
    ) {
        String token = extractToken(authHeader);
        log.info("Password change request");

        authenticationService.changePassword(request, token);

        return ApiResponse.success(
                "Password changed successfully",
                null
        );
    }

    /**
     * Extract JWT token from Authorization header.
     *
     * @param authHeader Authorization header (Bearer token)
     * @return JWT token string
     */
    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        throw new IllegalArgumentException("Invalid Authorization header format");
    }

    /**
     * Get client IP address from HTTP request.
     * Handles X-Forwarded-For header for proxied requests.
     *
     * @param request HTTP request
     * @return Client IP address
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
