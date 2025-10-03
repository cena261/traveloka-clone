package com.cena.traveloka.iam.repository;

import com.cena.traveloka.common.enums.Status;
import com.cena.traveloka.iam.entity.LoginHistory;
import com.cena.traveloka.iam.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T007: LoginHistoryRepository test with audit queries
 * Tests login tracking and audit log queries (FR-007, FR-015).
 *
 * TDD Phase: RED - These tests MUST fail before implementing LoginHistoryRepository
 *
 * Constitutional Compliance:
 * - Principle IV: Entity Immutability - LoginHistory entity is READ-ONLY, not modified here
 * - Principle VII: Test-First Development - Tests written before repository implementation
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@ActiveProfiles("test")
@DisplayName("LoginHistoryRepository Tests")
class LoginHistoryRepositoryTest {

    @Autowired
    private LoginHistoryRepository loginHistoryRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User testUser;
    private LoginHistory successfulLogin;
    private LoginHistory failedLogin;

    @BeforeEach
    void setUp() {
        // Clean up before each test
        loginHistoryRepository.deleteAll();

        // Create test user
        testUser = User.builder()
                .keycloakId(UUID.randomUUID())
                .username("johndoe")
                .email("john.doe@example.com")
                .firstName("John")
                .lastName("Doe")
                .status(Status.active)
                .emailVerified(true)
                .createdAt(OffsetDateTime.now())
                .createdBy("system")
                .isDeleted(false)
                .build();
        testUser = entityManager.persistAndFlush(testUser);

        // Create successful login history
        successfulLogin = LoginHistory.builder()
                .user(testUser)
                .username("johndoe")
                .email("john.doe@example.com")
                .loginType("password")
                .provider("local")
                .success(true)
                .ipAddress("192.168.1.100")
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .deviceType("desktop")
                .deviceId("device_123")
                .browser("Chrome 120.0")
                .os("Windows 11")
                .locationCountry("Vietnam")
                .locationCity("Ho Chi Minh City")
                .riskScore(10)
                .isSuspicious(false)
                .required2fa(false)
                .completed2fa(false)
                .attemptedAt(OffsetDateTime.now())
                .build();

        // Create failed login history
        failedLogin = LoginHistory.builder()
                .user(testUser)
                .username("johndoe")
                .email("john.doe@example.com")
                .loginType("password")
                .provider("local")
                .success(false)
                .failureReason("Invalid password")
                .ipAddress("192.168.1.100")
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .deviceType("desktop")
                .riskScore(50)
                .isSuspicious(true)
                .attemptedAt(OffsetDateTime.now().minusMinutes(10))
                .build();
    }

    @Test
    @DisplayName("Should save login history successfully")
    void shouldSaveLoginHistory() {
        // When
        LoginHistory savedHistory = loginHistoryRepository.save(successfulLogin);

        // Then
        assertThat(savedHistory).isNotNull();
        assertThat(savedHistory.getId()).isNotNull();
        assertThat(savedHistory.getSuccess()).isTrue();
        assertThat(savedHistory.getUser().getId()).isEqualTo(testUser.getId());
    }

    @Test
    @DisplayName("Should find login history by user ID")
    void shouldFindLoginHistoryByUserId() {
        // Given
        loginHistoryRepository.save(successfulLogin);
        loginHistoryRepository.save(failedLogin);

        // When
        List<LoginHistory> userHistory = loginHistoryRepository.findByUserId(testUser.getId());

        // Then
        assertThat(userHistory).hasSize(2);
    }

    @Test
    @DisplayName("Should find successful logins")
    void shouldFindSuccessfulLogins() {
        // Given
        loginHistoryRepository.save(successfulLogin);
        loginHistoryRepository.save(failedLogin);

        // When
        List<LoginHistory> successfulLogins = loginHistoryRepository.findBySuccess(true);

        // Then
        assertThat(successfulLogins).hasSize(1);
        assertThat(successfulLogins.get(0).getFailureReason()).isNull();
    }

