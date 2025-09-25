package com.cena.traveloka.iam.controller;

import com.cena.traveloka.iam.dto.request.UserCreateReq;
import com.cena.traveloka.iam.dto.request.UserSearchReq;
import com.cena.traveloka.iam.dto.request.UserUpdateReq;
import com.cena.traveloka.iam.dto.response.*;
import com.cena.traveloka.iam.entity.AppUser;
import com.cena.traveloka.iam.enums.UserStatus;
import com.cena.traveloka.iam.mapper.UserMapper;
import com.cena.traveloka.iam.service.UserService;
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
 * REST Controller for User Management
 *
 * Provides endpoints for user CRUD operations, search, and administration
 * Secured with OAuth2 and role-based access control
 */
@RestController
@RequestMapping("/api/iam/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Management", description = "User CRUD operations and administration")
@SecurityRequirement(name = "bearer-jwt")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    // === User CRUD Operations ===

    /**
     * Create a new user
     */
    @PostMapping
    @Operation(summary = "Create a new user", description = "Creates a new user account with provided information")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "User created successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid user data"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "User already exists")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'USER_MANAGER')")
    public ResponseEntity<ApiResponse<UserRes>> createUser(
            @Valid @RequestBody UserCreateReq request) {

        log.info("Creating new user with email: {}", request.getEmail());

        try {
            AppUser user = userMapper.toAppUser(request);
            AppUser createdUser = userService.createUser(user);
            UserRes userRes = userMapper.toUserRes(createdUser);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("User created successfully", userRes));

        } catch (IllegalArgumentException e) {
            log.warn("Failed to create user: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * Get user by ID
     */
    @GetMapping("/{userId}")
    @Operation(summary = "Get user by ID", description = "Retrieves user information by user ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER_MANAGER') or #userId == authentication.name")
    public ResponseEntity<ApiResponse<UserRes>> getUserById(
            @Parameter(description = "User ID") @PathVariable String userId) {

        log.debug("Getting user by ID: {}", userId);

        Optional<AppUser> user = userService.findById(userId);
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound("User not found: " + userId));
        }

        UserRes userRes = userMapper.toUserRes(user.get());
        return ResponseEntity.ok(ApiResponse.success(userRes));
    }

    /**
     * Get user by email
     */
    @GetMapping("/email/{email}")
    @Operation(summary = "Get user by email", description = "Retrieves user information by email address")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER_MANAGER')")
    public ResponseEntity<ApiResponse<UserRes>> getUserByEmail(
            @Parameter(description = "User email") @PathVariable String email) {

        log.debug("Getting user by email: {}", email);

        Optional<AppUser> user = userService.findByEmail(email);
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound("User not found with email: " + email));
        }

        UserRes userRes = userMapper.toUserRes(user.get());
        return ResponseEntity.ok(ApiResponse.success(userRes));
    }

    /**
     * Update user information
     */
    @PutMapping("/{userId}")
    @Operation(summary = "Update user", description = "Updates user information")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER_MANAGER') or #userId == authentication.name")
    public ResponseEntity<ApiResponse<UserRes>> updateUser(
            @Parameter(description = "User ID") @PathVariable String userId,
            @Valid @RequestBody UserUpdateReq request) {

        log.info("Updating user: {}", userId);

        try {
            Optional<AppUser> existingUser = userService.findById(userId);
            if (existingUser.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.notFound("User not found: " + userId));
            }

            AppUser user = existingUser.get();
            userMapper.updateAppUserFromDto(request, user);
            AppUser updatedUser = userService.updateUser(user);
            UserRes userRes = userMapper.toUserRes(updatedUser);

            return ResponseEntity.ok(ApiResponse.success("User updated successfully", userRes));

        } catch (IllegalArgumentException e) {
            log.warn("Failed to update user {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * Delete user (soft delete)
     */
    @DeleteMapping("/{userId}")
    @Operation(summary = "Delete user", description = "Soft deletes a user account")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @Parameter(description = "User ID") @PathVariable String userId) {

        log.info("Deleting user: {}", userId);

        try {
            userService.deleteUser(userId, "ADMIN"); // TODO: Get current user from security context
            return ResponseEntity.ok(ApiResponse.success("User deleted successfully"));

        } catch (IllegalArgumentException e) {
            log.warn("Failed to delete user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound(e.getMessage()));
        }
    }

    // === User Search and Listing ===

    /**
     * Search users with filters and pagination
     */
    @PostMapping("/search")
    @Operation(summary = "Search users", description = "Search users with filters and pagination")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER_MANAGER')")
    public ResponseEntity<PagedResponse<UserSummaryRes>> searchUsers(
            @Valid @RequestBody UserSearchReq request) {

        log.debug("Searching users with filters: {}", request);

        try {
            Pageable pageable = createPageable(request.getPage(), request.getSize(),
                    request.getSortBy(), request.getSortDirection());

            Page<AppUser> users = userService.searchUsers(
                    request.getEmail(),
                    request.getFirstName(),
                    request.getLastName(),
                    request.getStatus(),
                    request.getMinCompleteness(),
                    pageable
            );

            List<UserSummaryRes> userSummaries = userMapper.toUserSummaryResList(users.getContent());
            PagedResponse<UserSummaryRes> response = PagedResponse.of(userSummaries, users);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error searching users", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(PagedResponse.<UserSummaryRes>builder()
                            .success(false)
                            .message("Search failed: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Get all active users
     */
    @GetMapping("/active")
    @Operation(summary = "Get active users", description = "Retrieves all active users")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER_MANAGER')")
    public ResponseEntity<ApiResponse<List<UserSummaryRes>>> getActiveUsers() {

        log.debug("Getting all active users");

        List<AppUser> activeUsers = userService.findActiveUsers();
        List<UserSummaryRes> userSummaries = userMapper.toUserSummaryResList(activeUsers);

        return ResponseEntity.ok(ApiResponse.success(userSummaries));
    }

    /**
     * Get users by status with pagination
     */
    @GetMapping("/status/{status}")
    @Operation(summary = "Get users by status", description = "Retrieves users filtered by status with pagination")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER_MANAGER')")
    public ResponseEntity<PagedResponse<UserSummaryRes>> getUsersByStatus(
            @Parameter(description = "User status") @PathVariable UserStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        log.debug("Getting users by status: {} (page: {}, size: {})", status, page, size);

        Pageable pageable = createPageable(page, size, sortBy, sortDirection);
        Page<AppUser> users = userService.findUsersByStatus(status, pageable);

        List<UserSummaryRes> userSummaries = userMapper.toUserSummaryResList(users.getContent());
        PagedResponse<UserSummaryRes> response = PagedResponse.of(userSummaries, users);

        return ResponseEntity.ok(response);
    }

    // === User Administration ===

    /**
     * Activate user
     */
    @PostMapping("/{userId}/activate")
    @Operation(summary = "Activate user", description = "Activates a user account")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> activateUser(
            @Parameter(description = "User ID") @PathVariable String userId) {

        log.info("Activating user: {}", userId);

        try {
            userService.activateUser(userId, "ADMIN"); // TODO: Get current user from security context
            return ResponseEntity.ok(ApiResponse.success("User activated successfully"));

        } catch (IllegalArgumentException e) {
            log.warn("Failed to activate user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound(e.getMessage()));
        }
    }

    /**
     * Suspend user
     */
    @PostMapping("/{userId}/suspend")
    @Operation(summary = "Suspend user", description = "Suspends a user account")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> suspendUser(
            @Parameter(description = "User ID") @PathVariable String userId) {

        log.info("Suspending user: {}", userId);

        try {
            userService.suspendUser(userId, "ADMIN"); // TODO: Get current user from security context
            return ResponseEntity.ok(ApiResponse.success("User suspended successfully"));

        } catch (IllegalArgumentException e) {
            log.warn("Failed to suspend user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound(e.getMessage()));
        }
    }

    /**
     * Bulk update user status
     */
    @PostMapping("/bulk/status")
    @Operation(summary = "Bulk update user status", description = "Updates status for multiple users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> bulkUpdateStatus(
            @RequestParam List<String> userIds,
            @RequestParam UserStatus status) {

        log.info("Bulk updating status for {} users to: {}", userIds.size(), status);

        try {
            int updatedCount = userService.updateUserStatus(userIds, status, "ADMIN");
            return ResponseEntity.ok(ApiResponse.success(
                    "Updated successfully",
                    String.format("Updated status for %d users", updatedCount)));

        } catch (Exception e) {
            log.error("Failed to bulk update user status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.internalServerError("Bulk update failed: " + e.getMessage()));
        }
    }

    // === Analytics and Statistics ===

    /**
     * Get user statistics
     */
    @GetMapping("/statistics")
    @Operation(summary = "Get user statistics", description = "Retrieves user statistics for dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER_MANAGER')")
    public ResponseEntity<ApiResponse<Object>> getUserStatistics() {

        log.debug("Getting user statistics");

        try {
            var statistics = userService.getUserStatistics();
            return ResponseEntity.ok(ApiResponse.success(statistics));

        } catch (Exception e) {
            log.error("Failed to get user statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.internalServerError("Failed to retrieve statistics"));
        }
    }

    /**
     * Count users by status
     */
    @GetMapping("/count/{status}")
    @Operation(summary = "Count users by status", description = "Returns count of users with specific status")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER_MANAGER')")
    public ResponseEntity<ApiResponse<Long>> countUsersByStatus(
            @Parameter(description = "User status") @PathVariable UserStatus status) {

        log.debug("Counting users by status: {}", status);

        long count = userService.countUsersByStatus(status);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    /**
     * Get users with incomplete profiles
     */
    @GetMapping("/incomplete-profiles")
    @Operation(summary = "Get users with incomplete profiles", description = "Retrieves users with profile completeness below threshold")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER_MANAGER')")
    public ResponseEntity<ApiResponse<List<UserSummaryRes>>> getUsersWithIncompleteProfiles(
            @RequestParam(defaultValue = "80") int threshold) {

        log.debug("Getting users with profile completeness below: {}%", threshold);

        List<AppUser> users = userService.findUsersWithIncompleteProfiles(threshold);
        List<UserSummaryRes> userSummaries = userMapper.toUserSummaryResList(users);

        return ResponseEntity.ok(ApiResponse.success(userSummaries));
    }

    /**
     * Get recently created users
     */
    @GetMapping("/recent")
    @Operation(summary = "Get recently created users", description = "Retrieves users created within specified hours")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER_MANAGER')")
    public ResponseEntity<ApiResponse<List<UserSummaryRes>>> getRecentlyCreatedUsers(
            @RequestParam(defaultValue = "24") int hours) {

        log.debug("Getting users created in the last {} hours", hours);

        List<AppUser> users = userService.findRecentlyCreatedUsers(hours);
        List<UserSummaryRes> userSummaries = userMapper.toUserSummaryResList(users);

        return ResponseEntity.ok(ApiResponse.success(userSummaries));
    }

    // === Utility Methods ===

    private Pageable createPageable(int page, int size, String sortBy, String sortDirection) {
        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDirection) ?
                Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, sortBy);
        return PageRequest.of(page, Math.min(size, 100), sort);
    }
}
