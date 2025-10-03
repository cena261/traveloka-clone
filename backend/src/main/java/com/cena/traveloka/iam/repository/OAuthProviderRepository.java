package com.cena.traveloka.iam.repository;

import com.cena.traveloka.iam.entity.OAuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * T027: OAuthProviderRepository interface
 * Repository for OAuthProvider entity for social login management (FR-012).
 *
 * Constitutional Compliance:
 * - Principle III: Layered Architecture - Repository layer for data access
 * - Principle IV: Entity Immutability - OAuthProvider entity is READ-ONLY
 */
@Repository
public interface OAuthProviderRepository extends JpaRepository<OAuthProvider, UUID> {

    /**
     * Find all OAuth providers linked to a user.
     * @param userId User ID
     * @return List of OAuth provider configurations
     */
    List<OAuthProvider> findByUserId(UUID userId);

    /**
     * Find OAuth provider by user and provider name.
     * @param userId User ID
     * @param provider Provider name (google, facebook, apple)
     * @return Optional containing OAuth provider configuration
     */
    Optional<OAuthProvider> findByUserIdAndProvider(UUID userId, String provider);

    /**
     * Find OAuth provider by provider name and provider user ID.
     * Used during OAuth callback to find existing account.
     * @param provider Provider name
     * @param providerUserId Provider-specific user ID
     * @return Optional containing OAuth provider configuration
     */
    Optional<OAuthProvider> findByProviderAndProviderUserId(String provider, String providerUserId);

    /**
     * Check if OAuth provider is already linked to user.
     * @param userId User ID
     * @param provider Provider name
     * @return true if provider is linked, false otherwise
     */
    boolean existsByUserIdAndProvider(UUID userId, String provider);

    /**
     * Find all OAuth accounts by provider type.
     * @param provider Provider name (google, facebook, apple)
     * @return List of OAuth configurations for the provider
     */
    List<OAuthProvider> findByProvider(String provider);

    /**
     * Delete OAuth provider link for a user.
     * @param userId User ID
     * @param provider Provider name
     */
    void deleteByUserIdAndProvider(UUID userId, String provider);

    /**
     * Delete all OAuth providers for a user.
     * @param userId User ID
     */
    void deleteByUserId(UUID userId);

    /**
     * Find OAuth provider by email.
     * Used for account linking when email matches.
     * @param email Email address
     * @return List of OAuth providers with the email
     */
    List<OAuthProvider> findByEmail(String email);
}
