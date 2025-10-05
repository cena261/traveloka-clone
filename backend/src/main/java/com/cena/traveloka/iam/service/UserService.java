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

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserMapper userMapper;

    private static final Pattern VIETNAMESE_PHONE_PATTERN = Pattern.compile("^\\+84[0-9]{9,10}$");

    @Transactional(readOnly = true)
    public UserDto findById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
        return userMapper.toDto(user);
    }

    @Transactional(readOnly = true)
    public UserDetailDto getUserDetail(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
        return userMapper.toDetailDto(user);
    }

    public void updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

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

    @Transactional(readOnly = true)
    public Page<UserDto> searchByEmail(String email, Pageable pageable) {
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

    @Transactional(readOnly = true)
    public Page<UserDto> searchByUsername(String username, Pageable pageable) {
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

    @Transactional(readOnly = true)
    public Page<UserDto> findByStatus(Status status, Pageable pageable) {
        List<User> users = userRepository.findByStatus(status);

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), users.size());
        List<User> pageContent = users.subList(start, end);

        return new org.springframework.data.domain.PageImpl<>(
                pageContent.stream().map(userMapper::toDto).toList(),
                pageable,
                users.size()
        );
    }

    @Transactional(readOnly = true)
    public Page<UserDto> findAll(Pageable pageable) {
        Page<User> users = userRepository.findAll(pageable);
        return users.map(userMapper::toDto);
    }

    public void activateUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        user.setStatus(Status.active);
        user.setUpdatedAt(OffsetDateTime.now());
        userRepository.save(user);

        log.info("User activated: {}", userId);
    }

    public void suspendUser(UUID userId, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        user.setStatus(Status.suspended);
        user.setUpdatedAt(OffsetDateTime.now());
        userRepository.save(user);

        log.info("User suspended: {} - Reason: {}", userId, reason);
    }

    public void verifyEmail(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        user.setEmailVerified(true);
        user.setUpdatedAt(OffsetDateTime.now());
        userRepository.save(user);

        log.info("Email verified for user: {}", userId);
    }

    public void deleteUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        user.setIsDeleted(true);
        user.setDeletedAt(OffsetDateTime.now());
        user.setUpdatedAt(OffsetDateTime.now());
        userRepository.save(user);

        log.info("User soft deleted: {}", userId);
    }

    @Transactional(readOnly = true)
    public Optional<UserDto> findByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(userMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<UserDto> findByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(userMapper::toDto);
    }

    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Transactional(readOnly = true)
    public UserDetailDto getCurrentUser(String token) {
        throw new UnsupportedOperationException("getCurrentUser not yet implemented - requires JWT integration");
    }

    public UserDetailDto updateCurrentUser(UpdateProfileRequest request, String token) {
        throw new UnsupportedOperationException("updateCurrentUser not yet implemented - requires JWT integration");
    }

    @Transactional(readOnly = true)
    public com.cena.traveloka.common.dto.PageResponse<UserDto> getAllUsers(org.springframework.data.domain.Pageable pageable) {
        Page<User> usersPage = userRepository.findAll(pageable);
        List<UserDto> userDtos = usersPage.getContent().stream()
                .map(userMapper::toDto)
                .collect(java.util.stream.Collectors.toList());

        return com.cena.traveloka.common.dto.PageResponse.<UserDto>builder()
                .content(userDtos)
                .totalElements(usersPage.getTotalElements())
                .totalPages(usersPage.getTotalPages())
                .size(usersPage.getSize())
                .number(usersPage.getNumber())
                .first(usersPage.isFirst())
                .last(usersPage.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public UserDetailDto getUserById(UUID userId) {
        return getUserDetail(userId);
    }

    public void lockUser(UUID userId, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        user.setAccountLocked(true);
        user.setLockReason(reason != null ? reason : "Locked by admin");
        user.setLockedUntil(null); // Indefinite lock (admin must unlock)
        user.setUpdatedAt(java.time.OffsetDateTime.now());
        userRepository.save(user);

        log.info("User locked by admin: {} - Reason: {}", userId, reason);
    }

    public void unlockUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        user.setAccountLocked(false);
        user.setLockReason(null);
        user.setLockedUntil(null);
        user.setFailedLoginAttempts(0);
        user.setUpdatedAt(java.time.OffsetDateTime.now());
        userRepository.save(user);

        log.info("User unlocked by admin: {}", userId);
    }
}
