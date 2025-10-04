package com.cena.traveloka.iam.controller;

import com.cena.traveloka.common.dto.ApiResponse;
import com.cena.traveloka.iam.dto.response.SessionDto;
import com.cena.traveloka.iam.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * T075-T077: SessionController
 * REST API controller for session management operations.
 *
 * Endpoints:
 * - GET /api/v1/sessions/active (T075)
 * - DELETE /api/v1/sessions/{id} (T076)
 * - DELETE /api/v1/sessions/all-except-current (T077)
 *
 * Constitutional Compliance:
 * - Principle III: Layered Architecture - Controller delegates to service layer
 * - Principle IV: Entity Immutability - Uses DTOs for API contracts
 * - FR-013: Session listing and management
 * - FR-016: Maximum 5 concurrent sessions per user
 * - NFR-003: 24-hour session TTL
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    /**
     * T075: Get all active sessions for current user (FR-013).
     *
     * @param authHeader Authorization header with JWT token
     * @return ApiResponse with list of SessionDto
     */
    @GetMapping("/active")
    public ApiResponse<List<SessionDto>> getActiveSessions(
            @RequestHeader("Authorization") String authHeader
    ) {
        String token = extractToken(authHeader);
        log.info("Get active sessions request");

        List<SessionDto> sessions = sessionService.getActiveSessions(token);

        return ApiResponse.success(
                "Active sessions retrieved successfully",
                sessions
        );
    }

    /**
     * T076: Terminate specific session by ID (FR-013).
     *
     * @param id Session ID to terminate
     * @param authHeader Authorization header with JWT token
     * @return ApiResponse with success message
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> terminateSession(
            @PathVariable UUID id,
            @RequestHeader("Authorization") String authHeader
    ) {
        String token = extractToken(authHeader);
        log.info("Terminate session request for ID: {}", id);

        sessionService.terminateSessionWithAuth(id, token);

        return ApiResponse.success(
                "Session terminated successfully",
                null
        );
    }

    /**
     * T077: Terminate all sessions except current one (FR-013).
     * Useful for security purposes when user wants to logout from all other devices.
     *
     * @param authHeader Authorization header with JWT token
     * @return ApiResponse with success message
     */
    @DeleteMapping("/all-except-current")
    public ApiResponse<Void> terminateAllOtherSessions(
            @RequestHeader("Authorization") String authHeader
    ) {
        String token = extractToken(authHeader);
        log.info("Terminate all other sessions request");

        sessionService.terminateAllOtherSessions(token);

        return ApiResponse.success(
                "All other sessions terminated successfully",
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
}