    @Test
    @DisplayName("Should find failed logins")
    void shouldFindFailedLogins() {
        // Given
        loginHistoryRepository.save(successfulLogin);
        loginHistoryRepository.save(failedLogin);

        // When
        List<LoginHistory> failedLogins = loginHistoryRepository.findBySuccess(false);

        // Then
        assertThat(failedLogins).hasSize(1);
        assertThat(failedLogins.get(0).getFailureReason()).isEqualTo("Invalid password");
    }

    @Test
    @DisplayName("Should find failed login attempts by user ID (FR-008: Account lockout)")
    void shouldFindFailedLoginAttemptsByUserId() {
        // Given
        loginHistoryRepository.save(successfulLogin);
        loginHistoryRepository.save(failedLogin);

        LoginHistory anotherFailure = LoginHistory.builder()
                .user(testUser)
                .username("johndoe")
                .email("john.doe@example.com")
                .loginType("password")
                .provider("local")
                .success(false)
                .failureReason("Invalid password")
                .ipAddress("192.168.1.100")
                .attemptedAt(OffsetDateTime.now().minusMinutes(5))
                .build();
        loginHistoryRepository.save(anotherFailure);

        // When
        List<LoginHistory> failedAttempts = loginHistoryRepository.findByUserIdAndSuccessFalse(testUser.getId());

        // Then
        assertThat(failedAttempts).hasSize(2);
    }

    @Test
    @DisplayName("Should count recent failed login attempts (FR-008)")
    void shouldCountRecentFailedAttempts() {
        // Given
        OffsetDateTime thirtyMinutesAgo = OffsetDateTime.now().minusMinutes(30);

        // Create recent failed attempts
        for (int i = 0; i < 3; i++) {
            LoginHistory recentFailure = LoginHistory.builder()
                    .user(testUser)
                    .username("johndoe")
                    .email("john.doe@example.com")
                    .success(false)
                    .failureReason("Invalid password")
                    .attemptedAt(OffsetDateTime.now().minusMinutes(i * 5))
                    .build();
            loginHistoryRepository.save(recentFailure);
        }

        // Create old failed attempt (should not be counted)
        LoginHistory oldFailure = LoginHistory.builder()
                .user(testUser)
                .username("johndoe")
                .email("john.doe@example.com")
                .success(false)
                .failureReason("Invalid password")
                .attemptedAt(OffsetDateTime.now().minusMinutes(45))
                .build();
        loginHistoryRepository.save(oldFailure);

        // When
        long recentFailedCount = loginHistoryRepository.countByUserIdAndSuccessFalseAndAttemptedAtAfter(
                testUser.getId(), thirtyMinutesAgo);

        // Then
        assertThat(recentFailedCount).isEqualTo(3);
    }

    @Test
    @DisplayName("Should find login history by email")
    void shouldFindLoginHistoryByEmail() {
        // Given
        loginHistoryRepository.save(successfulLogin);
        loginHistoryRepository.save(failedLogin);

        // When
        List<LoginHistory> emailHistory = loginHistoryRepository.findByEmail("john.doe@example.com");

        // Then
        assertThat(emailHistory).hasSize(2);
    }

    @Test
    @DisplayName("Should find login history by username")
    void shouldFindLoginHistoryByUsername() {
        // Given
        loginHistoryRepository.save(successfulLogin);
        loginHistoryRepository.save(failedLogin);

        // When
        List<LoginHistory> usernameHistory = loginHistoryRepository.findByUsername("johndoe");

        // Then
        assertThat(usernameHistory).hasSize(2);
    }

    @Test
    @DisplayName("Should find login history by IP address")
    void shouldFindLoginHistoryByIpAddress() {
        // Given
        loginHistoryRepository.save(successfulLogin);
        loginHistoryRepository.save(failedLogin);

        LoginHistory differentIp = LoginHistory.builder()
                .user(testUser)
                .username("johndoe")
                .email("john.doe@example.com")
                .success(true)
                .ipAddress("10.0.0.1")
                .attemptedAt(OffsetDateTime.now())
                .build();
        loginHistoryRepository.save(differentIp);

        // When
        List<LoginHistory> sameIpHistory = loginHistoryRepository.findByIpAddress("192.168.1.100");

        // Then
        assertThat(sameIpHistory).hasSize(2);
    }

