package com.cena.traveloka.iam.controller;

import com.cena.traveloka.common.dto.ApiResponse;
import com.cena.traveloka.common.dto.PageResponse;
import com.cena.traveloka.iam.dto.request.UpdateProfileRequest;
import com.cena.traveloka.iam.dto.response.UserDetailDto;
import com.cena.traveloka.iam.dto.response.UserDto;
import com.cena.traveloka.iam.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * T069-T074: UserController
 * REST API controller for user management operations.
 *
 * Endpoints:
 * - GET /api/v1/users/me (T069)
 * - PUT /api/v1/users/me (T070)
 * - GET /api/v1/users (T071) - Admin only
 * - GET /api/v1/users/{id} (T072) - Admin only
 * - POST /api/v1/users/{id}/lock (T073) - Admin only
 * - POST /api/v1/users/{id}/unlock (T074) - Admin only
 *
 * Constitutional Compliance:
 * - Principle III: Layered Architecture - Controller delegates to service layer
 * - Principle IV: Entity Immutability - Uses DTOs for API contracts
 * - FR-019: User profile viewing and updating
 * - FR-008: Account lockout management
 * - FR-005: Role-based access control
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * T069: Get current user profile (FR-019).
     *
     * @param authHeader Authorization header with JWT token
     * @return ApiResponse with UserDetailDto
     */
    @GetMapping("/me")
    public ApiResponse<UserDetailDto> getCurrentUser(
            @RequestHeader("Authorization") String authHeader
    ) {
        String token = extractToken(authHeader);
        log.info("Get current user profile request");

        UserDetailDto userDetail = userService.getCurrentUser(token);

        return ApiResponse.success(
                "User profile retrieved successfully",
                userDetail
        );
    }

    /**
     * T070: Update current user profile (FR-019).
     *
     * @param request Update profile request
     * @param authHeader Authorization header with JWT token
     * @return ApiResponse with updated UserDetailDto
     */
    @PutMapping("/me")
    public ApiResponse<UserDetailDto> updateCurrentUser(
            @Valid @RequestBody UpdateProfileRequest request,
            @RequestHeader("Authorization") String authHeader
    ) {
        String token = extractToken(authHeader);
        log.info("Update current user profile request");

        UserDetailDto updatedUser = userService.updateCurrentUser(request, token);

        return ApiResponse.success(
                "Profile updated successfully",
                updatedUser
        );
    }

    /**
     * T071: Get all users with pagination (Admin only).
     *
     * @param page Page number (default: 0)
     * @param size Page size (default: 20, max: 100)
     * @return ApiResponse with PageResponse of UserDto
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<PageResponse<UserDto>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("Get all users request - page: {}, size: {}", page, size);

        // Enforce max page size
        size = Math.min(size, 100);

        Pageable pageable = PageRequest.of(page, size);
        PageResponse<UserDto> users = userService.getAllUsers(pageable);

        return ApiResponse.success(
                "Users retrieved successfully",
                users
        );
    }

    /**
     * T072: Get user by ID (Admin only).
     *
     * @param id User ID
     * @return ApiResponse with UserDetailDto
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<UserDetailDto> getUserById(
            @PathVariable UUID id
    ) {
        log.info("Get user by ID request: {}", id);

        UserDetailDto user = userService.getUserById(id);

        return ApiResponse.success(
                "User retrieved successfully",
                user
        );
    }

    /**
     * T073: Lock user account (Admin only, FR-008).
     *
     * @param id User ID to lock
     * @param reason Lock reason (optional)
     * @return ApiResponse with success message
     */
    @PostMapping("/{id}/lock")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<Void> lockUser(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason
    ) {
        log.info("Lock user account request: {} - Reason: {}", id, reason);

        userService.lockUser(id, reason);

        return ApiResponse.success(
                "User account locked successfully",
                null
        );
    }

    /**
     * T074: Unlock user account (Admin only, FR-008).
     *
     * @param id User ID to unlock
     * @return ApiResponse with success message
     */
    @PostMapping("/{id}/unlock")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<Void> unlockUser(
            @PathVariable UUID id
    ) {
        log.info("Unlock user account request: {}", id);

        userService.unlockUser(id);

        return ApiResponse.success(
                "User account unlocked successfully",
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
