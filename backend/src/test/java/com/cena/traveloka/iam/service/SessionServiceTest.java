package com.cena.traveloka.iam.service;

import com.cena.traveloka.iam.dto.response.SessionDto;
import com.cena.traveloka.iam.entity.Session;
import com.cena.traveloka.iam.entity.User;
import com.cena.traveloka.iam.mapper.SessionMapper;
import com.cena.traveloka.iam.repository.IamSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * T012: SessionServiceTest
 * Service layer tests for session management operations.
 *
 * TDD Phase: RED - These tests MUST fail before implementing SessionService
 *
 * Constitutional Compliance:
 * - Principle VII: Test-First Development - Tests written before service implementation
 * - Tests FR-013: Session listing and management
 * - Tests FR-016: 5 concurrent session limit with oldest eviction
 * - Tests FR-004: Redis session storage with 24-hour TTL
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SessionService Tests")
class SessionServiceTest {

    @Mock
    private IamSessionRepository sessionRepository;

    @Mock
    private SessionMapper sessionMapper;

    @InjectMocks
    private SessionService sessionService;

    private User testUser;
    private Session testSession;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .username("johndoe")
                .email("john.doe@example.com")
                .build();

        testSession = Session.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .sessionToken("session_token_123")
                .refreshToken("refresh_token_123")
                .ipAddress("192.168.1.100")
                .userAgent("Mozilla/5.0 (Windows NT 10.0)")
                .deviceType("desktop")
                .deviceId("device_123")
                .browser("Chrome 120.0")
                .os("Windows 11")
                .isActive(true)
                .lastActivity(OffsetDateTime.now())
                .expiresAt(OffsetDateTime.now().plusHours(24))
                .createdAt(OffsetDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should create new session")
    void shouldCreateNewSession() {
        // Given
        String ipAddress = "192.168.1.100";
        String userAgent = "Mozilla/5.0";
        String sessionToken = "session_token";
        String refreshToken = "refresh_token";

        when(sessionRepository.save(any(Session.class))).thenReturn(testSession);

        // When
        Session result = sessionService.createSession(testUser, sessionToken, refreshToken, ipAddress, userAgent);

        // Then
        assertThat(result).isNotNull();
        verify(sessionRepository).save(argThat(session ->
                session.getUser().equals(testUser) &&
                session.getIsActive() &&
                session.getExpiresAt() != null
        ));
    }

    @Test
    @DisplayName("Should get user active sessions (FR-013)")
    void shouldGetUserActiveSessions() {
        // Given
        UUID userId = testUser.getId();
        List<Session> sessions = Arrays.asList(testSession);
        SessionDto sessionDto = SessionDto.builder().id(testSession.getId()).build();

        when(sessionRepository.findByUserIdAndIsActiveTrue(userId)).thenReturn(sessions);
        when(sessionMapper.toDto(any(Session.class))).thenReturn(sessionDto);

        // When
        List<SessionDto> result = sessionService.getUserActiveSessions(userId);

        // Then
        assertThat(result).isNotEmpty();
        assertThat(result).hasSize(1);
        verify(sessionRepository).findByUserIdAndIsActiveTrue(userId);
    }

    @Test
    @DisplayName("Should count active sessions")
    void shouldCountActiveSessions() {
        // Given
        UUID userId = testUser.getId();
        when(sessionRepository.countByUserIdAndIsActiveTrue(userId)).thenReturn(3L);

        // When
        long count = sessionService.countActiveSessions(userId);

        // Then
        assertThat(count).isEqualTo(3);
        verify(sessionRepository).countByUserIdAndIsActiveTrue(userId);
    }

    @Test
    @DisplayName("Should enforce 5 session limit (FR-016)")
    void shouldEnforceFiveSessionLimit() {
        // Given
        UUID userId = testUser.getId();
        when(sessionRepository.countByUserIdAndIsActiveTrue(userId)).thenReturn(5L);

        Session oldestSession = Session.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .sessionToken("old_token")
                .isActive(true)
                .createdAt(OffsetDateTime.now().minusHours(5))
                .build();

        when(sessionRepository.findFirstByUserIdAndIsActiveTrueOrderByCreatedAtAsc(userId))
                .thenReturn(Optional.of(oldestSession));
        when(sessionRepository.save(any(Session.class))).thenReturn(oldestSession);

        // When
        sessionService.enforceSessionLimit(userId);

        // Then
        verify(sessionRepository).save(argThat(session ->
                !session.getIsActive() &&
                session.getTerminatedAt() != null &&
                session.getTerminationReason().contains("Session limit exceeded")
        ));
    }

    @Test
    @DisplayName("Should terminate oldest session when limit exceeded (FR-016)")
    void shouldTerminateOldestSessionWhenLimitExceeded() {
        // Given
        UUID userId = testUser.getId();
        Session oldestSession = Session.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .sessionToken("oldest_token")
                .isActive(true)
                .createdAt(OffsetDateTime.now().minusHours(10))
                .build();

        Session newestSession = Session.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .sessionToken("newest_token")
                .isActive(true)
                .createdAt(OffsetDateTime.now())
                .build();

        when(sessionRepository.countByUserIdAndIsActiveTrue(userId)).thenReturn(6L);
        when(sessionRepository.findFirstByUserIdAndIsActiveTrueOrderByCreatedAtAsc(userId))
                .thenReturn(Optional.of(oldestSession));
        when(sessionRepository.save(any(Session.class))).thenReturn(oldestSession);

        // When
        sessionService.enforceSessionLimit(userId);

        // Then
        verify(sessionRepository).save(argThat(session ->
                session.getId().equals(oldestSession.getId()) &&
                !session.getIsActive()
        ));
    }

    @Test
    @DisplayName("Should terminate session by ID")
    void shouldTerminateSessionById() {
        // Given
        UUID sessionId = testSession.getId();
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(testSession));
        when(sessionRepository.save(any(Session.class))).thenReturn(testSession);

        // When
        sessionService.terminateSession(sessionId, "User logout");

        // Then
        verify(sessionRepository).save(argThat(session ->
                !session.getIsActive() &&
                session.getTerminatedAt() != null &&
                session.getTerminationReason().equals("User logout")
        ));
    }

