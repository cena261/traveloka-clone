package com.cena.traveloka.iam.service;

import com.cena.traveloka.common.enums.Gender;
import com.cena.traveloka.common.enums.Status;
import com.cena.traveloka.iam.dto.request.UpdateProfileRequest;
import com.cena.traveloka.iam.dto.response.UserDetailDto;
import com.cena.traveloka.iam.dto.response.UserDto;
import com.cena.traveloka.iam.entity.User;
import com.cena.traveloka.iam.entity.UserProfile;
import com.cena.traveloka.iam.mapper.UserMapper;
import com.cena.traveloka.iam.repository.UserProfileRepository;
import com.cena.traveloka.iam.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * T011: UserServiceTest
 * Service layer tests for user CRUD operations.
 *
 * TDD Phase: RED - These tests MUST fail before implementing UserService
 *
 * Constitutional Compliance:
 * - Principle VII: Test-First Development - Tests written before service implementation
 * - Tests FR-018: Vietnamese phone number validation
 * - Tests FR-019: Extended user profiles
 * - Tests FR-020: User search and filtering
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private UserProfile testProfile;
    private UpdateProfileRequest updateRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .username("johndoe")
                .email("john.doe@example.com")
                .firstName("John")
                .lastName("Doe")
                .phone("+84901234567")
                .status(Status.active)
                .emailVerified(true)
                .createdAt(OffsetDateTime.now())
                .build();

        testProfile = UserProfile.builder()
                .userId(testUser.getId())
                .user(testUser)
                .nickname("Johnny")
                .occupation("Software Engineer")
                .company("Tech Corp")
                .build();

        updateRequest = UpdateProfileRequest.builder()
                .firstName("John")
                .lastName("Smith")
                .phone("+84987654321")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .gender(Gender.male)
                .nationality("VN")
                .preferredLanguage("vi")
                .build();
    }

    @Test
    @DisplayName("Should find user by ID")
    void shouldFindUserById() {
        // Given
        UUID userId = testUser.getId();
        UserDto expectedDto = UserDto.builder()
                .id(userId)
                .email(testUser.getEmail())
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userMapper.toDto(testUser)).thenReturn(expectedDto);

        // When
        UserDto result = userService.findById(userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(userId);
        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void shouldThrowExceptionWhenUserNotFound() {
        // Given
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> userService.findById(userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("Should get user detail with profile (FR-019)")
    void shouldGetUserDetailWithProfile() {
        // Given
        UUID userId = testUser.getId();
        testUser.setProfile(testProfile);
        UserDetailDto expectedDto = UserDetailDto.builder()
                .id(userId)
                .email(testUser.getEmail())
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userMapper.toDetailDto(testUser)).thenReturn(expectedDto);

        // When
        UserDetailDto result = userService.getUserDetail(userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(userId);
        verify(userMapper).toDetailDto(testUser);
    }

    @Test
    @DisplayName("Should update user profile (FR-018, FR-019)")
    void shouldUpdateUserProfile() {
        // Given
        UUID userId = testUser.getId();
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        doNothing().when(userMapper).updateFromRequest(updateRequest, testUser);

        // When
        userService.updateProfile(userId, updateRequest);

        // Then
        verify(userMapper).updateFromRequest(updateRequest, testUser);
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Should validate Vietnamese phone number format (FR-018)")
    void shouldValidateVietnamesePhoneNumber() {
        // Given
        UUID userId = testUser.getId();
        updateRequest.setPhone("+84901234567"); // Valid Vietnamese format

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        doNothing().when(userMapper).updateFromRequest(updateRequest, testUser);

        // When
        userService.updateProfile(userId, updateRequest);

        // Then
        verify(userRepository).save(argThat(user ->
                user.getPhone() != null &&
                user.getPhone().startsWith("+84")
        ));
    }

    @Test
    @DisplayName("Should throw exception for invalid phone format")
    void shouldThrowExceptionForInvalidPhoneFormat() {
        // Given
        UUID userId = testUser.getId();
        updateRequest.setPhone("0901234567"); // Invalid format (missing +84)

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // When/Then
        assertThatThrownBy(() -> userService.updateProfile(userId, updateRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid Vietnamese phone number format");
    }

    @Test
    @DisplayName("Should search users by email (FR-020)")
    void shouldSearchUsersByEmail() {
        // Given
        String searchEmail = "john";
        Pageable pageable = PageRequest.of(0, 10);
        List<User> users = Arrays.asList(testUser);
        Page<User> userPage = new PageImpl<>(users, pageable, 1);

        when(userRepository.findByEmailContainingIgnoreCase(searchEmail, pageable)).thenReturn(userPage);
        when(userMapper.toDto(any(User.class))).thenReturn(UserDto.builder().build());

        // When
        Page<UserDto> result = userService.searchByEmail(searchEmail, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(userRepository).findByEmailContainingIgnoreCase(searchEmail, pageable);
    }

    @Test
    @DisplayName("Should search users by username (FR-020)")
    void shouldSearchUsersByUsername() {
        // Given
        String searchUsername = "john";
        Pageable pageable = PageRequest.of(0, 10);
        List<User> users = Arrays.asList(testUser);
        Page<User> userPage = new PageImpl<>(users, pageable, 1);

        when(userRepository.findByUsernameContainingIgnoreCase(searchUsername, pageable)).thenReturn(userPage);
        when(userMapper.toDto(any(User.class))).thenReturn(UserDto.builder().build());

        // When
        Page<UserDto> result = userService.searchByUsername(searchUsername, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(userRepository).findByUsernameContainingIgnoreCase(searchUsername, pageable);
    }

    @Test
    @DisplayName("Should filter users by status (FR-020)")
    void shouldFilterUsersByStatus() {
        // Given
        Status status = Status.active;
        Pageable pageable = PageRequest.of(0, 10);
        List<User> users = Arrays.asList(testUser);
        Page<User> userPage = new PageImpl<>(users, pageable, 1);

        when(userRepository.findByStatus(status, pageable)).thenReturn(userPage);
        when(userMapper.toDto(any(User.class))).thenReturn(UserDto.builder().build());

        // When
        Page<UserDto> result = userService.findByStatus(status, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(userRepository).findByStatus(status, pageable);
    }

    @Test
    @DisplayName("Should get all users with pagination")
    void shouldGetAllUsersWithPagination() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);
        List<User> users = Arrays.asList(testUser);
        Page<User> userPage = new PageImpl<>(users, pageable, 1);

        when(userRepository.findAll(pageable)).thenReturn(userPage);
        when(userMapper.toDto(any(User.class))).thenReturn(UserDto.builder().build());

        // When
        Page<UserDto> result = userService.findAll(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(userRepository).findAll(pageable);
    }

    @Test
    @DisplayName("Should activate user account")
    void shouldActivateUserAccount() {
        // Given
        UUID userId = testUser.getId();
        testUser.setStatus(Status.pending);

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        userService.activateUser(userId);

        // Then
        verify(userRepository).save(argThat(user ->
                user.getStatus() == Status.active
        ));
    }

    @Test
    @DisplayName("Should suspend user account")
    void shouldSuspendUserAccount() {
        // Given
        UUID userId = testUser.getId();
        String reason = "Violation of terms";

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        userService.suspendUser(userId, reason);

        // Then
        verify(userRepository).save(argThat(user ->
                user.getStatus() == Status.suspended
        ));
    }

    @Test
    @DisplayName("Should verify user email")
    void shouldVerifyUserEmail() {
        // Given
        UUID userId = testUser.getId();
        testUser.setEmailVerified(false);

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        userService.verifyEmail(userId);

        // Then
        verify(userRepository).save(argThat(user ->
                user.getEmailVerified()
        ));
    }

    @Test
    @DisplayName("Should delete user (soft delete)")
    void shouldSoftDeleteUser() {
        // Given
        UUID userId = testUser.getId();
        testUser.setIsDeleted(false);

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        userService.deleteUser(userId);

        // Then
        verify(userRepository).save(argThat(user ->
                user.getIsDeleted() &&
                user.getDeletedAt() != null
        ));
    }

    @Test
    @DisplayName("Should find user by email")
    void shouldFindUserByEmail() {
        // Given
        String email = testUser.getEmail();
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
        when(userMapper.toDto(testUser)).thenReturn(UserDto.builder().email(email).build());

        // When
        Optional<UserDto> result = userService.findByEmail(email);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo(email);
    }

    @Test
    @DisplayName("Should find user by username")
    void shouldFindUserByUsername() {
        // Given
        String username = testUser.getUsername();
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));
        when(userMapper.toDto(testUser)).thenReturn(UserDto.builder().username(username).build());

        // When
        Optional<UserDto> result = userService.findByUsername(username);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo(username);
    }

    @Test
    @DisplayName("Should check if email exists")
    void shouldCheckIfEmailExists() {
        // Given
        String email = testUser.getEmail();
        when(userRepository.existsByEmail(email)).thenReturn(true);

        // When
        boolean exists = userService.existsByEmail(email);

        // Then
        assertThat(exists).isTrue();
        verify(userRepository).existsByEmail(email);
    }

    @Test
    @DisplayName("Should check if username exists")
    void shouldCheckIfUsernameExists() {
        // Given
        String username = testUser.getUsername();
        when(userRepository.existsByUsername(username)).thenReturn(true);

        // When
        boolean exists = userService.existsByUsername(username);

        // Then
        assertThat(exists).isTrue();
        verify(userRepository).existsByUsername(username);
    }
}
