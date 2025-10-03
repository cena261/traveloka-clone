package com.cena.traveloka.iam.repository;

import com.cena.traveloka.iam.entity.TwoFactorAuth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * T026: TwoFactorAuthRepository interface
 * Repository for TwoFactorAuth entity for 2FA management (FR-014).
 *
 * Constitutional Compliance:
 * - Principle III: Layered Architecture - Repository layer for data access
 * - Principle IV: Entity Immutability - TwoFactorAuth entity is READ-ONLY
 */
@Repository
public interface TwoFactorAuthRepository extends JpaRepository<TwoFactorAuth, UUID> {

    /**
     * Find all 2FA methods for a user.
     * @param userId User ID
     * @return List of 2FA configurations
     */
    List<TwoFactorAuth> findByUserId(UUID userId);

    /**
     * Find 2FA configuration by user and method.
     * @param userId User ID
     * @param method 2FA method (totp, sms, email, backup_codes)
     * @return Optional containing 2FA configuration if found
     */
    Optional<TwoFactorAuth> findByUserIdAndMethod(UUID userId, String method);

    /**
     * Find active 2FA methods for a user.
     * @param userId User ID
     * @return List of active 2FA configurations
     */
    List<TwoFactorAuth> findByUserIdAndIsActiveTrue(UUID userId);

    /**
     * Find verified 2FA methods for a user.
     * @param userId User ID
     * @return List of verified 2FA configurations
     */
    List<TwoFactorAuth> findByUserIdAndVerifiedTrue(UUID userId);

    /**
     * Find primary 2FA method for a user.
     * @param userId User ID
     * @return Optional containing primary 2FA configuration
     */
    Optional<TwoFactorAuth> findByUserIdAndIsPrimaryTrue(UUID userId);

    /**
     * Check if user has active 2FA enabled.
     * @param userId User ID
     * @return true if user has active 2FA, false otherwise
     */
    boolean existsByUserIdAndIsActiveTrueAndVerifiedTrue(UUID userId);

    /**
     * Delete all 2FA configurations for a user.
     * @param userId User ID
     */
    void deleteByUserId(UUID userId);

    /**
     * Find 2FA configurations by method type.
     * @param method 2FA method
     * @return List of 2FA configurations using the method
     */
    List<TwoFactorAuth> findByMethod(String method);
}