    @Test
    @DisplayName("Should terminate session by token")
    void shouldTerminateSessionByToken() {
        // Given
        String sessionToken = testSession.getSessionToken();
        when(sessionRepository.findBySessionToken(sessionToken)).thenReturn(Optional.of(testSession));
        when(sessionRepository.save(any(Session.class))).thenReturn(testSession);

        // When
        sessionService.terminateSessionByToken(sessionToken);

        // Then
        verify(sessionRepository).save(argThat(session ->
                !session.getIsActive()
        ));
    }

    @Test
    @DisplayName("Should terminate all user sessions")
    void shouldTerminateAllUserSessions() {
        // Given
        UUID userId = testUser.getId();
        List<Session> sessions = Arrays.asList(testSession);

        when(sessionRepository.findByUserIdAndIsActiveTrue(userId)).thenReturn(sessions);
        when(sessionRepository.save(any(Session.class))).thenReturn(testSession);

        // When
        sessionService.terminateAllUserSessions(userId);

        // Then
        verify(sessionRepository, times(1)).save(argThat(session ->
                !session.getIsActive() &&
                session.getTerminationReason().contains("All sessions terminated")
        ));
    }

    @Test
    @DisplayName("Should update session last activity")
    void shouldUpdateSessionLastActivity() {
        // Given
        String sessionToken = testSession.getSessionToken();
        OffsetDateTime beforeUpdate = testSession.getLastActivity();

        when(sessionRepository.findBySessionToken(sessionToken)).thenReturn(Optional.of(testSession));
        when(sessionRepository.save(any(Session.class))).thenReturn(testSession);

        // When
        sessionService.updateLastActivity(sessionToken);

        // Then
        verify(sessionRepository).save(argThat(session ->
                session.getLastActivity().isAfter(beforeUpdate)
        ));
    }

