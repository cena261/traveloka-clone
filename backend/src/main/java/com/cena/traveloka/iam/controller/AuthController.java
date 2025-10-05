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

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authenticationService;
    private final SessionService sessionService;

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

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @RequestHeader("Authorization") String authHeader
    ) {
        String token = extractToken(authHeader);
        log.info("Logout request with token");

        sessionService.terminateSessionByToken(token);

        return ApiResponse.success("Logout successful", null);
    }

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

    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        throw new IllegalArgumentException("Invalid Authorization header format");
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
