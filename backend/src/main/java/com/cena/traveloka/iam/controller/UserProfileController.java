package com.cena.traveloka.iam.controller;

import com.cena.traveloka.iam.dto.request.UserProfileCreateReq;
import com.cena.traveloka.iam.dto.request.UserProfileUpdateReq;
import com.cena.traveloka.iam.dto.response.*;
import com.cena.traveloka.iam.entity.UserProfile;
import com.cena.traveloka.iam.mapper.UserProfileMapper;
import com.cena.traveloka.iam.service.UserProfileService;
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
 * REST Controller for User Profile Management
 *
 * Provides endpoints for user profile operations, verification, and analytics
 * Secured with OAuth2 and role-based access control
 */
@RestController
@RequestMapping("/api/iam/profiles")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Profile Management", description = "User profile operations and verification")
@SecurityRequirement(name = "bearer-jwt")
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final UserProfileMapper userProfileMapper;

    // === Profile CRUD Operations ===

    /**
     * Create user profile
     */
    @PostMapping
    @Operation(summary = "Create user profile", description = "Creates a new user profile with provided information")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Profile created successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid profile data"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Profile already exists")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'USER_MANAGER') or #request.userId == authentication.name")
    public ResponseEntity<ApiResponse<UserProfileRes>> createProfile(
            @Valid @RequestBody UserProfileCreateReq request) {

        log.info("Creating profile for user: {}", request.getUserId());

        try {
            UserProfile profile = userProfileMapper.toUserProfile(request);
            UserProfile createdProfile = userProfileService.createProfile(profile);
            UserProfileRes profileRes = userProfileMapper.toUserProfileRes(createdProfile);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Profile created successfully", profileRes));

        } catch (IllegalArgumentException e) {
            log.warn("Failed to create profile: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * Get profile by user ID
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get profile by user ID", description = "Retrieves user profile by user ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER_MANAGER') or #userId == authentication.name")
    public ResponseEntity<ApiResponse<UserProfileRes>> getProfileByUserId(
            @Parameter(description = "User ID") @PathVariable String userId) {

        log.debug("Getting profile for user: {}", userId);

        Optional<UserProfile> profile = userProfileService.findByUserId(userId);
        if (profile.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound("Profile not found for user: " + userId));
        }

        UserProfileRes profileRes = userProfileMapper.toUserProfileRes(profile.get());
        return ResponseEntity.ok(ApiResponse.success(profileRes));
    }

    /**
     * Get profile by profile ID
     */
    @GetMapping("/{profileId}")
    @Operation(summary = "Get profile by ID", description = "Retrieves user profile by profile ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER_MANAGER', 'USER')")
    public ResponseEntity<ApiResponse<UserProfileRes>> getProfileById(
            @Parameter(description = "Profile ID") @PathVariable String profileId) {

        log.debug("Getting profile by ID: {}", profileId);

        Optional<UserProfile> profile = userProfileService.findById(profileId);
        if (profile.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound("Profile not found: " + profileId));
        }

        UserProfileRes profileRes = userProfileMapper.toUserProfileRes(profile.get());
        return ResponseEntity.ok(ApiResponse.success(profileRes));
    }

    /**
     * Update user profile
     */
    @PutMapping("/user/{userId}")
    @Operation(summary = "Update user profile", description = "Updates user profile information")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER_MANAGER') or #userId == authentication.name")
    public ResponseEntity<ApiResponse<UserProfileRes>> updateProfile(
            @Parameter(description = "User ID") @PathVariable String userId,
            @Valid @RequestBody UserProfileUpdateReq request) {

        log.info("Updating profile for user: {}", userId);

        try {
            Optional<UserProfile> existingProfile = userProfileService.findByUserId(userId);
            if (existingProfile.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.notFound("Profile not found for user: " + userId));
            }

            UserProfile profile = existingProfile.get();
            userProfileMapper.updateUserProfileFromDto(request, profile);
            UserProfile updatedProfile = userProfileService.updateProfile(profile);
            UserProfileRes profileRes = userProfileMapper.toUserProfileRes(updatedProfile);

            return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", profileRes));

        } catch (IllegalArgumentException e) {
            log.warn("Failed to update profile for user {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * Delete user profile
     */
    @DeleteMapping("/user/{userId}")
    @Operation(summary = "Delete user profile", description = "Soft deletes a user profile")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deleteProfile(
            @Parameter(description = "User ID") @PathVariable String userId) {

        log.info("Deleting profile for user: {}", userId);

        try {
            userProfileService.deleteProfile(userId, "ADMIN"); // TODO: Get current user from security context
            return ResponseEntity.ok(ApiResponse.success("Profile deleted successfully"));

        } catch (IllegalArgumentException e) {
            log.warn("Failed to delete profile for user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound(e.getMessage()));
        }
    }

    // === Profile Verification ===

    /**
     * Verify user profile
     */
    @PostMapping("/user/{userId}/verify")
    @Operation(summary = "Verify user profile", description = "Marks user profile as verified")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER_MANAGER', 'VERIFIER')")
    public ResponseEntity<ApiResponse<UserProfileRes>> verifyProfile(
            @Parameter(description = "User ID") @PathVariable String userId,
            @RequestParam(defaultValue = "MANUAL") String verificationMethod) {

        log.info("Verifying profile for user: {} with method: {}", userId, verificationMethod);

        try {
            UserProfile verifiedProfile = userProfileService.verifyProfile(userId, verificationMethod, "ADMIN");
            UserProfileRes profileRes = userProfileMapper.toUserProfileRes(verifiedProfile);

            return ResponseEntity.ok(ApiResponse.success("Profile verified successfully", profileRes));

        } catch (IllegalArgumentException e) {
            log.warn("Failed to verify profile for user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound(e.getMessage()));
        }
    }

    /**
     * Unverify user profile
     */
    @PostMapping("/user/{userId}/unverify")
    @Operation(summary = "Unverify user profile", description = "Removes verification status from user profile")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER_MANAGER')")
    public ResponseEntity<ApiResponse<UserProfileRes>> unverifyProfile(
            @Parameter(description = "User ID") @PathVariable String userId) {

        log.info("Unverifying profile for user: {}", userId);

        try {
            UserProfile unverifiedProfile = userProfileService.unverifyProfile(userId, "ADMIN");
            UserProfileRes profileRes = userProfileMapper.toUserProfileRes(unverifiedProfile);

            return ResponseEntity.ok(ApiResponse.success("Profile verification removed", profileRes));

        } catch (IllegalArgumentException e) {
            log.warn("Failed to unverify profile for user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound(e.getMessage()));
        }
    }

    // === Profile Search and Analytics ===

    /**
     * Search profiles by criteria
     */
    @GetMapping("/search")
    @Operation(summary = "Search profiles", description = "Search user profiles with filters and pagination")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER_MANAGER')")
    public ResponseEntity<PagedResponse<UserProfileRes>> searchProfiles(
            @RequestParam(required = false) String nationality,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String occupation,
            @RequestParam(required = false) Boolean verified,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        log.debug("Searching profiles with filters - nationality: {}, country: {}, occupation: {}, verified: {}",
                nationality, country, occupation, verified);

        try {
            Pageable pageable = createPageable(page, size, sortBy, sortDirection);
            Page<UserProfile> profiles = userProfileService.searchProfiles(
                    nationality, country, occupation, verified, pageable);

            List<UserProfileRes> profileResList = userProfileMapper.toUserProfileResList(profiles.getContent());
            PagedResponse<UserProfileRes> response = PagedResponse.of(profileResList, profiles);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error searching profiles", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(PagedResponse.<UserProfileRes>builder()
                            .success(false)
                            .message("Search failed: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Get verified profiles
     */
    @GetMapping("/verified")
    @Operation(summary = "Get verified profiles", description = "Retrieves all verified user profiles")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER_MANAGER')")
    public ResponseEntity<PagedResponse<UserProfileRes>> getVerifiedProfiles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.debug("Getting verified profiles (page: {}, size: {})", page, size);

        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<UserProfile> profiles = userProfileService.findVerifiedProfiles(pageable);

        List<UserProfileRes> profileResList = userProfileMapper.toUserProfileResList(profiles.getContent());
        PagedResponse<UserProfileRes> response = PagedResponse.of(profileResList, profiles);

        return ResponseEntity.ok(response);
    }

    /**
     * Get unverified profiles
     */
    @GetMapping("/unverified")
    @Operation(summary = "Get unverified profiles", description = "Retrieves all unverified user profiles")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER_MANAGER', 'VERIFIER')")
    public ResponseEntity<PagedResponse<UserProfileRes>> getUnverifiedProfiles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.debug("Getting unverified profiles (page: {}, size: {})", page, size);

        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<UserProfile> profiles = userProfileService.findUnverifiedProfiles(pageable);

        List<UserProfileRes> profileResList = userProfileMapper.toUserProfileResList(profiles.getContent());
        PagedResponse<UserProfileRes> response = PagedResponse.of(profileResList, profiles);

        return ResponseEntity.ok(response);
    }

    /**
     * Get profiles by age range
     */
    @GetMapping("/age-range")
    @Operation(summary = "Get profiles by age range", description = "Retrieves profiles within specified age range")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER_MANAGER')")
    public ResponseEntity<PagedResponse<UserProfileRes>> getProfilesByAgeRange(
            @RequestParam int minAge,
            @RequestParam int maxAge,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.debug("Getting profiles with age range: {}-{} (page: {}, size: {})", minAge, maxAge, page, size);

        try {
            Pageable pageable = PageRequest.of(page, Math.min(size, 100));
            Page<UserProfile> profiles = userProfileService.findByAgeRange(minAge, maxAge, pageable);

            List<UserProfileRes> profileResList = userProfileMapper.toUserProfileResList(profiles.getContent());
            PagedResponse<UserProfileRes> response = PagedResponse.of(profileResList, profiles);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(PagedResponse.<UserProfileRes>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    /**
     * Get profiles by nationality
     */
    @GetMapping("/nationality/{nationality}")
    @Operation(summary = "Get profiles by nationality", description = "Retrieves profiles filtered by nationality")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER_MANAGER')")
    public ResponseEntity<PagedResponse<UserProfileRes>> getProfilesByNationality(
            @Parameter(description = "Nationality code") @PathVariable String nationality,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.debug("Getting profiles by nationality: {} (page: {}, size: {})", nationality, page, size);

        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<UserProfile> profiles = userProfileService.findByNationality(nationality, pageable);

        List<UserProfileRes> profileResList = userProfileMapper.toUserProfileResList(profiles.getContent());
        PagedResponse<UserProfileRes> response = PagedResponse.of(profileResList, profiles);

        return ResponseEntity.ok(response);
    }

    // === Profile Statistics ===

    /**
     * Get profile demographics statistics
     */
    @GetMapping("/demographics")
    @Operation(summary = "Get profile demographics", description = "Retrieves demographic statistics for profiles")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER_MANAGER')")
    public ResponseEntity<ApiResponse<Object>> getProfileDemographics() {

        log.debug("Getting profile demographics");

        try {
            Object demographics = userProfileService.getProfileDemographics();
            return ResponseEntity.ok(ApiResponse.success(demographics));

        } catch (Exception e) {
            log.error("Failed to get profile demographics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.internalServerError("Failed to retrieve demographics"));
        }
    }

    /**
     * Get verification statistics
     */
    @GetMapping("/verification-stats")
    @Operation(summary = "Get verification statistics", description = "Retrieves profile verification statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER_MANAGER')")
    public ResponseEntity<ApiResponse<Object>> getVerificationStatistics() {

        log.debug("Getting verification statistics");

        try {
            var statistics = userProfileService.getVerificationStatistics();
            return ResponseEntity.ok(ApiResponse.success(statistics));

        } catch (Exception e) {
            log.error("Failed to get verification statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.internalServerError("Failed to retrieve verification statistics"));
        }
    }

    /**
     * Count profiles by verification status
     */
    @GetMapping("/count/verified")
    @Operation(summary = "Count verified profiles", description = "Returns count of verified profiles")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER_MANAGER')")
    public ResponseEntity<ApiResponse<Long>> countVerifiedProfiles() {

        log.debug("Counting verified profiles");

        long count = userProfileService.countVerifiedProfiles();
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    /**
     * Count profiles by nationality
     */
    @GetMapping("/count/nationality/{nationality}")
    @Operation(summary = "Count profiles by nationality", description = "Returns count of profiles with specific nationality")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER_MANAGER')")
    public ResponseEntity<ApiResponse<Long>> countProfilesByNationality(
            @Parameter(description = "Nationality code") @PathVariable String nationality) {

        log.debug("Counting profiles by nationality: {}", nationality);

        long count = userProfileService.countByNationality(nationality);
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