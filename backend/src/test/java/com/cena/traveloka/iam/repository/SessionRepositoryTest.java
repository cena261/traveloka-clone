package com.cena.traveloka.iam.repository;

import com.cena.traveloka.common.enums.Status;
import com.cena.traveloka.iam.entity.Session;
import com.cena.traveloka.iam.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T006: SessionRepository test with eviction queries
 * Tests session management including concurrent session limits (FR-016).
 *
 * TDD Phase: RED - These tests MUST fail before implementing SessionRepository
 *
 * Constitutional Compliance:
 * - Principle IV: Entity Immutability - Session entity is READ-ONLY, not modified here
 * - Principle VII: Test-First Development - Tests written before repository implementation
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@ActiveProfiles("test")
@DisplayName("SessionRepository Tests")
class SessionRepositoryTest {

    @Autowired
    private IamSessionRepository sessionRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User testUser;
    private Session activeSession;

    @BeforeEach
    void setUp() {
        // Clean up before each test
        sessionRepository.deleteAll();

        // Create test user
        testUser = User.builder()
                .keycloakId(UUID.randomUUID())
                .username("johndoe")
                .email("john.doe@example.com")
                .firstName("John")
                .lastName("Doe")
                .status(Status.active)
                .emailVerified(true)
                .createdAt(OffsetDateTime.now())
                .createdBy("system")
                .isDeleted(false)
                .build();
        testUser = entityManager.persistAndFlush(testUser);

        // Create test session
        activeSession = Session.builder()
                .user(testUser)
                .sessionToken("session_token_" + UUID.randomUUID())
                .refreshToken("refresh_token_" + UUID.randomUUID())
                .ipAddress("192.168.1.100")
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .deviceType("desktop")
                .deviceId("device_123")
                .browser("Chrome 120.0")
                .os("Windows 11")
                .locationCountry("Vietnam")
                .locationCity("Ho Chi Minh City")
                .isActive(true)
                .lastActivity(OffsetDateTime.now())
                .expiresAt(OffsetDateTime.now().plusHours(24))
                .refreshExpiresAt(OffsetDateTime.now().plusDays(7))
                .isSuspicious(false)
                .riskScore(0)
                .requires2fa(false)
                .twoFaCompleted(false)
                .createdAt(OffsetDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should save session successfully")
    void shouldSaveSession() {
        // When
        Session savedSession = sessionRepository.save(activeSession);

        // Then
        assertThat(savedSession).isNotNull();
        assertThat(savedSession.getId()).isNotNull();
        assertThat(savedSession.getUser().getId()).isEqualTo(testUser.getId());
        assertThat(savedSession.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("Should find session by token")
    void shouldFindSessionByToken() {
        // Given
        Session savedSession = sessionRepository.save(activeSession);

        // When
        Optional<Session> foundSession = sessionRepository.findBySessionToken(savedSession.getSessionToken());

        // Then
        assertThat(foundSession).isPresent();
        assertThat(foundSession.get().getUser().getId()).isEqualTo(testUser.getId());
    }

    @Test
    @DisplayName("Should find active sessions by user ID")
    void shouldFindActiveSessionsByUserId() {
        // Given
        sessionRepository.save(activeSession);

        // Create another active session
        Session anotherSession = Session.builder()
                .user(testUser)
                .sessionToken("session_token_2_" + UUID.randomUUID())
                .isActive(true)
                .expiresAt(OffsetDateTime.now().plusHours(24))
                .createdAt(OffsetDateTime.now())
                .build();
        sessionRepository.save(anotherSession);

        // Create inactive session
        Session inactiveSession = Session.builder()
                .user(testUser)
                .sessionToken("session_token_3_" + UUID.randomUUID())
                .isActive(false)
                .expiresAt(OffsetDateTime.now().plusHours(24))
                .terminatedAt(OffsetDateTime.now())
                .terminationReason("User logout")
                .createdAt(OffsetDateTime.now())
                .build();
        sessionRepository.save(inactiveSession);

        // When
        List<Session> activeSessions = sessionRepository.findByUserIdAndIsActiveTrue(testUser.getId());

        // Then
        assertThat(activeSessions).hasSize(2);
        assertThat(activeSessions).allMatch(Session::getIsActive);
    }

    @Test
    @DisplayName("Should count active sessions for user (FR-016: 5 session limit)")
    void shouldCountActiveSessionsForUser() {
        // Given
        sessionRepository.save(activeSession);

        Session session2 = Session.builder()
                .user(testUser)
                .sessionToken("token_2")
                .isActive(true)
                .createdAt(OffsetDateTime.now())
                .build();
        sessionRepository.save(session2);

        // When
        long activeCount = sessionRepository.countByUserIdAndIsActiveTrue(testUser.getId());

        // Then
        assertThat(activeCount).isEqualTo(2);
    }

    @Test
    @DisplayName("Should find oldest active session for eviction (FR-016)")
    void shouldFindOldestActiveSession() {
        // Given - Create sessions with different creation times
        Session oldestSession = Session.builder()
                .user(testUser)
                .sessionToken("token_oldest")
                .isActive(true)
                .createdAt(OffsetDateTime.now().minusHours(5))
                .expiresAt(OffsetDateTime.now().plusHours(19))
                .build();
        sessionRepository.save(oldestSession);

        Session middleSession = Session.builder()
                .user(testUser)
                .sessionToken("token_middle")
                .isActive(true)
                .createdAt(OffsetDateTime.now().minusHours(3))
                .expiresAt(OffsetDateTime.now().plusHours(21))
                .build();
        sessionRepository.save(middleSession);

        Session newestSession = Session.builder()
                .user(testUser)
                .sessionToken("token_newest")
                .isActive(true)
                .createdAt(OffsetDateTime.now())
                .expiresAt(OffsetDateTime.now().plusHours(24))
                .build();
        sessionRepository.save(newestSession);

        // When
        Optional<Session> oldestFound = sessionRepository.findFirstByUserIdAndIsActiveTrueOrderByCreatedAtAsc(testUser.getId());

        // Then
        assertThat(oldestFound).isPresent();
        assertThat(oldestFound.get().getSessionToken()).isEqualTo("token_oldest");
    }

    @Test
    @DisplayName("Should find expired sessions for cleanup")
    void shouldFindExpiredSessions() {
        // Given
        sessionRepository.save(activeSession);

        // Create expired session
        Session expiredSession = Session.builder()
                .user(testUser)
                .sessionToken("expired_token")
                .isActive(true)
                .expiresAt(OffsetDateTime.now().minusHours(1))
                .createdAt(OffsetDateTime.now().minusHours(25))
                .build();
        sessionRepository.save(expiredSession);

        // When
        List<Session> expiredSessions = sessionRepository.findByIsActiveTrueAndExpiresAtBefore(OffsetDateTime.now());

        // Then
        assertThat(expiredSessions).hasSize(1);
        assertThat(expiredSessions.get(0).getSessionToken()).isEqualTo("expired_token");
    }

    @Test
    @DisplayName("Should find suspicious sessions")
    void shouldFindSuspiciousSessions() {
        // Given
        activeSession.setIsSuspicious(true);
        activeSession.setRiskScore(75);
        sessionRepository.save(activeSession);

        // When
        List<Session> suspiciousSessions = sessionRepository.findByIsSuspiciousTrue();

        // Then
        assertThat(suspiciousSessions).hasSize(1);
        assertThat(suspiciousSessions.get(0).getRiskScore()).isEqualTo(75);
    }

    @Test
    @DisplayName("Should find sessions requiring 2FA")
    void shouldFindSessionsRequiring2FA() {
        // Given
        activeSession.setRequires2fa(true);
        activeSession.setTwoFaCompleted(false);
        sessionRepository.save(activeSession);

        // When
        List<Session> sessions2FARequired = sessionRepository.findByRequires2faTrueAndTwoFaCompletedFalse();

        // Then
        assertThat(sessions2FARequired).hasSize(1);
        assertThat(sessions2FARequired.get(0).getRequires2fa()).isTrue();
        assertThat(sessions2FARequired.get(0).getTwoFaCompleted()).isFalse();
    }

    @Test
    @DisplayName("Should update session last activity")
    void shouldUpdateSessionLastActivity() {
        // Given
        Session savedSession = sessionRepository.save(activeSession);
        OffsetDateTime originalLastActivity = savedSession.getLastActivity();

        // When
        savedSession.setLastActivity(OffsetDateTime.now().plusMinutes(5));
        savedSession.setUpdatedAt(OffsetDateTime.now());
        Session updatedSession = sessionRepository.save(savedSession);

        // Then
        assertThat(updatedSession.getLastActivity()).isAfter(originalLastActivity);
    }

    @Test
    @DisplayName("Should terminate session")
    void shouldTerminateSession() {
        // Given
        Session savedSession = sessionRepository.save(activeSession);

        // When
        savedSession.setIsActive(false);
        savedSession.setTerminatedAt(OffsetDateTime.now());
        savedSession.setTerminationReason("User logout");
        Session terminatedSession = sessionRepository.save(savedSession);

        // Then
        assertThat(terminatedSession.getIsActive()).isFalse();
        assertThat(terminatedSession.getTerminatedAt()).isNotNull();
        assertThat(terminatedSession.getTerminationReason()).isEqualTo("User logout");
    }

    @Test
    @DisplayName("Should find sessions by IP address (session hijacking detection)")
    void shouldFindSessionsByIpAddress() {
        // Given
        sessionRepository.save(activeSession);

        Session differentIpSession = Session.builder()
                .user(testUser)
                .sessionToken("different_ip_token")
                .ipAddress("10.0.0.1")
                .isActive(true)
                .createdAt(OffsetDateTime.now())
                .build();
        sessionRepository.save(differentIpSession);

        // When
        List<Session> sameIpSessions = sessionRepository.findByIpAddress("192.168.1.100");

        // Then
        assertThat(sameIpSessions).hasSize(1);
        assertThat(sameIpSessions.get(0).getSessionToken()).isEqualTo(activeSession.getSessionToken());
    }

    @Test
    @DisplayName("Should delete sessions for user")
    void shouldDeleteSessionsForUser() {
        // Given
        sessionRepository.save(activeSession);

        Session anotherSession = Session.builder()
                .user(testUser)
                .sessionToken("token_2")
                .isActive(true)
                .createdAt(OffsetDateTime.now())
                .build();
        sessionRepository.save(anotherSession);

        // When
        sessionRepository.deleteByUserId(testUser.getId());
        entityManager.flush();
        entityManager.clear();

        // Then
        List<Session> remainingSessions = sessionRepository.findByUserIdAndIsActiveTrue(testUser.getId());
        assertThat(remainingSessions).isEmpty();
    }

    @Test
    @DisplayName("Should find sessions by device ID")
    void shouldFindSessionsByDeviceId() {
        // Given
        sessionRepository.save(activeSession);

        // When
        List<Session> deviceSessions = sessionRepository.findByDeviceId("device_123");

        // Then
        assertThat(deviceSessions).hasSize(1);
        assertThat(deviceSessions.get(0).getBrowser()).isEqualTo("Chrome 120.0");
    }
}