    @Test
    @DisplayName("Should find suspicious login attempts")
    void shouldFindSuspiciousLogins() {
        // Given
        loginHistoryRepository.save(successfulLogin);
        loginHistoryRepository.save(failedLogin);

        // When
        List<LoginHistory> suspiciousLogins = loginHistoryRepository.findByIsSuspiciousTrue();

        // Then
        assertThat(suspiciousLogins).hasSize(1);
        assertThat(suspiciousLogins.get(0).getRiskScore()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should find logins by provider (OAuth tracking)")
    void shouldFindLoginsByProvider() {
        // Given
        loginHistoryRepository.save(successfulLogin);

        LoginHistory googleLogin = LoginHistory.builder()
                .user(testUser)
                .username("johndoe")
                .email("john.doe@example.com")
                .loginType("oauth")
                .provider("google")
                .success(true)
                .attemptedAt(OffsetDateTime.now())
                .build();
        loginHistoryRepository.save(googleLogin);

        // When
        List<LoginHistory> localLogins = loginHistoryRepository.findByProvider("local");
        List<LoginHistory> googleLogins = loginHistoryRepository.findByProvider("google");

        // Then
        assertThat(localLogins).hasSize(1);
        assertThat(googleLogins).hasSize(1);
    }

    @Test
    @DisplayName("Should find logins within date range")
    void shouldFindLoginsWithinDateRange() {
        // Given
        OffsetDateTime startDate = OffsetDateTime.now().minusHours(1);
        OffsetDateTime endDate = OffsetDateTime.now().plusHours(1);

        loginHistoryRepository.save(successfulLogin);
        loginHistoryRepository.save(failedLogin);

        // Create old login (outside range)
        LoginHistory oldLogin = LoginHistory.builder()
                .user(testUser)
                .username("johndoe")
                .email("john.doe@example.com")
                .success(true)
                .attemptedAt(OffsetDateTime.now().minusHours(2))
                .build();
        loginHistoryRepository.save(oldLogin);

        // When
        List<LoginHistory> historyInRange = loginHistoryRepository.findByAttemptedAtBetween(startDate, endDate);

        // Then
        assertThat(historyInRange).hasSize(2);
    }

    @Test
    @DisplayName("Should find recent login history ordered by attempted date")
    void shouldFindRecentLoginHistory() {
        // Given
        loginHistoryRepository.save(successfulLogin);
        loginHistoryRepository.save(failedLogin);

        LoginHistory mostRecentLogin = LoginHistory.builder()
                .user(testUser)
                .username("johndoe")
                .email("john.doe@example.com")
                .success(true)
                .attemptedAt(OffsetDateTime.now().plusMinutes(5))
                .build();
        loginHistoryRepository.save(mostRecentLogin);

        // When
        List<LoginHistory> recentHistory = loginHistoryRepository.findByUserIdOrderByAttemptedAtDesc(
                testUser.getId(), PageRequest.of(0, 10));

        // Then
        assertThat(recentHistory).hasSize(3);
        assertThat(recentHistory.get(0).getAttemptedAt()).isAfter(recentHistory.get(1).getAttemptedAt());
        assertThat(recentHistory.get(1).getAttemptedAt()).isAfter(recentHistory.get(2).getAttemptedAt());
    }

    @Test
    @DisplayName("Should count total login attempts by user")
    void shouldCountTotalLoginAttemptsByUser() {
        // Given
        loginHistoryRepository.save(successfulLogin);
        loginHistoryRepository.save(failedLogin);

        // When
        long totalAttempts = loginHistoryRepository.countByUserId(testUser.getId());

        // Then
        assertThat(totalAttempts).isEqualTo(2);
    }

    @Test
    @DisplayName("Should find logins requiring 2FA")
    void shouldFindLoginsRequiring2FA() {
        // Given
        successfulLogin.setRequired2fa(true);
        successfulLogin.setCompleted2fa(true);
        loginHistoryRepository.save(successfulLogin);

        // When
        List<LoginHistory> logins2FA = loginHistoryRepository.findByRequired2faTrue();

        // Then
        assertThat(logins2FA).hasSize(1);
        assertThat(logins2FA.get(0).getCompleted2fa()).isTrue();
    }
}
