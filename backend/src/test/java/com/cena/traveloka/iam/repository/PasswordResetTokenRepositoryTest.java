package com.cena.traveloka.iam.repository;

import com.cena.traveloka.common.enums.Status;
import com.cena.traveloka.iam.entity.PasswordResetToken;
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
 * T008: PasswordResetTokenRepository test
 * Tests password reset token management (FR-009).
 *
 * TDD Phase: RED - These tests MUST fail before implementing PasswordResetTokenRepository
 *
 * Constitutional Compliance:
 * - Principle IV: Entity Immutability - PasswordResetToken entity is READ-ONLY, not modified here
 * - Principle VII: Test-First Development - Tests written before repository implementation
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@ActiveProfiles("test")
@DisplayName("PasswordResetTokenRepository Tests")
class PasswordResetTokenRepositoryTest {

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User testUser;
    private PasswordResetToken validToken;

    @BeforeEach
    void setUp() {
        // Clean up before each test
        passwordResetTokenRepository.deleteAll();

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

        // Create valid password reset token
        validToken = PasswordResetToken.builder()
                .user(testUser)
                .token("reset_token_" + UUID.randomUUID())
                .expiresAt(OffsetDateTime.now().plusHours(1))
                .used(false)
                .ipAddress("192.168.1.100")
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .createdAt(OffsetDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should save password reset token successfully")
    void shouldSavePasswordResetToken() {
        // When
        PasswordResetToken savedToken = passwordResetTokenRepository.save(validToken);

        // Then
        assertThat(savedToken).isNotNull();
        assertThat(savedToken.getId()).isNotNull();
        assertThat(savedToken.getToken()).isNotEmpty();
        assertThat(savedToken.getUsed()).isFalse();
        assertThat(savedToken.getUser().getId()).isEqualTo(testUser.getId());
    }

    @Test
    @DisplayName("Should find password reset token by token string")
    void shouldFindPasswordResetTokenByToken() {
        // Given
        PasswordResetToken savedToken = passwordResetTokenRepository.save(validToken);

        // When
        Optional<PasswordResetToken> foundToken = passwordResetTokenRepository.findByToken(savedToken.getToken());

        // Then
        assertThat(foundToken).isPresent();
        assertThat(foundToken.get().getUser().getId()).isEqualTo(testUser.getId());
    }

    @Test
    @DisplayName("Should find valid unused token by token string")
    void shouldFindValidUnusedToken() {
        // Given
        passwordResetTokenRepository.save(validToken);

        // When
        Optional<PasswordResetToken> foundToken = passwordResetTokenRepository
                .findByTokenAndUsedFalseAndExpiresAtAfter(validToken.getToken(), OffsetDateTime.now());

        // Then
        assertThat(foundToken).isPresent();
        assertThat(foundToken.get().getUsed()).isFalse();
        assertThat(foundToken.get().getExpiresAt()).isAfter(OffsetDateTime.now());
    }

    @Test
    @DisplayName("Should not find expired token")
    void shouldNotFindExpiredToken() {
        // Given - Create expired token
        PasswordResetToken expiredToken = PasswordResetToken.builder()
                .user(testUser)
                .token("expired_token_" + UUID.randomUUID())
                .expiresAt(OffsetDateTime.now().minusHours(1))
                .used(false)
                .createdAt(OffsetDateTime.now().minusHours(2))
                .build();
        passwordResetTokenRepository.save(expiredToken);

        // When
        Optional<PasswordResetToken> foundToken = passwordResetTokenRepository
                .findByTokenAndUsedFalseAndExpiresAtAfter(expiredToken.getToken(), OffsetDateTime.now());

        // Then
        assertThat(foundToken).isEmpty();
    }

    @Test
    @DisplayName("Should not find already used token")
    void shouldNotFindUsedToken() {
        // Given - Create used token
        validToken.setUsed(true);
        validToken.setUsedAt(OffsetDateTime.now().minusMinutes(10));
        passwordResetTokenRepository.save(validToken);

        // When
        Optional<PasswordResetToken> foundToken = passwordResetTokenRepository
                .findByTokenAndUsedFalseAndExpiresAtAfter(validToken.getToken(), OffsetDateTime.now());

        // Then
        assertThat(foundToken).isEmpty();
    }

    @Test
    @DisplayName("Should find all tokens by user ID")
    void shouldFindAllTokensByUserId() {
        // Given
        passwordResetTokenRepository.save(validToken);

        PasswordResetToken anotherToken = PasswordResetToken.builder()
                .user(testUser)
                .token("another_reset_token_" + UUID.randomUUID())
                .expiresAt(OffsetDateTime.now().plusHours(1))
                .used(false)
                .createdAt(OffsetDateTime.now())
                .build();
        passwordResetTokenRepository.save(anotherToken);

        // When
        List<PasswordResetToken> userTokens = passwordResetTokenRepository.findByUserId(testUser.getId());

        // Then
        assertThat(userTokens).hasSize(2);
    }

    @Test
    @DisplayName("Should find unused tokens by user ID")
    void shouldFindUnusedTokensByUserId() {
        // Given
        passwordResetTokenRepository.save(validToken);

        PasswordResetToken usedToken = PasswordResetToken.builder()
                .user(testUser)
                .token("used_token_" + UUID.randomUUID())
                .expiresAt(OffsetDateTime.now().plusHours(1))
                .used(true)
                .usedAt(OffsetDateTime.now().minusMinutes(30))
                .createdAt(OffsetDateTime.now().minusHours(1))
                .build();
        passwordResetTokenRepository.save(usedToken);

        // When
        List<PasswordResetToken> unusedTokens = passwordResetTokenRepository.findByUserIdAndUsedFalse(testUser.getId());

        // Then
        assertThat(unusedTokens).hasSize(1);
        assertThat(unusedTokens.get(0).getUsed()).isFalse();
    }

    @Test
    @DisplayName("Should mark token as used")
    void shouldMarkTokenAsUsed() {
        // Given
        PasswordResetToken savedToken = passwordResetTokenRepository.save(validToken);

        // When
        savedToken.setUsed(true);
        savedToken.setUsedAt(OffsetDateTime.now());
        savedToken.setIpAddress("192.168.1.101");
        PasswordResetToken updatedToken = passwordResetTokenRepository.save(savedToken);

        // Then
        assertThat(updatedToken.getUsed()).isTrue();
        assertThat(updatedToken.getUsedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should delete expired tokens for cleanup")
    void shouldDeleteExpiredTokens() {
        // Given
        passwordResetTokenRepository.save(validToken);

        PasswordResetToken expiredToken = PasswordResetToken.builder()
                .user(testUser)
                .token("expired_token")
                .expiresAt(OffsetDateTime.now().minusHours(2))
                .used(false)
                .createdAt(OffsetDateTime.now().minusHours(3))
                .build();
        passwordResetTokenRepository.save(expiredToken);

        // When
        passwordResetTokenRepository.deleteByExpiresAtBefore(OffsetDateTime.now());
        entityManager.flush();
        entityManager.clear();

        // Then
        List<PasswordResetToken> remainingTokens = passwordResetTokenRepository.findAll();
        assertThat(remainingTokens).hasSize(1);
        assertThat(remainingTokens.get(0).getToken()).isEqualTo(validToken.getToken());
    }

    @Test
    @DisplayName("Should find expired tokens for cleanup job")
    void shouldFindExpiredTokens() {
        // Given
        passwordResetTokenRepository.save(validToken);

        PasswordResetToken expiredToken1 = PasswordResetToken.builder()
                .user(testUser)
                .token("expired_1")
                .expiresAt(OffsetDateTime.now().minusHours(1))
                .used(false)
                .createdAt(OffsetDateTime.now().minusHours(2))
                .build();
        passwordResetTokenRepository.save(expiredToken1);

        PasswordResetToken expiredToken2 = PasswordResetToken.builder()
                .user(testUser)
                .token("expired_2")
                .expiresAt(OffsetDateTime.now().minusDays(1))
                .used(false)
                .createdAt(OffsetDateTime.now().minusDays(2))
                .build();
        passwordResetTokenRepository.save(expiredToken2);

        // When
        List<PasswordResetToken> expiredTokens = passwordResetTokenRepository.findByExpiresAtBefore(OffsetDateTime.now());

        // Then
        assertThat(expiredTokens).hasSize(2);
    }

    @Test
    @DisplayName("Should count unused tokens by user")
    void shouldCountUnusedTokensByUser() {
        // Given
        passwordResetTokenRepository.save(validToken);

        PasswordResetToken anotherUnusedToken = PasswordResetToken.builder()
                .user(testUser)
                .token("another_unused")
                .expiresAt(OffsetDateTime.now().plusHours(1))
                .used(false)
                .createdAt(OffsetDateTime.now())
                .build();
        passwordResetTokenRepository.save(anotherUnusedToken);

        PasswordResetToken usedToken = PasswordResetToken.builder()
                .user(testUser)
                .token("used_token")
                .expiresAt(OffsetDateTime.now().plusHours(1))
                .used(true)
                .usedAt(OffsetDateTime.now())
                .createdAt(OffsetDateTime.now())
                .build();
        passwordResetTokenRepository.save(usedToken);

        // When
        long unusedCount = passwordResetTokenRepository.countByUserIdAndUsedFalse(testUser.getId());

        // Then
        assertThat(unusedCount).isEqualTo(2);
    }

    @Test
    @DisplayName("Should invalidate all user tokens by marking them as used")
    void shouldInvalidateAllUserTokens() {
        // Given
        passwordResetTokenRepository.save(validToken);

        PasswordResetToken anotherToken = PasswordResetToken.builder()
                .user(testUser)
                .token("another_token")
                .expiresAt(OffsetDateTime.now().plusHours(1))
                .used(false)
                .createdAt(OffsetDateTime.now())
                .build();
        passwordResetTokenRepository.save(anotherToken);

        // When - Invalidate all by marking as used
        List<PasswordResetToken> userTokens = passwordResetTokenRepository.findByUserIdAndUsedFalse(testUser.getId());
        userTokens.forEach(token -> {
            token.setUsed(true);
            token.setUsedAt(OffsetDateTime.now());
        });
        passwordResetTokenRepository.saveAll(userTokens);

        // Then
        long unusedCount = passwordResetTokenRepository.countByUserIdAndUsedFalse(testUser.getId());
        assertThat(unusedCount).isZero();
    }

    @Test
    @DisplayName("Should delete all tokens for user")
    void shouldDeleteAllTokensForUser() {
        // Given
        passwordResetTokenRepository.save(validToken);

        PasswordResetToken anotherToken = PasswordResetToken.builder()
                .user(testUser)
                .token("another_token")
                .expiresAt(OffsetDateTime.now().plusHours(1))
                .used(false)
                .createdAt(OffsetDateTime.now())
                .build();
        passwordResetTokenRepository.save(anotherToken);

        // When
        passwordResetTokenRepository.deleteByUserId(testUser.getId());
        entityManager.flush();
        entityManager.clear();

        // Then
        List<PasswordResetToken> remainingTokens = passwordResetTokenRepository.findByUserId(testUser.getId());
        assertThat(remainingTokens).isEmpty();
    }
}
