package com.cena.traveloka.iam.controller;

import com.cena.traveloka.common.dto.ApiResponse;
import com.cena.traveloka.iam.dto.request.ChangePasswordRequest;
import com.cena.traveloka.iam.dto.request.LoginRequest;
import com.cena.traveloka.iam.dto.request.PasswordResetRequest;
import com.cena.traveloka.iam.dto.request.RefreshTokenRequest;
import com.cena.traveloka.iam.dto.request.RegisterRequest;
import com.cena.traveloka.iam.dto.request.VerifyEmailRequest;
import com.cena.traveloka.iam.dto.response.AuthResponse;
import com.cena.traveloka.iam.service.AuthenticationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T015: Test AuthController endpoints
 * Controller test for AuthController (TDD - Phase 3.2).
 *
 * Tests all authentication endpoints:
 * - POST /api/v1/auth/register
 * - POST /api/v1/auth/login
 * - POST /api/v1/auth/logout
 * - POST /api/v1/auth/refresh
 * - POST /api/v1/auth/forgot-password
 * - POST /api/v1/auth/reset-password
 * - POST /api/v1/auth/verify-email
 * - POST /api/v1/auth/change-password
 *
 * Constitutional Compliance:
 * - Principle VII: Test Coverage - TDD mandatory, tests before implementation
 * - Principle IV: Entity Immutability - Uses DTOs for API contracts
 * - FR-001: User registration and login
 * - FR-006: Password reset
 * - FR-010: Email verification
 */
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security for unit tests
@DisplayName("AuthController Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationService authenticationService;

    private RegisterRequest validRegisterRequest;
    private LoginRequest validLoginRequest;
    private RefreshTokenRequest validRefreshRequest;
    private PasswordResetRequest validPasswordResetRequest;
    private VerifyEmailRequest validVerifyEmailRequest;
    private ChangePasswordRequest validChangePasswordRequest;
    private AuthResponse authResponse;

    @BeforeEach
    void setUp() {
        // Setup valid request objects
        validRegisterRequest = RegisterRequest.builder()
                .username("testuser")
                .email("test@example.com")
                .password("Test@1234")
                .passwordConfirmation("Test@1234")
                .firstName("Test")
                .lastName("User")
                .acceptTerms(true)
                .build();

        validLoginRequest = LoginRequest.builder()
                .email("test@example.com")
                .password("Test@1234")
                .build();

        validRefreshRequest = RefreshTokenRequest.builder()
                .refreshToken("valid-refresh-token")
                .build();

        validPasswordResetRequest = PasswordResetRequest.builder()
                .email("test@example.com")
                .build();

        validVerifyEmailRequest = VerifyEmailRequest.builder()
                .token("verification-token")
                .build();

        validChangePasswordRequest = ChangePasswordRequest.builder()
                .currentPassword("Test@1234")
                .newPassword("NewTest@1234")
                .newPasswordConfirmation("NewTest@1234")
                .build();

        authResponse = AuthResponse.builder()
                .accessToken("jwt-access-token")
                .refreshToken("jwt-refresh-token")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .build();
    }

    @Nested
    @DisplayName("POST /api/v1/auth/register - User Registration (FR-001)")
    class RegisterTests {

        @Test
        @DisplayName("Should register new user successfully with valid request")
        void shouldRegisterNewUser_Success() throws Exception {
            // Given
            when(authenticationService.register(any(RegisterRequest.class)))
                    .thenReturn(authResponse);

            // When & Then
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRegisterRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.accessToken").value("jwt-access-token"))
                    .andExpect(jsonPath("$.data.refreshToken").value("jwt-refresh-token"))
                    .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.data.expiresIn").value(3600));

            verify(authenticationService).register(any(RegisterRequest.class));
        }

        @Test
        @DisplayName("Should fail registration when username is blank")
        void shouldFailRegistration_WhenUsernameIsBlank() throws Exception {
            // Given
            validRegisterRequest.setUsername("");

            // When & Then
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRegisterRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should fail registration when email is invalid")
        void shouldFailRegistration_WhenEmailIsInvalid() throws Exception {
            // Given
            validRegisterRequest.setEmail("invalid-email");

            // When & Then
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRegisterRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should fail registration when password is too short")
        void shouldFailRegistration_WhenPasswordTooShort() throws Exception {
            // Given
            validRegisterRequest.setPassword("Test@1");

            // When & Then
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRegisterRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should fail registration when password doesn't meet complexity requirements (NFR-001)")
        void shouldFailRegistration_WhenPasswordNotComplex() throws Exception {
            // Given
            validRegisterRequest.setPassword("simplepass");

            // When & Then
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRegisterRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should fail registration when terms not accepted")
        void shouldFailRegistration_WhenTermsNotAccepted() throws Exception {
            // Given
            validRegisterRequest.setAcceptTerms(false);

            // When & Then
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRegisterRequest)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/login - User Login (FR-001)")
    class LoginTests {

        @Test
        @DisplayName("Should login successfully with valid credentials")
        void shouldLogin_Success() throws Exception {
            // Given
            when(authenticationService.login(any(LoginRequest.class)))
                    .thenReturn(authResponse);

            // When & Then
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validLoginRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.accessToken").value("jwt-access-token"))
                    .andExpect(jsonPath("$.data.tokenType").value("Bearer"));

            verify(authenticationService).login(any(LoginRequest.class));
        }

        @Test
        @DisplayName("Should fail login when email is blank")
        void shouldFailLogin_WhenEmailIsBlank() throws Exception {
            // Given
            validLoginRequest.setEmail("");

            // When & Then
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validLoginRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should fail login when email format is invalid")
        void shouldFailLogin_WhenEmailInvalid() throws Exception {
            // Given
            validLoginRequest.setEmail("not-an-email");

            // When & Then
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validLoginRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should fail login when password is blank")
        void shouldFailLogin_WhenPasswordIsBlank() throws Exception {
            // Given
            validLoginRequest.setPassword("");

            // When & Then
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validLoginRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should login with 2FA code when provided")
        void shouldLogin_With2FACode() throws Exception {
            // Given
            validLoginRequest.setTwoFactorCode("123456");
            when(authenticationService.login(any(LoginRequest.class)))
                    .thenReturn(authResponse);

            // When & Then
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validLoginRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"));

            verify(authenticationService).login(any(LoginRequest.class));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/logout - User Logout (FR-013)")
    class LogoutTests {

        @Test
        @DisplayName("Should logout successfully")
        void shouldLogout_Success() throws Exception {
            // Given
            doNothing().when(authenticationService).logout(anyString());

            // When & Then
            mockMvc.perform(post("/api/v1/auth/logout")
                            .header("Authorization", "Bearer jwt-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.message").exists());

            verify(authenticationService).logout(anyString());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/refresh - Refresh Token (NFR-002)")
    class RefreshTokenTests {

        @Test
        @DisplayName("Should refresh token successfully with valid refresh token")
        void shouldRefreshToken_Success() throws Exception {
            // Given
            when(authenticationService.refreshToken(any(RefreshTokenRequest.class)))
                    .thenReturn(authResponse);

            // When & Then
            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRefreshRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.accessToken").value("jwt-access-token"))
                    .andExpect(jsonPath("$.data.refreshToken").value("jwt-refresh-token"));

            verify(authenticationService).refreshToken(any(RefreshTokenRequest.class));
        }

        @Test
        @DisplayName("Should fail refresh when refresh token is blank")
        void shouldFailRefresh_WhenTokenIsBlank() throws Exception {
            // Given
            validRefreshRequest.setRefreshToken("");

            // When & Then
            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRefreshRequest)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/forgot-password - Forgot Password (FR-006)")
    class ForgotPasswordTests {

        @Test
        @DisplayName("Should send password reset email successfully")
        void shouldSendResetEmail_Success() throws Exception {
            // Given
            doNothing().when(authenticationService).forgotPassword(any(PasswordResetRequest.class));

            // When & Then
            mockMvc.perform(post("/api/v1/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validPasswordResetRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.message").exists());

            verify(authenticationService).forgotPassword(any(PasswordResetRequest.class));
        }

        @Test
        @DisplayName("Should fail forgot password when email is blank")
        void shouldFailForgotPassword_WhenEmailBlank() throws Exception {
            // Given
            validPasswordResetRequest.setEmail("");

            // When & Then
            mockMvc.perform(post("/api/v1/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validPasswordResetRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should fail forgot password when email is invalid")
        void shouldFailForgotPassword_WhenEmailInvalid() throws Exception {
            // Given
            validPasswordResetRequest.setEmail("not-an-email");

            // When & Then
            mockMvc.perform(post("/api/v1/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validPasswordResetRequest)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/reset-password - Reset Password (FR-006)")
    class ResetPasswordTests {

        @Test
        @DisplayName("Should reset password successfully with valid token")
        void shouldResetPassword_Success() throws Exception {
            // Given
            PasswordResetRequest resetRequest = PasswordResetRequest.builder()
                    .token("valid-reset-token")
                    .newPassword("NewTest@1234")
                    .build();

            doNothing().when(authenticationService).resetPassword(any(PasswordResetRequest.class));

            // When & Then
            mockMvc.perform(post("/api/v1/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(resetRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.message").exists());

            verify(authenticationService).resetPassword(any(PasswordResetRequest.class));
        }

        @Test
        @DisplayName("Should fail reset password when token is blank")
        void shouldFailResetPassword_WhenTokenBlank() throws Exception {
            // Given
            PasswordResetRequest resetRequest = PasswordResetRequest.builder()
                    .token("")
                    .newPassword("NewTest@1234")
                    .build();

            // When & Then
            mockMvc.perform(post("/api/v1/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(resetRequest)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/verify-email - Email Verification (FR-010)")
    class VerifyEmailTests {

        @Test
        @DisplayName("Should verify email successfully with valid token")
        void shouldVerifyEmail_Success() throws Exception {
            // Given
            doNothing().when(authenticationService).verifyEmail(any(VerifyEmailRequest.class));

            // When & Then
            mockMvc.perform(post("/api/v1/auth/verify-email")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validVerifyEmailRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.message").exists());

            verify(authenticationService).verifyEmail(any(VerifyEmailRequest.class));
        }

        @Test
        @DisplayName("Should fail email verification when token is blank")
        void shouldFailVerifyEmail_WhenTokenBlank() throws Exception {
            // Given
            validVerifyEmailRequest.setToken("");

            // When & Then
            mockMvc.perform(post("/api/v1/auth/verify-email")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validVerifyEmailRequest)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/change-password - Change Password (FR-004)")
    class ChangePasswordTests {

        @Test
        @DisplayName("Should change password successfully with valid request")
        void shouldChangePassword_Success() throws Exception {
            // Given
            doNothing().when(authenticationService).changePassword(any(ChangePasswordRequest.class), anyString());

            // When & Then
            mockMvc.perform(post("/api/v1/auth/change-password")
                            .header("Authorization", "Bearer jwt-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validChangePasswordRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.message").exists());

            verify(authenticationService).changePassword(any(ChangePasswordRequest.class), anyString());
        }

        @Test
        @DisplayName("Should fail change password when current password is blank")
        void shouldFailChangePassword_WhenCurrentPasswordBlank() throws Exception {
            // Given
            validChangePasswordRequest.setCurrentPassword("");

            // When & Then
            mockMvc.perform(post("/api/v1/auth/change-password")
                            .header("Authorization", "Bearer jwt-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validChangePasswordRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should fail change password when new password is too short")
        void shouldFailChangePassword_WhenNewPasswordTooShort() throws Exception {
            // Given
            validChangePasswordRequest.setNewPassword("Test@1");

            // When & Then
            mockMvc.perform(post("/api/v1/auth/change-password")
                            .header("Authorization", "Bearer jwt-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validChangePasswordRequest)))
                    .andExpect(status().isBadRequest());
        }
    }
}
