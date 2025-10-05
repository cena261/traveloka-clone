package com.cena.traveloka.iam.integration;

import com.cena.traveloka.common.dto.ApiResponse;
import com.cena.traveloka.iam.dto.request.LoginRequest;
import com.cena.traveloka.iam.dto.request.RefreshTokenRequest;
import com.cena.traveloka.iam.dto.request.RegisterRequest;
import com.cena.traveloka.iam.dto.response.AuthResponse;
import com.cena.traveloka.iam.dto.response.UserDto;
import com.cena.traveloka.iam.entity.Session;
import com.cena.traveloka.iam.entity.User;
import com.cena.traveloka.iam.repository.IamSessionRepository;
import com.cena.traveloka.iam.repository.UserRepository;
import com.cena.traveloka.iam.security.JwtTokenProvider;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * T089: Authentication flow integration test.
 *
 * Tests complete authentication flows including:
 * - User registration → login → session creation
 * - JWT token generation and validation
 * - Refresh token flow
 * - Logout and session termination
 * - Login failure scenarios
 *
 * Uses TestContainers for PostgreSQL and Redis.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@Transactional
public class AuthenticationIntegrationTest {

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
    private IamSessionRepository sessionRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        sessionRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldCompleteFullAuthenticationFlow_RegisterLoginRefreshLogout() throws Exception {
        // Step 1: Register new user
        RegisterRequest registerRequest = RegisterRequest.builder()
                .username("testuser")
                .email("testuser@example.com")
                .password("SecurePass123!")
                .firstName("Test")
                .lastName("User")
                .phone("+84901234567")
                .build();

        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.username").value("testuser"))
                .andExpect(jsonPath("$.data.email").value("testuser@example.com"))
                .andReturn();

        // Verify user created in database
        User createdUser = userRepository.findByEmail("testuser@example.com").orElseThrow();
        assertThat(createdUser.getUsername()).isEqualTo("testuser");
        assertThat(createdUser.getEmailVerified()).isFalse(); // Email not verified yet

        // Step 2: Login with credentials
        LoginRequest loginRequest = LoginRequest.builder()
                .email("testuser@example.com")
                .password("SecurePass123!")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest))
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                        .header("X-Forwarded-For", "192.168.1.100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(3600))
                .andExpect(jsonPath("$.data.user.username").value("testuser"))
                .andReturn();

        String loginResponseBody = loginResult.getResponse().getContentAsString();
        AuthResponse authResponse = objectMapper.readValue(
                objectMapper.readTree(loginResponseBody).get("data").toString(),
                AuthResponse.class
        );

        String accessToken = authResponse.getAccessToken();
        String refreshToken = authResponse.getRefreshToken();

        // Verify JWT token is valid
        assertThat(jwtTokenProvider.validateToken(accessToken)).isTrue();
        assertThat(jwtTokenProvider.getUserIdFromToken(accessToken)).isEqualTo(createdUser.getId().toString());

        // Verify session created in database
        List<Session> sessions = sessionRepository.findByUserIdAndIsActiveTrue(createdUser.getId());
        assertThat(sessions).hasSize(1);
        Session session = sessions.get(0);
        assertThat(session.getIpAddress()).isEqualTo("192.168.1.100");
        assertThat(session.getDeviceType()).isEqualTo("desktop");
        assertThat(session.getBrowser()).contains("Chrome");

        // Step 3: Refresh token
        RefreshTokenRequest refreshRequest = RefreshTokenRequest.builder()
                .refreshToken(refreshToken)
                .build();

        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andReturn();

        String refreshResponseBody = refreshResult.getResponse().getContentAsString();
        AuthResponse newAuthResponse = objectMapper.readValue(
                objectMapper.readTree(refreshResponseBody).get("data").toString(),
                AuthResponse.class
        );

        String newAccessToken = newAuthResponse.getAccessToken();
        assertThat(newAccessToken).isNotEqualTo(accessToken); // New token generated
        assertThat(jwtTokenProvider.validateToken(newAccessToken)).isTrue();

        // Step 4: Logout
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + newAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Logout successful"));

        // Verify session terminated
        sessions = sessionRepository.findByUserIdAndIsActiveTrue(createdUser.getId());
        assertThat(sessions).isEmpty(); // No active sessions
    }

    @Test
    void shouldFailLogin_WithInvalidCredentials() throws Exception {
        // Create user first
        RegisterRequest registerRequest = RegisterRequest.builder()
                .username("testuser")
                .email("testuser@example.com")
                .password("SecurePass123!")
                .firstName("Test")
                .lastName("User")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        // Attempt login with wrong password
        LoginRequest loginRequest = LoginRequest.builder()
                .email("testuser@example.com")
                .password("WrongPassword123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());

        // Verify no session created
        User user = userRepository.findByEmail("testuser@example.com").orElseThrow();
        List<Session> sessions = sessionRepository.findByUserIdAndIsActiveTrue(user.getId());
        assertThat(sessions).isEmpty();
    }

    @Test
    void shouldFailLogin_WithNonExistentUser() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .email("nonexistent@example.com")
                .password("SecurePass123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldFailRefresh_WithInvalidToken() throws Exception {
        RefreshTokenRequest refreshRequest = RefreshTokenRequest.builder()
                .refreshToken("invalid-refresh-token")
                .build();

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldCreateNewSession_OnEachLogin() throws Exception {
        // Register user
        RegisterRequest registerRequest = RegisterRequest.builder()
                .username("testuser")
                .email("testuser@example.com")
                .password("SecurePass123!")
                .firstName("Test")
                .lastName("User")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        LoginRequest loginRequest = LoginRequest.builder()
                .email("testuser@example.com")
                .password("SecurePass123!")
                .build();

        // First login from desktop
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest))
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0)"))
                .andExpect(status().isOk());

        // Second login from mobile
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest))
                        .header("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 14_0)"))
                .andExpect(status().isOk());

        // Verify 2 active sessions
        User user = userRepository.findByEmail("testuser@example.com").orElseThrow();
        List<Session> sessions = sessionRepository.findByUserIdAndIsActiveTrue(user.getId());
        assertThat(sessions).hasSize(2);
        assertThat(sessions).extracting(Session::getDeviceType).containsExactlyInAnyOrder("desktop", "mobile");
    }

    @Test
    void shouldTrackLoginHistory_OnSuccessfulLogin() throws Exception {
        // Register and login
        RegisterRequest registerRequest = RegisterRequest.builder()
                .username("testuser")
                .email("testuser@example.com")
                .password("SecurePass123!")
                .firstName("Test")
                .lastName("User")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        LoginRequest loginRequest = LoginRequest.builder()
                .email("testuser@example.com")
                .password("SecurePass123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest))
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0)")
                        .header("X-Forwarded-For", "203.0.113.42"))
                .andExpect(status().isOk());

        // Verify session has correct metadata
        User user = userRepository.findByEmail("testuser@example.com").orElseThrow();
        List<Session> sessions = sessionRepository.findByUserIdAndIsActiveTrue(user.getId());
        assertThat(sessions).hasSize(1);
        Session session = sessions.get(0);
        assertThat(session.getIpAddress()).isEqualTo("203.0.113.42");
        assertThat(session.getUserAgent()).contains("Windows NT 10.0");
        assertThat(session.getIsActive()).isTrue();
        assertThat(session.getExpiresAt()).isNotNull();
    }
}
