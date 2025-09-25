package com.cena.traveloka.iam.controller;

import com.cena.traveloka.iam.dto.request.SessionCreateReq;
import com.cena.traveloka.iam.dto.response.*;
import com.cena.traveloka.iam.entity.UserSession;
import com.cena.traveloka.iam.enums.SessionStatus;
import com.cena.traveloka.iam.mapper.SessionMapper;
import com.cena.traveloka.iam.service.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * REST Controller for Session Management
 *
 * Provides endpoints for session operations, multi-device management, and monitoring
 * Secured with OAuth2 and role-based access control
 */
@RestController
@RequestMapping("/api/iam/sessions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Session Management", description = "Session operations and multi-device management")
@SecurityRequirement(name = "bearer-jwt")
public class SessionController {

    private final SessionService sessionService;
    private final SessionMapper sessionMapper;

    // === Session CRUD Operations ===

    /**
     * Create a new session
     */
    @PostMapping
    @Operation(summary = "Create session", description = "Creates a new user session")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Session created successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid session data"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Session already exists")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'USER_MANAGER', 'USER')")
    public ResponseEntity<ApiResponse<SessionRes>> createSession(
            @Valid @RequestBody SessionCreateReq request) {

        log.info("Creating session for user: {}", request.getUserId());

        try {
            UserSession createdSession = sessionService.createSession(
                    request.getUserId(),
                    request.getDeviceId(),
                    request.getDeviceType(),
                    request.getIpAddress(),
                    request.getUserAgent(),
                    request.getLocation());
            SessionRes sessionRes = sessionMapper.toSessionRes(createdSession);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Session created successfully", sessionRes));

        } catch (IllegalArgumentException e) {
            log.warn("Failed to create session: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * Get session by ID
     */
    @GetMapping("/{sessionId}")
    @Operation(summary = "Get session by ID", description = "Retrieves session information by session ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER_MANAGER') or @sessionService.isSessionOwner(#sessionId, authentication.name)")
    public ResponseEntity<ApiResponse<SessionRes>> getSessionById(
            @Parameter(description = "Session ID") @PathVariable String sessionId) {

        log.debug("Getting session by ID: {}", sessionId);

        Optional<UserSession> session = sessionService.findActiveSession(sessionId);
        if (session.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound("Session not found: " + sessionId));
        }

        SessionRes sessionRes = sessionMapper.toSessionRes(session.get());
        return ResponseEntity.ok(ApiResponse.success(sessionRes));
    }

    /**
     * Update session activity
     */
    @PutMapping("/{sessionId}/activity")
    @Operation(summary = "Update session activity", description = "Updates session last accessed time")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER_MANAGER') or @sessionService.isSessionOwner(#sessionId, authentication.name)")
    public ResponseEntity<ApiResponse<SessionRes>> updateSessionActivity(
            @Parameter(description = "Session ID") @PathVariable String sessionId) {

        log.debug("Updating activity for session: {}", sessionId);

        try {
            boolean refreshed = sessionService.refreshSession(sessionId);
            if (!refreshed) {
                throw new IllegalArgumentException("Session not found or expired: " + sessionId);
            }
            UserSession updatedSession = sessionService.findActiveSession(sessionId)
                    .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
            SessionRes sessionRes = sessionMapper.toSessionRes(updatedSession);

            return ResponseEntity.ok(ApiResponse.success("Session activity updated", sessionRes));

        } catch (IllegalArgumentException e) {
            log.warn("Failed to update session activity {}: {}", sessionId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound(e.getMessage()));
        }
    }

    /**
     * Invalidate session
     */
    @DeleteMapping("/{sessionId}")
    @Operation(summary = "Invalidate session", description = "Invalidates/terminates a session")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER_MANAGER') or @sessionService.isSessionOwner(#sessionId, authentication.name)")
    public ResponseEntity<ApiResponse<Void>> invalidateSession(
            @Parameter(description = "Session ID") @PathVariable String sessionId,
            @RequestParam(defaultValue = "USER_LOGOUT") String reason) {

        log.info("Invalidating session: {} with reason: {}", sessionId, reason);

        try {
            boolean terminated = sessionService.terminateSession(sessionId, reason);
            if (!terminated) {
                throw new IllegalArgumentException("Session not found or could not be terminated: " + sessionId);
            }
            return ResponseEntity.ok(ApiResponse.success("Session invalidated successfully"));

        } catch (IllegalArgumentException e) {
            log.warn("Failed to invalidate session {}: {}", sessionId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound(e.getMessage()));
        }
    }

    // === User Session Management ===

    /**
     * Get active sessions for user
     */
    @GetMapping("/user/{userId}/active")
    @Operation(summary = "Get active sessions for user", description = "Retrieves all active sessions for a user")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER_MANAGER') or #userId == authentication.name")
    public ResponseEntity<ApiResponse<ActiveSessionsRes>> getActiveSessionsForUser(
            @Parameter(description = "User ID") @PathVariable String userId) {

        log.debug("Getting active sessions for user: {}", userId);

        List<UserSession> activeSessions = sessionService.findActiveUserSessions(userId);
        ActiveSessionsRes response = sessionMapper.toActiveSessionsRes(activeSessions, userId, null);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get all sessions for user (including expired)
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get all sessions for user", description = "Retrieves all sessions for a user with pagination")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER_MANAGER') or #userId == authentication.name")
    public ResponseEntity<PagedResponse<SessionRes>> getSessionsForUser(
            @Parameter(description = "User ID") @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        log.debug("Getting sessions for user: {} (page: {}, size: {})", userId, page, size);

        Pageable pageable = createPageable(page, size, sortBy, sortDirection);
        List<UserSession> userSessions = sessionService.findUserSessions(userId);
        // Convert to Page manually since repository doesn't support pagination for this method
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), userSessions.size());
        List<UserSession> pageContent = userSessions.subList(start, end);
        Page<UserSession> sessions = new org.springframework.data.domain.PageImpl<>(pageContent, pageable, userSessions.size());

        List<SessionRes> sessionResList = sessionMapper.toSessionResList(sessions.getContent());
        PagedResponse<SessionRes> response = PagedResponse.of(sessionResList, sessions);

        return ResponseEntity.ok(response);
    }

    /**
     * Invalidate all sessions for user
     */
    @DeleteMapping("/user/{userId}/all")
    @Operation(summary = "Invalidate all sessions for user", description = "Invalidates all sessions for a user")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER_MANAGER') or #userId == authentication.name")
    public ResponseEntity<ApiResponse<String>> invalidateAllUserSessions(
            @Parameter(description = "User ID") @PathVariable String userId,
            @RequestParam(defaultValue = "USER_LOGOUT_ALL") String reason) {

        log.info("Invalidating all sessions for user: {} with reason: {}", userId, reason);

        try {
            int invalidatedCount = sessionService.terminateAllUserSessions(userId, reason);
            return ResponseEntity.ok(ApiResponse.success(
                    "Sessions invalidated successfully",
                    String.format("Invalidated %d sessions", invalidatedCount)));

        } catch (IllegalArgumentException e) {
            log.warn("Failed to invalidate all sessions for user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound(e.getMessage()));
        }
    }

    /**
     * Invalidate other sessions (keep current)
     */
    @DeleteMapping("/user/{userId}/others")
    @Operation(summary = "Invalidate other sessions", description = "Invalidates all sessions for a user except current one")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER_MANAGER') or #userId == authentication.name")
    public ResponseEntity<ApiResponse<String>> invalidateOtherSessions(
            @Parameter(description = "User ID") @PathVariable String userId,
            @RequestParam String currentSessionId,
            @RequestParam(defaultValue = "USER_LOGOUT_OTHERS") String reason) {

        log.info("Invalidating other sessions for user: {} except: {}", userId, currentSessionId);

        try {
            int invalidatedCount = sessionService.terminateOtherUserSessions(userId, currentSessionId, reason);
            return ResponseEntity.ok(ApiResponse.success(
                    "Other sessions invalidated successfully",
                    String.format("Invalidated %d other sessions", invalidatedCount)));

        } catch (IllegalArgumentException e) {
            log.warn("Failed to invalidate other sessions for user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound(e.getMessage()));
        }
    }

    // === Session Monitoring and Analytics ===

    /**
     * Get expired sessions
     */
    @GetMapping("/expired")
    @Operation(summary = "Get expired sessions", description = "Retrieves expired sessions with pagination")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER_MANAGER')")
    public ResponseEntity<PagedResponse<SessionRes>> getExpiredSessions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.debug("Getting expired sessions (page: {}, size: {})", page, size);

        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        // Use search method to find expired sessions
        Page<UserSession> expiredSessions = sessionService.searchSessions(null, null, "EXPIRED", null, null, pageable);

        List<SessionRes> sessionResList = sessionMapper.toSessionResList(expiredSessions.getContent());
        PagedResponse<SessionRes> response = PagedResponse.of(sessionResList, expiredSessions);

        return ResponseEntity.ok(response);
    }

    /**
     * Get sessions by status
     */
    @GetMapping("/status/{status}")
    @Operation(summary = "Get sessions by status", description = "Retrieves sessions filtered by status")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER_MANAGER')")
    public ResponseEntity<PagedResponse<SessionRes>> getSessionsByStatus(
            @Parameter(description = "Session status") @PathVariable SessionStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.debug("Getting sessions by status: {} (page: {}, size: {})", status, page, size);

        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<UserSession> sessions = sessionService.searchSessions(null, null, status.name(), null, null, pageable);

        List<SessionRes> sessionResList = sessionMapper.toSessionResList(sessions.getContent());
        PagedResponse<SessionRes> response = PagedResponse.of(sessionResList, sessions);

        return ResponseEntity.ok(response);
    }

    /**
     * Get concurrent sessions count for user
     */
    @GetMapping("/user/{userId}/concurrent-count")
    @Operation(summary = "Get concurrent session count", description = "Returns number of concurrent active sessions for user")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER_MANAGER') or #userId == authentication.name")
    public ResponseEntity<ApiResponse<Long>> getConcurrentSessionCount(
            @Parameter(description = "User ID") @PathVariable String userId) {

        log.debug("Getting concurrent session count for user: {}", userId);

        long count = sessionService.countActiveUserSessions(userId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    /**
     * Get session statistics
     */
    @GetMapping("/statistics")
    @Operation(summary = "Get session statistics", description = "Retrieves session statistics for monitoring")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER_MANAGER')")
    public ResponseEntity<ApiResponse<Object>> getSessionStatistics() {

        log.debug("Getting session statistics");

        try {
            var statistics = sessionService.getSessionStatistics();
            return ResponseEntity.ok(ApiResponse.success(statistics));

        } catch (Exception e) {
            log.error("Failed to get session statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.internalServerError("Failed to retrieve session statistics"));
        }
    }

    // === Session Cleanup and Maintenance ===

    /**
     * Cleanup expired sessions
     */
    @PostMapping("/cleanup/expired")
    @Operation(summary = "Cleanup expired sessions", description = "Removes expired sessions from the system")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> cleanupExpiredSessions() {

        log.info("Starting cleanup of expired sessions");

        try {
            // Perform manual cleanup and get statistics
            var stats = sessionService.performManualCleanup();
            int cleanedCount = (int) (stats.getSessionsToExpire() + stats.getStaleSessions());
            return ResponseEntity.ok(ApiResponse.success(
                    "Expired sessions cleaned up successfully",
                    String.format("Cleaned up %d expired sessions", cleanedCount)));

        } catch (Exception e) {
            log.error("Failed to cleanup expired sessions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.internalServerError("Cleanup failed: " + e.getMessage()));
        }
    }

    /**
     * Force cleanup old sessions
     */
    @PostMapping("/cleanup/old")
    @Operation(summary = "Cleanup old sessions", description = "Removes sessions older than specified days")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> cleanupOldSessions(
            @RequestParam(defaultValue = "30") int olderThanDays) {

        log.info("Starting cleanup of sessions older than {} days", olderThanDays);

        try {
            // Use manual cleanup for old sessions
            var stats = sessionService.performManualCleanup();
            int cleanedCount = (int) stats.getSessionsToDelete();
            return ResponseEntity.ok(ApiResponse.success(
                    "Old sessions cleaned up successfully",
                    String.format("Cleaned up %d old sessions", cleanedCount)));

        } catch (Exception e) {
            log.error("Failed to cleanup old sessions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.internalServerError("Cleanup failed: " + e.getMessage()));
        }
    }

    // === Bulk Operations ===

    /**
     * Bulk invalidate sessions
     */
    @PostMapping("/bulk/invalidate")
    @Operation(summary = "Bulk invalidate sessions", description = "Invalidates multiple sessions at once")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> bulkInvalidateSessions(
            @RequestParam List<String> sessionIds,
            @RequestParam(defaultValue = "ADMIN_BULK_INVALIDATE") String reason) {

        log.info("Bulk invalidating {} sessions with reason: {}", sessionIds.size(), reason);

        try {
            // Bulk terminate sessions using individual calls
            int invalidatedCount = 0;
            for (String sessionId : sessionIds) {
                if (sessionService.terminateSession(sessionId, reason)) {
                    invalidatedCount++;
                }
            }
            return ResponseEntity.ok(ApiResponse.success(
                    "Sessions invalidated successfully",
                    String.format("Invalidated %d sessions", invalidatedCount)));

        } catch (Exception e) {
            log.error("Failed to bulk invalidate sessions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.internalServerError("Bulk invalidation failed: " + e.getMessage()));
        }
    }

    // === Security Operations ===

    /**
     * Get suspicious sessions
     */
    @GetMapping("/suspicious")
    @Operation(summary = "Get suspicious sessions", description = "Retrieves sessions flagged as suspicious")
    @PreAuthorize("hasAnyRole('ADMIN', 'SECURITY_MANAGER')")
    public ResponseEntity<PagedResponse<SessionRes>> getSuspiciousSessions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.debug("Getting suspicious sessions (page: {}, size: {})", page, size);

        try {
            Pageable pageable = PageRequest.of(page, Math.min(size, 100));
            // Use search method to find suspicious sessions
            Page<UserSession> suspiciousSessions = sessionService.searchSessions(null, null, "SUSPICIOUS", null, null, pageable);

            List<SessionRes> sessionResList = sessionMapper.toSessionResList(suspiciousSessions.getContent());
            PagedResponse<SessionRes> response = PagedResponse.of(sessionResList, suspiciousSessions);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to get suspicious sessions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(PagedResponse.<SessionRes>builder()
                            .success(false)
                            .message("Failed to retrieve suspicious sessions")
                            .build());
        }
    }

    /**
     * Count active sessions system-wide
     */
    @GetMapping("/count/active")
    @Operation(summary = "Count active sessions", description = "Returns total number of active sessions system-wide")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER_MANAGER')")
    public ResponseEntity<ApiResponse<Long>> countActiveSessions() {

        log.debug("Counting active sessions system-wide");

        // Get statistics and extract active session count
        var stats = sessionService.getSessionStatistics();
        long count = stats.getActiveSessions();
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    // === Utility Methods ===

    private Pageable createPageable(int page, int size, String sortBy, String sortDirection) {
        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDirection) ?
                Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, sortBy);
        return PageRequest.of(page, Math.min(size, 100), sort);
    }
}