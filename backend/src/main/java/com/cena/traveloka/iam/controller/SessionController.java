package com.cena.traveloka.iam.controller;

import com.cena.traveloka.common.dto.ApiResponse;
import com.cena.traveloka.iam.dto.response.SessionDto;
import com.cena.traveloka.iam.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

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

    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        throw new IllegalArgumentException("Invalid Authorization header format");
    }
}
