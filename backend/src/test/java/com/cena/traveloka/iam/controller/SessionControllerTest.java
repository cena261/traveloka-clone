package com.cena.traveloka.iam.controller;

import com.cena.traveloka.iam.dto.response.SessionDto;
import com.cena.traveloka.iam.service.SessionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T017: Test SessionController
 * Controller test for SessionController (TDD - Phase 3.2).
 *
 * Tests all session management endpoints:
 * - GET /api/v1/sessions/active
 * - DELETE /api/v1/sessions/{id}
 * - DELETE /api/v1/sessions/all-except-current
 *
 * Constitutional Compliance:
 * - Principle VII: Test Coverage - TDD mandatory, tests before implementation
 * - Principle IV: Entity Immutability - Uses DTOs for API contracts
 * - FR-013: Session management and multi-device tracking
 * - FR-016: Maximum 5 concurrent sessions per user
 * - NFR-003: 24-hour session TTL
 */
@WebMvcTest(SessionController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security for unit tests
@DisplayName("SessionController Tests")
class SessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SessionService sessionService;

    private SessionDto currentSessionDto;
    private SessionDto otherSessionDto;
    private List<SessionDto> activeSessions;

    @BeforeEach
    void setUp() {
        UUID userId = UUID.randomUUID();
        UUID currentSessionId = UUID.randomUUID();
        UUID otherSessionId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        currentSessionDto = SessionDto.builder()
                .id(currentSessionId)
                .userId(userId)
                .deviceType("desktop")
                .deviceId("device-123")
                .browser("Chrome")
                .os("Windows 10")
                .ipAddress("192.168.1.1")
                .locationCountry("VN")
                .locationCity("Ho Chi Minh")
                .isActive(true)
                .isCurrent(true)
                .lastActivity(now)
                .expiresAt(now.plusHours(24))
                .createdAt(now.minusHours(2))
                .isSuspicious(false)
                .riskScore(0)
                .requires2fa(false)
                .twoFaCompleted(false)
                .build();

        otherSessionDto = SessionDto.builder()
                .id(otherSessionId)
                .userId(userId)
                .deviceType("mobile")
                .deviceId("device-456")
                .browser("Safari")
                .os("iOS 17")
                .ipAddress("192.168.1.2")
                .locationCountry("VN")
                .locationCity("Hanoi")
                .isActive(true)
                .isCurrent(false)
                .lastActivity(now.minusHours(1))
                .expiresAt(now.plusHours(23))
                .createdAt(now.minusHours(3))
                .isSuspicious(false)
                .riskScore(0)
                .requires2fa(false)
                .twoFaCompleted(false)
                .build();

        activeSessions = List.of(currentSessionDto, otherSessionDto);
    }

    @Nested
    @DisplayName("GET /api/v1/sessions/active - Get Active Sessions (FR-013)")
    class GetActiveSessionsTests {

        @Test
        @DisplayName("Should get all active sessions successfully")
        void shouldGetActiveSessions_Success() throws Exception {
            // Given
            when(sessionService.getActiveSessions(anyString()))
                    .thenReturn(activeSessions);

            // When & Then
            mockMvc.perform(get("/api/v1/sessions/active")
                            .header("Authorization", "Bearer jwt-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].isCurrent").value(true))
                    .andExpect(jsonPath("$.data[0].deviceType").value("desktop"))
                    .andExpect(jsonPath("$.data[0].browser").value("Chrome"))
                    .andExpect(jsonPath("$.data[0].os").value("Windows 10"))
                    .andExpect(jsonPath("$.data[1].isCurrent").value(false))
                    .andExpect(jsonPath("$.data[1].deviceType").value("mobile"))
                    .andExpect(jsonPath("$.data[1].browser").value("Safari"));

            verify(sessionService).getActiveSessions(anyString());
        }

        @Test
        @DisplayName("Should return empty list when no active sessions")
        void shouldGetActiveSessions_WhenNoSessions() throws Exception {
            // Given
            when(sessionService.getActiveSessions(anyString()))
                    .thenReturn(List.of());

            // When & Then
            mockMvc.perform(get("/api/v1/sessions/active")
                            .header("Authorization", "Bearer jwt-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(0));

            verify(sessionService).getActiveSessions(anyString());
        }

        @Test
        @DisplayName("Should include session metadata (location, device, timestamps)")
        void shouldGetActiveSessions_WithMetadata() throws Exception {
            // Given
            when(sessionService.getActiveSessions(anyString()))
                    .thenReturn(activeSessions);

            // When & Then
            mockMvc.perform(get("/api/v1/sessions/active")
                            .header("Authorization", "Bearer jwt-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].locationCountry").value("VN"))
                    .andExpect(jsonPath("$.data[0].locationCity").value("Ho Chi Minh"))
                    .andExpect(jsonPath("$.data[0].ipAddress").value("192.168.1.1"))
                    .andExpect(jsonPath("$.data[0].lastActivity").exists())
                    .andExpect(jsonPath("$.data[0].expiresAt").exists())
                    .andExpect(jsonPath("$.data[0].createdAt").exists());

            verify(sessionService).getActiveSessions(anyString());
        }

        @Test
        @DisplayName("Should include security flags (suspicious, risk score, 2FA)")
        void shouldGetActiveSessions_WithSecurityFlags() throws Exception {
            // Given
            when(sessionService.getActiveSessions(anyString()))
                    .thenReturn(activeSessions);

            // When & Then
            mockMvc.perform(get("/api/v1/sessions/active")
                            .header("Authorization", "Bearer jwt-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].isSuspicious").value(false))
                    .andExpect(jsonPath("$.data[0].riskScore").value(0))
                    .andExpect(jsonPath("$.data[0].requires2fa").value(false))
                    .andExpect(jsonPath("$.data[0].twoFaCompleted").value(false));

            verify(sessionService).getActiveSessions(anyString());
        }

        @Test
        @DisplayName("Should respect 5 concurrent session limit (FR-016)")
        void shouldGetActiveSessions_WithMaxFiveSessions() throws Exception {
            // Given - Simulate 5 sessions (max allowed)
            List<SessionDto> fiveSessions = List.of(
                    createSessionDto("desktop", true),
                    createSessionDto("mobile", false),
                    createSessionDto("tablet", false),
                    createSessionDto("mobile", false),
                    createSessionDto("desktop", false)
            );

            when(sessionService.getActiveSessions(anyString()))
                    .thenReturn(fiveSessions);

            // When & Then
            mockMvc.perform(get("/api/v1/sessions/active")
                            .header("Authorization", "Bearer jwt-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.length()").value(5));

            verify(sessionService).getActiveSessions(anyString());
        }

        @Test
        @DisplayName("Should fail when no authorization header provided")
        void shouldFailGetActiveSessions_WhenNoAuthHeader() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/v1/sessions/active"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/sessions/{id} - Terminate Specific Session (FR-013)")
    class TerminateSessionTests {

        @Test
        @DisplayName("Should terminate specific session successfully")
        void shouldTerminateSession_Success() throws Exception {
            // Given
            UUID sessionId = UUID.randomUUID();
            doNothing().when(sessionService).terminateSession(eq(sessionId), anyString());

            // When & Then
            mockMvc.perform(delete("/api/v1/sessions/{id}", sessionId)
                            .header("Authorization", "Bearer jwt-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.message").exists());

            verify(sessionService).terminateSession(eq(sessionId), anyString());
        }

        @Test
        @DisplayName("Should fail terminate when session ID is invalid")
        void shouldFailTerminate_WhenIdInvalid() throws Exception {
            // When & Then
            mockMvc.perform(delete("/api/v1/sessions/{id}", "invalid-uuid")
                            .header("Authorization", "Bearer jwt-token"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should fail when no authorization header provided")
        void shouldFailTerminate_WhenNoAuthHeader() throws Exception {
            // Given
            UUID sessionId = UUID.randomUUID();

            // When & Then
            mockMvc.perform(delete("/api/v1/sessions/{id}", sessionId))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/sessions/all-except-current - Terminate All Other Sessions (FR-013)")
    class TerminateAllOtherSessionsTests {

        @Test
        @DisplayName("Should terminate all other sessions successfully")
        void shouldTerminateAllOtherSessions_Success() throws Exception {
            // Given
            doNothing().when(sessionService).terminateAllOtherSessions(anyString());

            // When & Then
            mockMvc.perform(delete("/api/v1/sessions/all-except-current")
                            .header("Authorization", "Bearer jwt-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.message").exists());

            verify(sessionService).terminateAllOtherSessions(anyString());
        }

        @Test
        @DisplayName("Should keep current session active after terminating others")
        void shouldKeepCurrentSession_AfterTerminatingOthers() throws Exception {
            // Given
            doNothing().when(sessionService).terminateAllOtherSessions(anyString());

            // When & Then
            mockMvc.perform(delete("/api/v1/sessions/all-except-current")
                            .header("Authorization", "Bearer jwt-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.message").value("All other sessions terminated successfully"));

            verify(sessionService).terminateAllOtherSessions(anyString());
        }

        @Test
        @DisplayName("Should fail when no authorization header provided")
        void shouldFailTerminateAll_WhenNoAuthHeader() throws Exception {
            // When & Then
            mockMvc.perform(delete("/api/v1/sessions/all-except-current"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should succeed even when only one session exists")
        void shouldTerminateAll_WhenOnlyOneSession() throws Exception {
            // Given - Only current session exists, nothing to terminate
            doNothing().when(sessionService).terminateAllOtherSessions(anyString());

            // When & Then
            mockMvc.perform(delete("/api/v1/sessions/all-except-current")
                            .header("Authorization", "Bearer jwt-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"));

            verify(sessionService).terminateAllOtherSessions(anyString());
        }
    }

    @Nested
    @DisplayName("Session TTL Tests (NFR-003)")
    class SessionTtlTests {

        @Test
        @DisplayName("Should return session with 24-hour expiry from creation")
        void shouldReturnSession_With24HourExpiry() throws Exception {
            // Given
            OffsetDateTime now = OffsetDateTime.now();
            SessionDto sessionWith24HExpiry = SessionDto.builder()
                    .id(UUID.randomUUID())
                    .userId(UUID.randomUUID())
                    .deviceType("desktop")
                    .isActive(true)
                    .isCurrent(true)
                    .createdAt(now)
                    .expiresAt(now.plusHours(24)) // 24-hour TTL
                    .lastActivity(now)
                    .build();

            when(sessionService.getActiveSessions(anyString()))
                    .thenReturn(List.of(sessionWith24HExpiry));

            // When & Then
            mockMvc.perform(get("/api/v1/sessions/active")
                            .header("Authorization", "Bearer jwt-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].expiresAt").exists())
                    .andExpect(jsonPath("$.data[0].createdAt").exists());

            verify(sessionService).getActiveSessions(anyString());
        }
    }

    /**
     * Helper method to create SessionDto for testing.
     */
    private SessionDto createSessionDto(String deviceType, boolean isCurrent) {
        OffsetDateTime now = OffsetDateTime.now();
        return SessionDto.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .deviceType(deviceType)
                .deviceId("device-" + UUID.randomUUID())
                .browser("Chrome")
                .os("Windows 10")
                .ipAddress("192.168.1.1")
                .isActive(true)
                .isCurrent(isCurrent)
                .lastActivity(now)
                .expiresAt(now.plusHours(24))
                .createdAt(now.minusHours(1))
                .isSuspicious(false)
                .riskScore(0)
                .requires2fa(false)
                .twoFaCompleted(false)
                .build();
    }
}
