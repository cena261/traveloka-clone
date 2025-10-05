package com.cena.traveloka.iam.integration;

import com.cena.traveloka.iam.dto.request.PasswordResetRequest;
import com.cena.traveloka.iam.dto.request.RegisterRequest;
import com.cena.traveloka.iam.entity.PasswordResetToken;
import com.cena.traveloka.iam.entity.User;
import com.cena.traveloka.iam.repository.PasswordResetTokenRepository;
import com.cena.traveloka.iam.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T093: Password reset request flow test.
 *
 * Tests password reset request flow including:
 * - Password reset request (forgot password) with valid email
 * - Token generation
 * - Token expiration (24 hours)
 * - Security: not revealing user existence
 * - Invalid email handling
 * - Multiple reset requests
 *
 * NOTE: This tests only the /forgot-password endpoint (PasswordResetRequest DTO).
 * Actual password reset with token would use a different endpoint/DTO.
 *
 * Uses TestContainers for PostgreSQL and Redis.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@Transactional
public class PasswordResetIntegrationTest {

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
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @BeforeEach
    void setUp() {
        passwordResetTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldRequestPasswordReset_WithValidEmail() throws Exception {
        registerUser("test@example.com");

        PasswordResetRequest request = PasswordResetRequest.builder()
                .email("test@example.com")
                .build();

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Password reset email sent successfully"));

        // Verify token created in database
        User user = userRepository.findByEmail("test@example.com").orElseThrow();
        List<PasswordResetToken> tokens = passwordResetTokenRepository.findByUserIdAndUsedFalse(user.getId());
        assertThat(tokens).isNotEmpty();

        PasswordResetToken token = tokens.get(0);
        assertThat(token.getToken()).isNotNull();
        assertThat(token.getUsed()).isFalse();
        assertThat(token.getExpiresAt()).isAfter(OffsetDateTime.now());
        assertThat(token.getExpiresAt()).isBefore(OffsetDateTime.now().plusHours(25)); // 24-hour expiry
    }

    @Test
    void shouldAcceptPasswordResetRequest_WithNonExistentEmail() throws Exception {
        // Password reset should not reveal if email exists (security)
        PasswordResetRequest request = PasswordResetRequest.builder()
                .email("nonexistent@example.com")
                .build();

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        // No token should be created
        assertThat(passwordResetTokenRepository.count()).isZero();
    }

    @Test
    void shouldCreatePasswordResetToken_OnRequest() throws Exception {
        registerUser("test@example.com");
        User user = userRepository.findByEmail("test@example.com").orElseThrow();

        PasswordResetRequest resetRequest = PasswordResetRequest.builder()
                .email("test@example.com")
                .build();

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetRequest)))
                .andExpect(status().isOk());

        // Verify token created
        List<PasswordResetToken> tokens = passwordResetTokenRepository
                .findByUserIdAndUsedFalse(user.getId());
        assertThat(tokens).hasSize(1);
        PasswordResetToken token = tokens.get(0);
        assertThat(token.getToken()).isNotNull();
        assertThat(token.getUsed()).isFalse();
        assertThat(token.getExpiresAt()).isAfter(OffsetDateTime.now());
    }

    @Test
    void shouldFailPasswordResetRequest_WithInvalidEmail() throws Exception {
        PasswordResetRequest resetRequest = PasswordResetRequest.builder()
                .email("invalid-email")
                .build();

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldFailPasswordResetRequest_WithMissingEmail() throws Exception {
        PasswordResetRequest resetRequest = PasswordResetRequest.builder()
                .build();

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldGenerateUniqueToken_ForEachRequest() throws Exception {
        registerUser("test@example.com");

        PasswordResetRequest request = PasswordResetRequest.builder()
                .email("test@example.com")
                .build();

        // Request 1
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // Request 2
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        User user = userRepository.findByEmail("test@example.com").orElseThrow();
        List<PasswordResetToken> tokens = passwordResetTokenRepository.findByUserIdAndUsedFalse(user.getId());

        // Should have at least 2 tokens (or 1 if implementation invalidates old ones)
        assertThat(tokens.size()).isGreaterThanOrEqualTo(1);

        // If multiple tokens, they should be unique
        if (tokens.size() > 1) {
            List<String> tokenValues = tokens.stream().map(PasswordResetToken::getToken).toList();
            assertThat(tokenValues).doesNotHaveDuplicates();
        }
    }

    @Test
    void shouldGenerateUniqueTokens_ForDifferentUsers() throws Exception {
        registerUser("user1@example.com");
        registerUser("user2@example.com");

        PasswordResetRequest request1 = PasswordResetRequest.builder()
                .email("user1@example.com")
                .build();

        PasswordResetRequest request2 = PasswordResetRequest.builder()
                .email("user2@example.com")
                .build();

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)));

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)));

        User user1 = userRepository.findByEmail("user1@example.com").orElseThrow();
        User user2 = userRepository.findByEmail("user2@example.com").orElseThrow();

        List<PasswordResetToken> tokens1 = passwordResetTokenRepository
                .findByUserIdAndUsedFalse(user1.getId());
        List<PasswordResetToken> tokens2 = passwordResetTokenRepository
                .findByUserIdAndUsedFalse(user2.getId());

        assertThat(tokens1).hasSize(1);
        assertThat(tokens2).hasSize(1);
        assertThat(tokens1.get(0).getToken()).isNotEqualTo(tokens2.get(0).getToken());
    }

    @Test
    void shouldSetTokenExpiry_To24Hours() throws Exception {
        registerUser("test@example.com");
        User user = userRepository.findByEmail("test@example.com").orElseThrow();

        PasswordResetRequest request = PasswordResetRequest.builder()
                .email("test@example.com")
                .build();

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        List<PasswordResetToken> tokens = passwordResetTokenRepository
                .findByUserIdAndUsedFalse(user.getId());
        PasswordResetToken token = tokens.get(0);

        // Verify expiry is ~24 hours
        long hoursDiff = java.time.Duration.between(
                token.getCreatedAt(),
                token.getExpiresAt()
        ).toHours();

        assertThat(hoursDiff).isGreaterThanOrEqualTo(23).isLessThanOrEqualTo(25);
    }

    @Test
    void shouldStoreTokenSecurely() throws Exception {
        registerUser("test@example.com");
        User user = userRepository.findByEmail("test@example.com").orElseThrow();

        PasswordResetRequest request = PasswordResetRequest.builder()
                .email("test@example.com")
                .build();

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        List<PasswordResetToken> tokens = passwordResetTokenRepository
                .findByUserIdAndUsedFalse(user.getId());
        PasswordResetToken token = tokens.get(0);

        // Verify token is a UUID or similar secure format
        assertThat(token.getToken()).isNotNull();
        assertThat(token.getToken().length()).isGreaterThan(20); // Reasonable minimum length
        assertThat(token.getToken()).matches("[a-zA-Z0-9-]+"); // Alphanumeric + hyphens
    }

    @Test
    void shouldAllowMultiplePasswordResetRequests() throws Exception {
        registerUser("test@example.com");

        PasswordResetRequest request = PasswordResetRequest.builder()
                .email("test@example.com")
                .build();

        // First request
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        Thread.sleep(10); // Small delay

        // Second request (should succeed)
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        User user = userRepository.findByEmail("test@example.com").orElseThrow();
        List<PasswordResetToken> tokens = passwordResetTokenRepository
                .findByUserIdAndUsedFalse(user.getId());

        // Should have tokens (implementation may invalidate old ones or keep them)
        assertThat(tokens).isNotEmpty();
    }

    // Helper method

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
}
