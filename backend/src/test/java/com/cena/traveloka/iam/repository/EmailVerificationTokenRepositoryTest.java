package com.cena.traveloka.iam.repository;

import com.cena.traveloka.common.enums.Status;
import com.cena.traveloka.iam.entity.EmailVerificationToken;
import com.cena.traveloka.iam.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T009: EmailVerificationTokenRepository test
 * Tests email verification token management (FR-010).
 *
 * TDD Phase: RED - These tests MUST fail before implementing EmailVerificationTokenRepository
 *
 * Constitutional Compliance:
 * - Principle IV: Entity Immutability - EmailVerificationToken entity is READ-ONLY, not modified here
 * - Principle VII: Test-First Development - Tests written before repository implementation
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@ActiveProfiles("test")
@DisplayName("EmailVerificationTokenRepository Tests")
class EmailVerificationTokenRepositoryTest {

    @Autowired
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User testUser;
    private EmailVerificationToken validToken;

    @BeforeEach
    void setUp() {
        // Clean up before each test
        emailVerificationTokenRepository.deleteAll();

        // Create test user with unverified email
        testUser = User.builder()
                .keycloakId(UUID.randomUUID())
                .username("johndoe")
                .email("john.doe@example.com")
                .firstName("John")
                .lastName("Doe")
                .status(Status.pending)
                .emailVerified(false)
                .createdAt(OffsetDateTime.now())
                .createdBy("system")
                .isDeleted(false)
                .build();
        testUser = entityManager.persistAndFlush(testUser);

        // Create valid email verification token
        validToken = EmailVerificationToken.builder()
                .user(testUser)
                .email("john.doe@example.com")
                .token("verify_token_" + UUID.randomUUID())
                .expiresAt(OffsetDateTime.now().plusHours(24))
                .verified(false)
                .attempts(0)
                .createdAt(OffsetDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should save email verification token successfully")
    void shouldSaveEmailVerificationToken() {
        // When
        EmailVerificationToken savedToken = emailVerificationTokenRepository.save(validToken);

        // Then
        assertThat(savedToken).isNotNull();
        assertThat(savedToken.getId()).isNotNull();
        assertThat(savedToken.getToken()).isNotEmpty();
        assertThat(savedToken.getVerified()).isFalse();
        assertThat(savedToken.getUser().getId()).isEqualTo(testUser.getId());
        assertThat(savedToken.getEmail()).isEqualTo("john.doe@example.com");
    }

    @Test
    @DisplayName("Should find email verification token by token string")
    void shouldFindEmailVerificationTokenByToken() {
        // Given
        EmailVerificationToken savedToken = emailVerificationTokenRepository.save(validToken);

        // When
        Optional<EmailVerificationToken> foundToken = emailVerificationTokenRepository
                .findByToken(savedToken.getToken());

        // Then
        assertThat(foundToken).isPresent();
        assertThat(foundToken.get().getUser().getId()).isEqualTo(testUser.getId());
        assertThat(foundToken.get().getEmail()).isEqualTo("john.doe@example.com");
    }

    @Test
    @DisplayName("Should find valid unverified token by token string")
    void shouldFindValidUnverifiedToken() {
        // Given
        emailVerificationTokenRepository.save(validToken);

        // When
        Optional<EmailVerificationToken> foundToken = emailVerificationTokenRepository
                .findByTokenAndVerifiedFalseAndExpiresAtAfter(validToken.getToken(), OffsetDateTime.now());

        // Then
        assertThat(foundToken).isPresent();
        assertThat(foundToken.get().getVerified()).isFalse();
        assertThat(foundToken.get().getExpiresAt()).isAfter(OffsetDateTime.now());
    }

    @Test
    @DisplayName("Should not find expired token")
    void shouldNotFindExpiredToken() {
        // Given - Create expired token
        EmailVerificationToken expiredToken = EmailVerificationToken.builder()
                .user(testUser)
                .email("john.doe@example.com")
                .token("expired_token_" + UUID.randomUUID())
                .expiresAt(OffsetDateTime.now().minusHours(1))
                .verified(false)
                .attempts(0)
                .createdAt(OffsetDateTime.now().minusHours(25))
                .build();
        emailVerificationTokenRepository.save(expiredToken);

        // When
        Optional<EmailVerificationToken> foundToken = emailVerificationTokenRepository
                .findByTokenAndVerifiedFalseAndExpiresAtAfter(expiredToken.getToken(), OffsetDateTime.now());

        // Then
        assertThat(foundToken).isEmpty();
    }

    @Test
    @DisplayName("Should not find already verified token")
    void shouldNotFindVerifiedToken() {
        // Given - Create verified token
        validToken.setVerified(true);
        validToken.setVerifiedAt(OffsetDateTime.now().minusMinutes(10));
        emailVerificationTokenRepository.save(validToken);

        // When
        Optional<EmailVerificationToken> foundToken = emailVerificationTokenRepository
                .findByTokenAndVerifiedFalseAndExpiresAtAfter(validToken.getToken(), OffsetDateTime.now());

        // Then
        assertThat(foundToken).isEmpty();
    }

    @Test
    @DisplayName("Should find tokens by user ID")
    void shouldFindTokensByUserId() {
        // Given
        emailVerificationTokenRepository.save(validToken);

        EmailVerificationToken anotherToken = EmailVerificationToken.builder()
                .user(testUser)
                .email("john.doe@example.com")
                .token("another_verify_token_" + UUID.randomUUID())
                .expiresAt(OffsetDateTime.now().plusHours(24))
                .verified(false)
                .attempts(0)
                .createdAt(OffsetDateTime.now())
                .build();
        emailVerificationTokenRepository.save(anotherToken);

        // When
        List<EmailVerificationToken> userTokens = emailVerificationTokenRepository.findByUserId(testUser.getId());

        // Then
        assertThat(userTokens).hasSize(2);
    }

    @Test
    @DisplayName("Should find unverified tokens by user ID")
    void shouldFindUnverifiedTokensByUserId() {
        // Given
        emailVerificationTokenRepository.save(validToken);

        EmailVerificationToken verifiedToken = EmailVerificationToken.builder()
                .user(testUser)
                .email("john.doe@example.com")
                .token("verified_token_" + UUID.randomUUID())
                .expiresAt(OffsetDateTime.now().plusHours(24))
                .verified(true)
                .verifiedAt(OffsetDateTime.now().minusHours(1))
                .attempts(1)
                .createdAt(OffsetDateTime.now().minusHours(2))
                .build();
        emailVerificationTokenRepository.save(verifiedToken);

        // When
        List<EmailVerificationToken> unverifiedTokens = emailVerificationTokenRepository
                .findByUserIdAndVerifiedFalse(testUser.getId());

        // Then
        assertThat(unverifiedTokens).hasSize(1);
        assertThat(unverifiedTokens.get(0).getVerified()).isFalse();
    }

    @Test
    @DisplayName("Should find token by email address")
    void shouldFindTokenByEmail() {
        // Given
        emailVerificationTokenRepository.save(validToken);

        // When
        List<EmailVerificationToken> emailTokens = emailVerificationTokenRepository
                .findByEmail("john.doe@example.com");

        // Then
        assertThat(emailTokens).hasSize(1);
        assertThat(emailTokens.get(0).getEmail()).isEqualTo("john.doe@example.com");
    }

    @Test
    @DisplayName("Should find latest unverified token by user ID")
    void shouldFindLatestUnverifiedToken() {
        // Given - Create multiple tokens with different creation times
        EmailVerificationToken olderToken = EmailVerificationToken.builder()
                .user(testUser)
                .email("john.doe@example.com")
                .token("older_token")
                .expiresAt(OffsetDateTime.now().plusHours(24))
                .verified(false)
                .attempts(0)
                .createdAt(OffsetDateTime.now().minusHours(2))
                .build();
        emailVerificationTokenRepository.save(olderToken);

        validToken.setCreatedAt(OffsetDateTime.now());
        emailVerificationTokenRepository.save(validToken);

        // When
        Optional<EmailVerificationToken> latestToken = emailVerificationTokenRepository
                .findFirstByUserIdAndVerifiedFalseOrderByCreatedAtDesc(testUser.getId());

        // Then
        assertThat(latestToken).isPresent();
        assertThat(latestToken.get().getToken()).isEqualTo(validToken.getToken());
    }

    @Test
    @DisplayName("Should mark token as verified")
    void shouldMarkTokenAsVerified() {
        // Given
        EmailVerificationToken savedToken = emailVerificationTokenRepository.save(validToken);

        // When
        savedToken.setVerified(true);
        savedToken.setVerifiedAt(OffsetDateTime.now());
        savedToken.setAttempts(savedToken.getAttempts() + 1);
        EmailVerificationToken updatedToken = emailVerificationTokenRepository.save(savedToken);

        // Then
        assertThat(updatedToken.getVerified()).isTrue();
        assertThat(updatedToken.getVerifiedAt()).isNotNull();
        assertThat(updatedToken.getAttempts()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should increment verification attempts")
    void shouldIncrementVerificationAttempts() {
        // Given
        EmailVerificationToken savedToken = emailVerificationTokenRepository.save(validToken);

        // When - Simulate multiple verification attempts
        savedToken.setAttempts(savedToken.getAttempts() + 1);
        emailVerificationTokenRepository.save(savedToken);
        savedToken.setAttempts(savedToken.getAttempts() + 1);
        emailVerificationTokenRepository.save(savedToken);
        savedToken.setAttempts(savedToken.getAttempts() + 1);
        EmailVerificationToken updatedToken = emailVerificationTokenRepository.save(savedToken);

        // Then
        assertThat(updatedToken.getAttempts()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should delete expired tokens for cleanup")
    void shouldDeleteExpiredTokens() {
        // Given
        emailVerificationTokenRepository.save(validToken);

        EmailVerificationToken expiredToken = EmailVerificationToken.builder()
                .user(testUser)
                .email("john.doe@example.com")
                .token("expired_token")
                .expiresAt(OffsetDateTime.now().minusHours(25))
                .verified(false)
                .attempts(0)
                .createdAt(OffsetDateTime.now().minusHours(26))
                .build();
        emailVerificationTokenRepository.save(expiredToken);

        // When
        emailVerificationTokenRepository.deleteByExpiresAtBefore(OffsetDateTime.now());
        entityManager.flush();
        entityManager.clear();

        // Then
        List<EmailVerificationToken> remainingTokens = emailVerificationTokenRepository.findAll();
        assertThat(remainingTokens).hasSize(1);
        assertThat(remainingTokens.get(0).getToken()).isEqualTo(validToken.getToken());
    }

    @Test
    @DisplayName("Should find expired tokens for cleanup job")
    void shouldFindExpiredTokens() {
        // Given
        emailVerificationTokenRepository.save(validToken);

        EmailVerificationToken expiredToken1 = EmailVerificationToken.builder()
                .user(testUser)
                .email("john.doe@example.com")
                .token("expired_1")
                .expiresAt(OffsetDateTime.now().minusHours(1))
                .verified(false)
                .attempts(0)
                .createdAt(OffsetDateTime.now().minusHours(2))
                .build();
        emailVerificationTokenRepository.save(expiredToken1);

        EmailVerificationToken expiredToken2 = EmailVerificationToken.builder()
                .user(testUser)
                .email("john.doe@example.com")
                .token("expired_2")
                .expiresAt(OffsetDateTime.now().minusDays(1))
                .verified(false)
                .attempts(0)
                .createdAt(OffsetDateTime.now().minusDays(2))
                .build();
        emailVerificationTokenRepository.save(expiredToken2);

        // When
        List<EmailVerificationToken> expiredTokens = emailVerificationTokenRepository
                .findByExpiresAtBefore(OffsetDateTime.now());

        // Then
        assertThat(expiredTokens).hasSize(2);
    }

    @Test
    @DisplayName("Should count unverified tokens by user")
    void shouldCountUnverifiedTokensByUser() {
        // Given
        emailVerificationTokenRepository.save(validToken);

        EmailVerificationToken anotherUnverifiedToken = EmailVerificationToken.builder()
                .user(testUser)
                .email("john.doe@example.com")
                .token("another_unverified")
                .expiresAt(OffsetDateTime.now().plusHours(24))
                .verified(false)
                .attempts(0)
                .createdAt(OffsetDateTime.now())
                .build();
        emailVerificationTokenRepository.save(anotherUnverifiedToken);

        EmailVerificationToken verifiedToken = EmailVerificationToken.builder()
                .user(testUser)
                .email("john.doe@example.com")
                .token("verified_token")
                .expiresAt(OffsetDateTime.now().plusHours(24))
                .verified(true)
                .verifiedAt(OffsetDateTime.now())
                .attempts(1)
                .createdAt(OffsetDateTime.now())
                .build();
        emailVerificationTokenRepository.save(verifiedToken);

        // When
        long unverifiedCount = emailVerificationTokenRepository.countByUserIdAndVerifiedFalse(testUser.getId());

        // Then
        assertThat(unverifiedCount).isEqualTo(2);
    }

    @Test
    @DisplayName("Should delete all tokens for user")
    void shouldDeleteAllTokensForUser() {
        // Given
        emailVerificationTokenRepository.save(validToken);

        EmailVerificationToken anotherToken = EmailVerificationToken.builder()
                .user(testUser)
                .email("john.doe@example.com")
                .token("another_token")
                .expiresAt(OffsetDateTime.now().plusHours(24))
                .verified(false)
                .attempts(0)
                .createdAt(OffsetDateTime.now())
                .build();
        emailVerificationTokenRepository.save(anotherToken);

        // When
        emailVerificationTokenRepository.deleteByUserId(testUser.getId());
        entityManager.flush();
        entityManager.clear();

        // Then
        List<EmailVerificationToken> remainingTokens = emailVerificationTokenRepository.findByUserId(testUser.getId());
        assertThat(remainingTokens).isEmpty();
    }

    @Test
    @DisplayName("Should check if user has pending verification")
    void shouldCheckPendingVerification() {
        // Given
        emailVerificationTokenRepository.save(validToken);

        // When
        boolean hasPending = emailVerificationTokenRepository.existsByUserIdAndVerifiedFalse(testUser.getId());

        // Then
        assertThat(hasPending).isTrue();
    }

    @Test
    @DisplayName("Should invalidate old tokens when new one is created")
    void shouldInvalidateOldTokens() {
        // Given - User has old unverified tokens
        EmailVerificationToken oldToken1 = EmailVerificationToken.builder()
                .user(testUser)
                .email("john.doe@example.com")
                .token("old_token_1")
                .expiresAt(OffsetDateTime.now().plusHours(24))
                .verified(false)
                .attempts(0)
                .createdAt(OffsetDateTime.now().minusHours(5))
                .build();
        emailVerificationTokenRepository.save(oldToken1);

        EmailVerificationToken oldToken2 = EmailVerificationToken.builder()
                .user(testUser)
                .email("john.doe@example.com")
                .token("old_token_2")
                .expiresAt(OffsetDateTime.now().plusHours(24))
                .verified(false)
                .attempts(0)
                .createdAt(OffsetDateTime.now().minusHours(3))
                .build();
        emailVerificationTokenRepository.save(oldToken2);

        // When - Delete old unverified tokens before creating new one
        emailVerificationTokenRepository.deleteByUserIdAndVerifiedFalse(testUser.getId());
        emailVerificationTokenRepository.save(validToken);
        entityManager.flush();
        entityManager.clear();

        // Then - Only new token should exist
        List<EmailVerificationToken> userTokens = emailVerificationTokenRepository.findByUserId(testUser.getId());
        assertThat(userTokens).hasSize(1);
        assertThat(userTokens.get(0).getToken()).isEqualTo(validToken.getToken());
    }
}
