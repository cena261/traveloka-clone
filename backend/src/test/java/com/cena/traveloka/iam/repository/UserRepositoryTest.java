package com.cena.traveloka.iam.repository;

import com.cena.traveloka.common.enums.Gender;
import com.cena.traveloka.common.enums.Status;
import com.cena.traveloka.iam.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T004: UserRepository CRUD test
 * Tests basic repository operations for User entity.
 *
 * TDD Phase: RED - These tests MUST fail before implementing UserRepository
 *
 * Constitutional Compliance:
 * - Principle IV: Entity Immutability - User entity is READ-ONLY, not modified here
 * - Principle VII: Test-First Development - Tests written before repository implementation
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@ActiveProfiles("test")
@DisplayName("UserRepository Tests")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Clean up before each test
        userRepository.deleteAll();

        // Create test user following existing entity structure
        testUser = User.builder()
                .keycloakId(UUID.randomUUID())
                .username("johndoe")
                .email("john.doe@example.com")
                .phone("+84901234567")
                .firstName("John")
                .lastName("Doe")
                .displayName("John Doe")
                .dateOfBirth(LocalDate.of(1990, 1, 15))
                .gender(Gender.male)
                .nationality("VN")
                .preferredLanguage("vi")
                .preferredCurrency("VND")
                .timezone("Asia/Ho_Chi_Minh")
                .status(Status.pending)
                .emailVerified(false)
                .phoneVerified(false)
                .twoFactorEnabled(false)
                .accountLocked(false)
                .loginCount(0)
                .failedLoginAttempts(0)
                .marketingConsent(false)
                .createdAt(OffsetDateTime.now())
                .createdBy("system")
                .isDeleted(false)
                .build();
    }

    @Test
    @DisplayName("Should save user successfully")
    void shouldSaveUser() {
        // When
        User savedUser = userRepository.save(testUser);

        // Then
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getUsername()).isEqualTo("johndoe");
        assertThat(savedUser.getEmail()).isEqualTo("john.doe@example.com");
        assertThat(savedUser.getStatus()).isEqualTo(Status.pending);
    }

    @Test
    @DisplayName("Should find user by ID")
    void shouldFindUserById() {
        // Given
        User savedUser = userRepository.save(testUser);

        // When
        Optional<User> foundUser = userRepository.findById(savedUser.getId());

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername()).isEqualTo("johndoe");
    }

    @Test
    @DisplayName("Should find user by email")
    void shouldFindUserByEmail() {
        // Given
        userRepository.save(testUser);

        // When
        Optional<User> foundUser = userRepository.findByEmail("john.doe@example.com");

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername()).isEqualTo("johndoe");
    }

    @Test
    @DisplayName("Should find user by username")
    void shouldFindUserByUsername() {
        // Given
        userRepository.save(testUser);

        // When
        Optional<User> foundUser = userRepository.findByUsername("johndoe");

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo("john.doe@example.com");
    }

    @Test
    @DisplayName("Should find user by Keycloak ID")
    void shouldFindUserByKeycloakId() {
        // Given
        User savedUser = userRepository.save(testUser);

        // When
        Optional<User> foundUser = userRepository.findByKeycloakId(savedUser.getKeycloakId());

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername()).isEqualTo("johndoe");
    }

    @Test
    @DisplayName("Should check if email exists")
    void shouldCheckEmailExists() {
        // Given
        userRepository.save(testUser);

        // When
        boolean exists = userRepository.existsByEmail("john.doe@example.com");
        boolean notExists = userRepository.existsByEmail("nonexistent@example.com");

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("Should check if username exists")
    void shouldCheckUsernameExists() {
        // Given
        userRepository.save(testUser);

        // When
        boolean exists = userRepository.existsByUsername("johndoe");
        boolean notExists = userRepository.existsByUsername("nonexistent");

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("Should find users by status")
    void shouldFindUsersByStatus() {
        // Given
        userRepository.save(testUser);

        User activeUser = User.builder()
                .keycloakId(UUID.randomUUID())
                .username("janedoe")
                .email("jane.doe@example.com")
                .firstName("Jane")
                .lastName("Doe")
                .status(Status.active)
                .emailVerified(true)
                .createdAt(OffsetDateTime.now())
                .createdBy("system")
                .isDeleted(false)
                .build();
        userRepository.save(activeUser);

        // When
        List<User> pendingUsers = userRepository.findByStatus(Status.pending);
        List<User> activeUsers = userRepository.findByStatus(Status.active);

        // Then
        assertThat(pendingUsers).hasSize(1);
        assertThat(pendingUsers.get(0).getUsername()).isEqualTo("johndoe");
        assertThat(activeUsers).hasSize(1);
        assertThat(activeUsers.get(0).getUsername()).isEqualTo("janedoe");
    }

    @Test
    @DisplayName("Should find locked users")
    void shouldFindLockedUsers() {
        // Given
        testUser.setAccountLocked(true);
        testUser.setLockReason("Security violation");
        testUser.setLockedUntil(OffsetDateTime.now().plusHours(1));
        userRepository.save(testUser);

        // When
        List<User> lockedUsers = userRepository.findByAccountLockedTrue();

        // Then
        assertThat(lockedUsers).hasSize(1);
        assertThat(lockedUsers.get(0).getLockReason()).isEqualTo("Security violation");
    }

    @Test
    @DisplayName("Should update user details")
    void shouldUpdateUser() {
        // Given
        User savedUser = userRepository.save(testUser);

        // When
        savedUser.setEmailVerified(true);
        savedUser.setStatus(Status.active);
        savedUser.setUpdatedAt(OffsetDateTime.now());
        savedUser.setUpdatedBy("admin");
        User updatedUser = userRepository.save(savedUser);

        // Then
        assertThat(updatedUser.getEmailVerified()).isTrue();
        assertThat(updatedUser.getStatus()).isEqualTo(Status.active);
        assertThat(updatedUser.getUpdatedBy()).isEqualTo("admin");
    }

    @Test
    @DisplayName("Should soft delete user")
    void shouldSoftDeleteUser() {
        // Given
        User savedUser = userRepository.save(testUser);

        // When
        savedUser.setIsDeleted(true);
        savedUser.setDeletedAt(OffsetDateTime.now());
        savedUser.setDeletedBy("admin");
        userRepository.save(savedUser);

        // Then
        Optional<User> foundUser = userRepository.findById(savedUser.getId());
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getIsDeleted()).isTrue();
        assertThat(foundUser.get().getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should find users with failed login attempts above threshold")
    void shouldFindUsersWithExcessiveFailedLogins() {
        // Given
        testUser.setFailedLoginAttempts(5);
        userRepository.save(testUser);

        // When
        List<User> usersWithFailedLogins = userRepository.findByFailedLoginAttemptsGreaterThanEqual(5);

        // Then
        assertThat(usersWithFailedLogins).hasSize(1);
        assertThat(usersWithFailedLogins.get(0).getFailedLoginAttempts()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should delete user permanently")
    void shouldDeleteUser() {
        // Given
        User savedUser = userRepository.save(testUser);
        UUID userId = savedUser.getId();

        // When
        userRepository.deleteById(userId);

        // Then
        Optional<User> deletedUser = userRepository.findById(userId);
        assertThat(deletedUser).isEmpty();
    }

    @Test
    @DisplayName("Should find all users")
    void shouldFindAllUsers() {
        // Given
        userRepository.save(testUser);

        User anotherUser = User.builder()
                .keycloakId(UUID.randomUUID())
                .username("janedoe")
                .email("jane.doe@example.com")
                .firstName("Jane")
                .lastName("Doe")
                .status(Status.active)
                .createdAt(OffsetDateTime.now())
                .createdBy("system")
                .isDeleted(false)
                .build();
        userRepository.save(anotherUser);

        // When
        List<User> allUsers = userRepository.findAll();

        // Then
        assertThat(allUsers).hasSize(2);
    }
}