    @Test
    @DisplayName("Should find session by token")
    void shouldFindSessionByToken() {
        // Given
        String sessionToken = testSession.getSessionToken();
        when(sessionRepository.findBySessionToken(sessionToken)).thenReturn(Optional.of(testSession));

        // When
        Optional<Session> result = sessionService.findBySessionToken(sessionToken);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getSessionToken()).isEqualTo(sessionToken);
    }

    @Test
    @DisplayName("Should validate active session")
    void shouldValidateActiveSession() {
        // Given
        String sessionToken = testSession.getSessionToken();
        testSession.setIsActive(true);
        testSession.setExpiresAt(OffsetDateTime.now().plusHours(1));

        when(sessionRepository.findBySessionToken(sessionToken)).thenReturn(Optional.of(testSession));

        // When
        boolean isValid = sessionService.isSessionValid(sessionToken);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should invalidate expired session")
    void shouldInvalidateExpiredSession() {
        // Given
        String sessionToken = testSession.getSessionToken();
        testSession.setIsActive(true);
        testSession.setExpiresAt(OffsetDateTime.now().minusHours(1)); // Expired

        when(sessionRepository.findBySessionToken(sessionToken)).thenReturn(Optional.of(testSession));

        // When
        boolean isValid = sessionService.isSessionValid(sessionToken);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should invalidate inactive session")
    void shouldInvalidateInactiveSession() {
        // Given
        String sessionToken = testSession.getSessionToken();
        testSession.setIsActive(false);

        when(sessionRepository.findBySessionToken(sessionToken)).thenReturn(Optional.of(testSession));

        // When
        boolean isValid = sessionService.isSessionValid(sessionToken);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should clean up expired sessions")
    void shouldCleanUpExpiredSessions() {
        // Given
        Session expiredSession = Session.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .sessionToken("expired_token")
                .isActive(true)
                .expiresAt(OffsetDateTime.now().minusHours(1))
                .build();

        List<Session> expiredSessions = Arrays.asList(expiredSession);
        when(sessionRepository.findByIsActiveTrueAndExpiresAtBefore(any(OffsetDateTime.class)))
                .thenReturn(expiredSessions);
        when(sessionRepository.save(any(Session.class))).thenReturn(expiredSession);

        // When
        int cleaned = sessionService.cleanupExpiredSessions();

        // Then
        assertThat(cleaned).isEqualTo(1);
        verify(sessionRepository).save(argThat(session ->
                !session.getIsActive() &&
                session.getTerminationReason().contains("Expired")
        ));
    }

    @Test
    @DisplayName("Should detect session hijacking by IP change")
    void shouldDetectSessionHijackingByIpChange() {
        // Given
        String sessionToken = testSession.getSessionToken();
        String originalIp = "192.168.1.100";
        String newIp = "10.0.0.1";

        testSession.setIpAddress(originalIp);
        when(sessionRepository.findBySessionToken(sessionToken)).thenReturn(Optional.of(testSession));

        // When
        boolean isSuspicious = sessionService.detectSuspiciousActivity(sessionToken, newIp, testSession.getUserAgent());

        // Then
        assertThat(isSuspicious).isTrue();
    }

    @Test
    @DisplayName("Should detect session hijacking by user agent change")
    void shouldDetectSessionHijackingByUserAgentChange() {
        // Given
        String sessionToken = testSession.getSessionToken();
        String originalUserAgent = "Mozilla/5.0 (Windows NT 10.0)";
        String newUserAgent = "Mozilla/5.0 (Linux)";

        testSession.setUserAgent(originalUserAgent);
        when(sessionRepository.findBySessionToken(sessionToken)).thenReturn(Optional.of(testSession));

        // When
        boolean isSuspicious = sessionService.detectSuspiciousActivity(sessionToken, testSession.getIpAddress(), newUserAgent);

        // Then
        assertThat(isSuspicious).isTrue();
    }

    @Test
    @DisplayName("Should mark session as suspicious")
    void shouldMarkSessionAsSuspicious() {
        // Given
        UUID sessionId = testSession.getId();
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(testSession));
        when(sessionRepository.save(any(Session.class))).thenReturn(testSession);

        // When
        sessionService.markSessionSuspicious(sessionId, 75);

        // Then
        verify(sessionRepository).save(argThat(session ->
                session.getIsSuspicious() &&
                session.getRiskScore() == 75
        ));
    }

    @Test
    @DisplayName("Should throw exception when session not found")
    void shouldThrowExceptionWhenSessionNotFound() {
        // Given
        UUID sessionId = UUID.randomUUID();
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> sessionService.terminateSession(sessionId, "test"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Session not found");
    }
}
