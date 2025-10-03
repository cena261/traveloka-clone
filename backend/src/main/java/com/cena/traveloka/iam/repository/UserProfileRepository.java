package com.cena.traveloka.iam.repository;

import com.cena.traveloka.iam.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * T019: UserProfileRepository interface
 * Repository for UserProfile entity.
 *
 * Constitutional Compliance:
 * - Principle III: Layered Architecture - Repository layer for data access
 * - Principle IV: Entity Immutability - UserProfile entity is READ-ONLY
 */
@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    /**
     * Find user profile by user ID.
     * @param userId User ID
     * @return Optional containing user profile if found
     */
    Optional<UserProfile> findByUserId(UUID userId);

    /**
     * Check if user profile exists for user.
     * @param userId User ID
     * @return true if profile exists, false otherwise
     */
    boolean existsByUserId(UUID userId);
}
