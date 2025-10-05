package com.cena.traveloka.iam.integration;

import com.cena.traveloka.iam.dto.request.LoginRequest;
import com.cena.traveloka.iam.dto.request.RegisterRequest;
import com.cena.traveloka.iam.entity.Session;
import com.cena.traveloka.iam.entity.User;
import com.cena.traveloka.iam.repository.IamSessionRepository;
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

import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T098: Concurrent session limit test.
 *
 * Tests session limit enforcement including:
 * - Maximum 5 concurrent sessions per user (FR-016)
 * - Oldest session eviction when limit exceeded
 * - Session tracking across multiple devices
 * - Session metadata preservation
 * - Session ordering by creation time
 * - Multiple user session isolation
 *
 * Uses TestContainers for PostgreSQL and Redis.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@Transactional
public class SessionLimitIntegrationTest {

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

    private static final int MAX_SESSIONS = 5;

    @BeforeEach
    void setUp() {
        sessionRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldAllowUpTo5ConcurrentSessions() throws Exception {
        registerUser("test@example.com");

        LoginRequest loginRequest = LoginRequest.builder()
                .email("test@example.com")
                .password("SecurePass123!")
                .build();

        // Create 5 sessions from different devices
        String[] userAgents = {
                "Mozilla/5.0 (Windows NT 10.0)", // Desktop
                "Mozilla/5.0 (iPhone; CPU iPhone OS 14_0)", // Mobile 1
                "Mozilla/5.0 (iPad; CPU OS 14_0)", // Tablet
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)", // Desktop 2
                "Mozilla/5.0 (Linux; Android 11)" // Mobile 2
        };

        for (String userAgent : userAgents) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest))
                            .header("User-Agent", userAgent))
                    .andExpect(status().isOk());
        }

        // Verify exactly 5 active sessions
        User user = userRepository.findByEmail("test@example.com").orElseThrow();
        List<Session> sessions = sessionRepository.findByUserIdAndIsActiveTrue(user.getId());
        assertThat(sessions).hasSize(MAX_SESSIONS);
    }

    @Test
    void shouldEvictOldestSession_When6thSessionCreated() throws Exception {
        registerUser("test@example.com");

        LoginRequest loginRequest = LoginRequest.builder()
                .email("test@example.com")
                .password("SecurePass123!")
                .build();

        // Create 5 sessions
        for (int i = 1; i <= 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest))
                            .header("User-Agent", "Device-" + i))
                    .andExpect(status().isOk());

            // Small delay to ensure different timestamps
            Thread.sleep(10);
        }

        User user = userRepository.findByEmail("test@example.com").orElseThrow();
        List<Session> sessionsBeforeSixth = sessionRepository.findByUserIdAndIsActiveTrue(user.getId());
        assertThat(sessionsBeforeSixth).hasSize(5);

        // Get oldest session ID
        Session oldestSession = sessionsBeforeSixth.stream()
                .min(Comparator.comparing(Session::getCreatedAt))
                .orElseThrow();

        // Create 6th session
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest))
                        .header("User-Agent", "Device-6"))
                .andExpect(status().isOk());

        // Verify still only 5 active sessions
        List<Session> sessionsAfterSixth = sessionRepository.findByUserIdAndIsActiveTrue(user.getId());
        assertThat(sessionsAfterSixth).hasSize(MAX_SESSIONS);

        // Verify oldest session was terminated
        Session terminatedSession = sessionRepository.findById(oldestSession.getId()).orElseThrow();
        assertThat(terminatedSession.getIsActive()).isFalse();
        assertThat(terminatedSession.getTerminatedAt()).isNotNull();
        assertThat(terminatedSession.getTerminationReason())
                .contains("Session limit exceeded")
                .contains("max 5 concurrent sessions");
    }

    @Test
    void shouldMaintainSessionOrder_ByCreationTime() throws Exception {
        registerUser("test@example.com");

        LoginRequest loginRequest = LoginRequest.builder()
                .email("test@example.com")
                .password("SecurePass123!")
                .build();

        // Create 5 sessions with delays
        for (int i = 1; i <= 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest))
                    .header("User-Agent", "Device-" + i));
            Thread.sleep(50); // Ensure different timestamps
        }

        User user = userRepository.findByEmail("test@example.com").orElseThrow();
        List<Session> sessions = sessionRepository.findByUserIdAndIsActiveTrue(user.getId());

        // Verify sessions can be ordered by creation time
        List<Session> orderedSessions = sessions.stream()
                .sorted(Comparator.comparing(Session::getCreatedAt))
                .toList();

        for (int i = 0; i < orderedSessions.size() - 1; i++) {
            assertThat(orderedSessions.get(i).getCreatedAt())
                    .isBefore(orderedSessions.get(i + 1).getCreatedAt());
        }
    }

    @Test
    void shouldIsolateSessions_BetweenDifferentUsers() throws Exception {
        // Create two users
        registerUser("user1@example.com");
        registerUser("user2@example.com");

        LoginRequest user1Login = LoginRequest.builder()
                .email("user1@example.com")
                .password("SecurePass123!")
                .build();

        LoginRequest user2Login = LoginRequest.builder()
                .email("user2@example.com")
                .password("SecurePass123!")
                .build();

        // Create 5 sessions for user1
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(user1Login)));
        }

        // Create 5 sessions for user2
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(user2Login)));
        }

        // Verify each user has exactly 5 sessions
        User user1 = userRepository.findByEmail("user1@example.com").orElseThrow();
        User user2 = userRepository.findByEmail("user2@example.com").orElseThrow();

        List<Session> user1Sessions = sessionRepository.findByUserIdAndIsActiveTrue(user1.getId());
        List<Session> user2Sessions = sessionRepository.findByUserIdAndIsActiveTrue(user2.getId());

        assertThat(user1Sessions).hasSize(MAX_SESSIONS);
        assertThat(user2Sessions).hasSize(MAX_SESSIONS);

        // Verify no session overlap
        List<String> user1SessionIds = user1Sessions.stream()
                .map(s -> s.getId().toString())
                .toList();
        List<String> user2SessionIds = user2Sessions.stream()
                .map(s -> s.getId().toString())
                .toList();

        assertThat(user1SessionIds).doesNotContainAnyElementsOf(user2SessionIds);
    }

    @Test
    void shouldPreserveSessionMetadata_AfterEviction() throws Exception {
        registerUser("test@example.com");

        LoginRequest loginRequest = LoginRequest.builder()
                .email("test@example.com")
                .password("SecurePass123!")
                .build();

        // Create 6 sessions to trigger eviction
        for (int i = 1; i <= 6; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest))
                            .header("User-Agent", "Mozilla/5.0 (Device-" + i + ")")
                            .header("X-Forwarded-For", "192.168.1." + i))
                    .andExpect(status().isOk());
            Thread.sleep(10);
        }

        User user = userRepository.findByEmail("test@example.com").orElseThrow();

        // Get evicted session (oldest, now inactive) - query all sessions and filter
        List<Session> allSessions = sessionRepository.findAll();
        Session evictedSession = allSessions.stream()
                .filter(s -> s.getUser().getId().equals(user.getId()) && !s.getIsActive())
                .findFirst()
                .orElseThrow();

        // Verify evicted session metadata is preserved
        assertThat(evictedSession.getIsActive()).isFalse();
        assertThat(evictedSession.getTerminatedAt()).isNotNull();
        assertThat(evictedSession.getTerminationReason()).isNotNull();
        assertThat(evictedSession.getIpAddress()).isNotNull();
        assertThat(evictedSession.getUserAgent()).isNotNull();
        assertThat(evictedSession.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldTrackDeviceTypes_AcrossSessions() throws Exception {
        registerUser("test@example.com");

        LoginRequest loginRequest = LoginRequest.builder()
                .email("test@example.com")
                .password("SecurePass123!")
                .build();

        // Create sessions from different device types
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0)")); // Desktop

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
                .header("User-Agent", "Mozilla/5.0 (iPhone)")); // Mobile

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
                .header("User-Agent", "Mozilla/5.0 (iPad)")); // Tablet

        User user = userRepository.findByEmail("test@example.com").orElseThrow();
        List<Session> sessions = sessionRepository.findByUserIdAndIsActiveTrue(user.getId());

        assertThat(sessions).hasSize(3);
        assertThat(sessions).extracting(Session::getDeviceType)
                .containsExactlyInAnyOrder("desktop", "mobile", "tablet");
    }

    @Test
    void shouldEnforceLimit_ConsistentlyAcrossMultipleLogins() throws Exception {
        registerUser("test@example.com");

        LoginRequest loginRequest = LoginRequest.builder()
                .email("test@example.com")
                .password("SecurePass123!")
                .build();

        // Create 10 sessions (should evict 5)
        for (int i = 1; i <= 10; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest))
                            .header("User-Agent", "Device-" + i))
                    .andExpect(status().isOk());
            Thread.sleep(10);
        }

        User user = userRepository.findByEmail("test@example.com").orElseThrow();

        // Verify exactly 5 active sessions
        List<Session> activeSessions = sessionRepository.findByUserIdAndIsActiveTrue(user.getId());
        assertThat(activeSessions).hasSize(MAX_SESSIONS);

        // Verify 5 sessions were evicted - count inactive sessions manually
        List<Session> allSessions = sessionRepository.findAll();
        long inactiveCount = allSessions.stream()
                .filter(s -> s.getUser().getId().equals(user.getId()) && !s.getIsActive())
                .count();
        assertThat(inactiveCount).isEqualTo(5);
    }

    @Test
    void shouldCountActiveSessions_Correctly() throws Exception {
        registerUser("test@example.com");

        LoginRequest loginRequest = LoginRequest.builder()
                .email("test@example.com")
                .password("SecurePass123!")
                .build();

        User user = userRepository.findByEmail("test@example.com").orElseThrow();

        // Initially 0 sessions
        long count0 = sessionRepository.countByUserIdAndIsActiveTrue(user.getId());
        assertThat(count0).isZero();

        // Create 3 sessions
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)));
        }

        long count3 = sessionRepository.countByUserIdAndIsActiveTrue(user.getId());
        assertThat(count3).isEqualTo(3);

        // Create 2 more sessions (total 5)
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)));
        }

        long count5 = sessionRepository.countByUserIdAndIsActiveTrue(user.getId());
        assertThat(count5).isEqualTo(5);

        // Create 1 more session (should still be 5, oldest evicted)
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)));

        long countAfter6 = sessionRepository.countByUserIdAndIsActiveTrue(user.getId());
        assertThat(countAfter6).isEqualTo(5);
    }

    @Test
    void shouldHandleRapidSuccessiveLogins() throws Exception {
        registerUser("test@example.com");

        LoginRequest loginRequest = LoginRequest.builder()
                .email("test@example.com")
                .password("SecurePass123!")
                .build();

        // Create 7 sessions rapidly (no delay)
        for (int i = 1; i <= 7; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest))
                            .header("User-Agent", "RapidDevice-" + i))
                    .andExpect(status().isOk());
        }

        User user = userRepository.findByEmail("test@example.com").orElseThrow();
        List<Session> activeSessions = sessionRepository.findByUserIdAndIsActiveTrue(user.getId());

        // Should still enforce 5 session limit
        assertThat(activeSessions).hasSize(MAX_SESSIONS);
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
