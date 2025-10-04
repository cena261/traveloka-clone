package com.cena.traveloka.iam.service;

import com.cena.traveloka.common.enums.Status;
import com.cena.traveloka.iam.dto.request.UpdateProfileRequest;
import com.cena.traveloka.iam.dto.response.UserDetailDto;
import com.cena.traveloka.iam.dto.response.UserDto;
import com.cena.traveloka.iam.entity.User;
import com.cena.traveloka.iam.mapper.UserMapper;
import com.cena.traveloka.iam.repository.UserProfileRepository;
import com.cena.traveloka.iam.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * T052: UserService
 * Service for user CRUD operations.
 *
 * Constitutional Compliance:
 * - FR-018: Vietnamese phone number validation
 * - FR-019: Extended user profiles
 * - FR-020: User search and filtering
 * - Principle III: Layered Architecture - Business logic in service layer
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserMapper userMapper;

    /**
     * Vietnamese phone number pattern: +84xxxxxxxxx (FR-018)
     */
    private static final Pattern VIETNAMESE_PHONE_PATTERN = Pattern.compile("^\\+84[0-9]{9,10}$");

    /**
     * Find user by ID.
     *
     * @param userId User ID
     * @return UserDto
     */
    @Transactional(readOnly = true)
    public UserDto findById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
        return userMapper.toDto(user);
    }

    /**
     * Get user detail with profile (FR-019).
     *
     * @param userId User ID
     * @return UserDetailDto with profile information
     */
    @Transactional(readOnly = true)
    public UserDetailDto getUserDetail(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
        return userMapper.toDetailDto(user);
    }

    /**
     * Update user profile (FR-018, FR-019).
     *
     * @param userId User ID
     * @param request Update profile request
     */
    public void updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        // Validate Vietnamese phone number format (FR-018)
        if (request.getPhone() != null && !request.getPhone().isEmpty()) {
            if (!VIETNAMESE_PHONE_PATTERN.matcher(request.getPhone()).matches()) {
                throw new RuntimeException("Invalid Vietnamese phone number format. Must be +84xxxxxxxxx");
            }
        }

        userMapper.updateFromRequest(request, user);
        user.setUpdatedAt(OffsetDateTime.now());
        userRepository.save(user);

        log.info("Profile updated for user: {}", userId);
    }

    /**
     * Search users by email (FR-020).
     *
     * @param email Email search term
     * @param pageable Pagination
     * @return Page of UserDto
     */
    @Transactional(readOnly = true)
    public Page<UserDto> searchByEmail(String email, Pageable pageable) {
        // Simple implementation: get all users and filter by email containing search term
        Page<User> users = userRepository.findAll(pageable);
        List<User> filtered = users.stream()
                .filter(u -> u.getEmail() != null && u.getEmail().toLowerCase().contains(email.toLowerCase()))
                .toList();

        return new org.springframework.data.domain.PageImpl<>(
                filtered.stream().map(userMapper::toDto).toList(),
                pageable,
                filtered.size()
        );
    }

    /**
     * Search users by username (FR-020).
     *
     * @param username Username search term
     * @param pageable Pagination
     * @return Page of UserDto
     */
    @Transactional(readOnly = true)
    public Page<UserDto> searchByUsername(String username, Pageable pageable) {
        // Simple implementation: get all users and filter by username containing search term
        Page<User> users = userRepository.findAll(pageable);
        List<User> filtered = users.stream()
                .filter(u -> u.getUsername() != null && u.getUsername().toLowerCase().contains(username.toLowerCase()))
                .toList();

        return new org.springframework.data.domain.PageImpl<>(
                filtered.stream().map(userMapper::toDto).toList(),
                pageable,
                filtered.size()
        );
    }

    /**
     * Filter users by status (FR-020).
     *
     * @param status User status
     * @param pageable Pagination
     * @return Page of UserDto
     */
    @Transactional(readOnly = true)
    public Page<UserDto> findByStatus(Status status, Pageable pageable) {
        // Use non-paginated method and convert to Page
        List<User> users = userRepository.findByStatus(status);

        // Apply pagination manually
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), users.size());
        List<User> pageContent = users.subList(start, end);

        return new org.springframework.data.domain.PageImpl<>(
                pageContent.stream().map(userMapper::toDto).toList(),
                pageable,
                users.size()
        );
    }

    /**
     * Get all users with pagination.
     *
     * @param pageable Pagination
     * @return Page of UserDto
     */
    @Transactional(readOnly = true)
    public Page<UserDto> findAll(Pageable pageable) {
        Page<User> users = userRepository.findAll(pageable);
        return users.map(userMapper::toDto);
    }

    /**
     * Activate user account.
     *
     * @param userId User ID
     */
    public void activateUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        user.setStatus(Status.active);
        user.setUpdatedAt(OffsetDateTime.now());
        userRepository.save(user);

        log.info("User activated: {}", userId);
    }

    /**
     * Suspend user account.
     *
     * @param userId User ID
     * @param reason Suspension reason
     */
    public void suspendUser(UUID userId, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        user.setStatus(Status.suspended);
        user.setUpdatedAt(OffsetDateTime.now());
        userRepository.save(user);

        log.info("User suspended: {} - Reason: {}", userId, reason);
    }

    /**
     * Verify user email.
     *
     * @param userId User ID
     */
    public void verifyEmail(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        user.setEmailVerified(true);
        user.setUpdatedAt(OffsetDateTime.now());
        userRepository.save(user);

        log.info("Email verified for user: {}", userId);
    }

    /**
     * Soft delete user.
     *
     * @param userId User ID
     */
    public void deleteUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        user.setIsDeleted(true);
        user.setDeletedAt(OffsetDateTime.now());
        user.setUpdatedAt(OffsetDateTime.now());
        userRepository.save(user);

        log.info("User soft deleted: {}", userId);
    }

    /**
     * Find user by email.
     *
     * @param email Email address
     * @return Optional UserDto
     */
    @Transactional(readOnly = true)
    public Optional<UserDto> findByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(userMapper::toDto);
    }

    /**
     * Find user by username.
     *
     * @param username Username
     * @return Optional UserDto
     */
    @Transactional(readOnly = true)
    public Optional<UserDto> findByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(userMapper::toDto);
    }

    /**
     * Check if email exists.
     *
     * @param email Email address
     * @return true if email exists
     */
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * Check if username exists.
     *
     * @param username Username
     * @return true if username exists
     */
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
}
