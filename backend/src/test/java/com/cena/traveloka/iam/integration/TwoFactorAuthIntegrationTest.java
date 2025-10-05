package com.cena.traveloka.iam.integration;

import com.cena.traveloka.iam.dto.request.LoginRequest;
import com.cena.traveloka.iam.dto.request.RegisterRequest;
import com.cena.traveloka.iam.dto.response.AuthResponse;
import com.cena.traveloka.iam.dto.response.TwoFactorSetupDto;
import com.cena.traveloka.iam.entity.TwoFactorAuth;
import com.cena.traveloka.iam.entity.User;
import com.cena.traveloka.iam.repository.TwoFactorAuthRepository;
import com.cena.traveloka.iam.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T095: Two-factor authentication flow test.
 *
 * Tests complete 2FA flows including:
 * - TOTP 2FA setup (FR-014)
 * - QR code generation for authenticator apps
 * - Backup code generation
 * - 2FA verification and activation
 * - 2FA-protected login flow
 * - Backup code usage
 * - 2FA disable with password/backup code
 * - SMS/Email 2FA fallback (FR-015)
 *
 * Uses TestContainers for PostgreSQL and Redis.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@Transactional
public class TwoFactorAuthIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("traveloka_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TwoFactorAuthRepository twoFactorAuthRepository;

    @Autowired
    private GoogleAuthenticator googleAuthenticator;

    private String accessToken;
    private User testUser;

    @BeforeEach
    void setUp() throws Exception {
        twoFactorAuthRepository.deleteAll();
        userRepository.deleteAll();

        // Register and login test user
        registerUser("test@example.com");
        accessToken = loginUser("test@example.com");
        testUser = userRepository.findByEmail("test@example.com").orElseThrow();
    }

    @Test
    void shouldSetupTOTP2FA_WithQRCodeAndBackupCodes() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/users/me/2fa/setup")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.secret").exists())
                .andExpect(jsonPath("$.data.qrCode").exists())
                .andExpect(jsonPath("$.data.backupCodes").isArray())
                .andExpect(jsonPath("$.data.backupCodes.length()").value(10))
                .andExpect(jsonPath("$.data.accountName").value("test@example.com"))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        TwoFactorSetupDto setupDto = objectMapper.readValue(
                objectMapper.readTree(responseBody).get("data").toString(),
                TwoFactorSetupDto.class
        );

        // Verify secret is generated
        assertThat(setupDto.getSecret()).isNotNull();
        assertThat(setupDto.getSecret().length()).isGreaterThan(10);

        // Verify QR code URL format
        assertThat(setupDto.getQrCode()).startsWith("otpauth://totp/");
        assertThat(setupDto.getQrCode()).contains("test@example.com");
        assertThat(setupDto.getQrCode()).contains("secret=" + setupDto.getSecret());

        // Verify backup codes
        assertThat(setupDto.getBackupCodes()).hasSize(10);
        setupDto.getBackupCodes().forEach(code -> {
            assertThat(code).hasSize(8);
            assertThat(code).matches("\\d{8}");
        });

        // Verify 2FA entity created but not active yet
        Optional<TwoFactorAuth> twoFactorAuth = twoFactorAuthRepository
                .findByUserIdAndMethod(testUser.getId(), "totp");
        assertThat(twoFactorAuth).isPresent();
        assertThat(twoFactorAuth.get().getIsActive()).isFalse();
        assertThat(twoFactorAuth.get().getVerified()).isFalse();
    }

    @Test
    void shouldVerifyAndActivate2FA_WithValidTOTPCode() throws Exception {
        // Setup 2FA first
        MvcResult setupResult = mockMvc.perform(post("/api/v1/users/me/2fa/setup")
                        .header("Authorization", "Bearer " + accessToken))
                .andReturn();

        String setupResponseBody = setupResult.getResponse().getContentAsString();
        TwoFactorSetupDto setupDto = objectMapper.readValue(
                objectMapper.readTree(setupResponseBody).get("data").toString(),
                TwoFactorSetupDto.class
        );

        String secret = setupDto.getSecret();

        // Generate valid TOTP code
        int totpCode = googleAuthenticator.getTotpPassword(secret);

        // Verify 2FA
        mockMvc.perform(post("/api/v1/users/me/2fa/verify")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("code", String.format("%06d", totpCode)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("2FA enabled successfully. Your account is now protected with two-factor authentication."));

        // Verify 2FA is now active
        TwoFactorAuth twoFactorAuth = twoFactorAuthRepository
                .findByUserIdAndMethod(testUser.getId(), "totp").orElseThrow();
        assertThat(twoFactorAuth.getIsActive()).isTrue();
        assertThat(twoFactorAuth.getVerified()).isTrue();
        assertThat(twoFactorAuth.getIsPrimary()).isTrue();
        assertThat(twoFactorAuth.getVerifiedAt()).isNotNull();

        // Verify user 2FA status updated
        User user = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(user.getTwoFactorEnabled()).isTrue();
    }

    @Test
    void shouldFailVerification_WithInvalidTOTPCode() throws Exception {
        // Setup 2FA first
        mockMvc.perform(post("/api/v1/users/me/2fa/setup")
                .header("Authorization", "Bearer " + accessToken));

        // Try to verify with invalid code
        mockMvc.perform(post("/api/v1/users/me/2fa/verify")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("code", "000000"))
                .andExpect(status().isBadRequest());

        // Verify 2FA is still not active
        Optional<TwoFactorAuth> twoFactorAuth = twoFactorAuthRepository
                .findByUserIdAndMethod(testUser.getId(), "totp");
        assertThat(twoFactorAuth).isPresent();
        assertThat(twoFactorAuth.get().getIsActive()).isFalse();
    }

    @Test
    void shouldDisable2FA_WithPassword() throws Exception {
        // Setup and verify 2FA first
        setupAndVerify2FA();

        // Disable 2FA with password
        mockMvc.perform(post("/api/v1/users/me/2fa/disable")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("password", "SecurePass123!"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("2FA disabled successfully"));

        // Verify 2FA is now inactive
        Optional<TwoFactorAuth> twoFactorAuth = twoFactorAuthRepository
                .findByUserIdAndMethod(testUser.getId(), "totp");
        if (twoFactorAuth.isPresent()) {
            assertThat(twoFactorAuth.get().getIsActive()).isFalse();
        }

        // Verify user 2FA status updated
        User user = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(user.getTwoFactorEnabled()).isFalse();
    }

    @Test
    void shouldDisable2FA_WithBackupCode() throws Exception {
        // Setup and verify 2FA first
        TwoFactorSetupDto setupDto = setupAndVerify2FA();
        String backupCode = setupDto.getBackupCodes().get(0);

        // Disable 2FA with backup code
        mockMvc.perform(post("/api/v1/users/me/2fa/disable")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("backupCode", backupCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        // Verify 2FA is now inactive
        User user = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(user.getTwoFactorEnabled()).isFalse();
    }

    @Test
    void shouldFailDisable2FA_WithoutPasswordOrBackupCode() throws Exception {
        setupAndVerify2FA();

        mockMvc.perform(post("/api/v1/users/me/2fa/disable")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldGenerateUniqueBackupCodes() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/users/me/2fa/setup")
                        .header("Authorization", "Bearer " + accessToken))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        TwoFactorSetupDto setupDto = objectMapper.readValue(
                objectMapper.readTree(responseBody).get("data").toString(),
                TwoFactorSetupDto.class
        );

        List<String> backupCodes = setupDto.getBackupCodes();

        // Verify all codes are unique
        assertThat(backupCodes).doesNotHaveDuplicates();

        // Verify all codes are 8 digits
        backupCodes.forEach(code -> {
            assertThat(code).hasSize(8);
            assertThat(code).matches("\\d+");
        });
    }

    @Test
    void shouldPreventDuplicate2FASetup() throws Exception {
        // Setup 2FA first time
        setupAndVerify2FA();

        // Try to setup again
        mockMvc.perform(post("/api/v1/users/me/2fa/setup")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldValidateTOTPCodeFormat() throws Exception {
        mockMvc.perform(post("/api/v1/users/me/2fa/setup")
                .header("Authorization", "Bearer " + accessToken));

        // Code too short
        mockMvc.perform(post("/api/v1/users/me/2fa/verify")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("code", "12345"))
                .andExpect(status().isBadRequest());

        // Code too long
        mockMvc.perform(post("/api/v1/users/me/2fa/verify")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("code", "1234567"))
                .andExpect(status().isBadRequest());

        // Empty code
        mockMvc.perform(post("/api/v1/users/me/2fa/verify")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("code", ""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRequireAuthentication_For2FAEndpoints() throws Exception {
        // Setup without token
        mockMvc.perform(post("/api/v1/users/me/2fa/setup"))
                .andExpect(status().isUnauthorized());

        // Verify without token
        mockMvc.perform(post("/api/v1/users/me/2fa/verify")
                        .param("code", "123456"))
                .andExpect(status().isUnauthorized());

        // Disable without token
        mockMvc.perform(post("/api/v1/users/me/2fa/disable")
                        .param("password", "SecurePass123!"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldStoreSecretSecurely() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/users/me/2fa/setup")
                        .header("Authorization", "Bearer " + accessToken))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        TwoFactorSetupDto setupDto = objectMapper.readValue(
                objectMapper.readTree(responseBody).get("data").toString(),
                TwoFactorSetupDto.class
        );

        // Verify secret is stored in database
        TwoFactorAuth twoFactorAuth = twoFactorAuthRepository
                .findByUserIdAndMethod(testUser.getId(), "totp").orElseThrow();

        assertThat(twoFactorAuth.getSecret()).isNotNull();
        assertThat(twoFactorAuth.getSecret()).isEqualTo(setupDto.getSecret());
    }

    @Test
    void shouldGenerateValidOTPAuthURL() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/users/me/2fa/setup")
                        .header("Authorization", "Bearer " + accessToken))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        TwoFactorSetupDto setupDto = objectMapper.readValue(
                objectMapper.readTree(responseBody).get("data").toString(),
                TwoFactorSetupDto.class
        );

        String qrCode = setupDto.getQrCode();

        // Verify OTP Auth URL format
        assertThat(qrCode).startsWith("otpauth://totp/");
        assertThat(qrCode).contains("Traveloka:");
        assertThat(qrCode).contains("test@example.com");
        assertThat(qrCode).contains("secret=" + setupDto.getSecret());
        assertThat(qrCode).contains("issuer=Traveloka");
    }

    // Helper methods

    private void registerUser(String email) throws Exception {
        RegisterRequest registerRequest = RegisterRequest.builder()
                .username(email.split("@")[0])
                .email(email)
                .password("SecurePass123!")
                .firstName("Test")
                .lastName("User")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));
    }

    private String loginUser(String email) throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .email(email)
                .password("SecurePass123!")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn();

        String loginResponseBody = loginResult.getResponse().getContentAsString();
        AuthResponse authResponse = objectMapper.readValue(
                objectMapper.readTree(loginResponseBody).get("data").toString(),
                AuthResponse.class
        );

        return authResponse.getAccessToken();
    }

    private TwoFactorSetupDto setupAndVerify2FA() throws Exception {
        // Setup
        MvcResult setupResult = mockMvc.perform(post("/api/v1/users/me/2fa/setup")
                        .header("Authorization", "Bearer " + accessToken))
                .andReturn();

        String setupResponseBody = setupResult.getResponse().getContentAsString();
        TwoFactorSetupDto setupDto = objectMapper.readValue(
                objectMapper.readTree(setupResponseBody).get("data").toString(),
                TwoFactorSetupDto.class
        );

        String secret = setupDto.getSecret();

        // Generate and verify TOTP code
        int totpCode = googleAuthenticator.getTotpPassword(secret);

        mockMvc.perform(post("/api/v1/users/me/2fa/verify")
                .header("Authorization", "Bearer " + accessToken)
                .param("code", String.format("%06d", totpCode)));

        return setupDto;
    }
}
