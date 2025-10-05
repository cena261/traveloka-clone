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

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

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

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<PageResponse<UserDto>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("Get all users request - page: {}, size: {}", page, size);

        size = Math.min(size, 100);

        Pageable pageable = PageRequest.of(page, size);
        PageResponse<UserDto> users = userService.getAllUsers(pageable);

        return ApiResponse.success(
                "Users retrieved successfully",
                users
        );
    }

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

    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        throw new IllegalArgumentException("Invalid Authorization header format");
    }
}
