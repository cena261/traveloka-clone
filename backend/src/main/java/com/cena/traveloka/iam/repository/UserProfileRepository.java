package com.cena.traveloka.iam.repository;

import com.cena.traveloka.iam.entity.UserProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for UserProfile entity operations
 *
 * Provides operations for managing extended user profile information including:
 * - Personal details and demographics
 * - Travel preferences and requirements
 * - Emergency contact information
 * - Privacy and preference settings
 * - Profile analytics and reporting
 *
 * Key Features:
 * - User-profile relationship queries
 * - Profile completeness tracking
 * - Demographic filtering and analytics
 * - Privacy-aware profile searches
 * - Emergency contact management
 */
@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, String> {

    // === Basic User Profile Relationships ===

    /**
     * Find profile by user ID - primary lookup method
     * Uses unique index on user_id for optimal performance
     */
    @Query("SELECT p FROM UserProfile p WHERE p.userId = :userId")
    Optional<UserProfile> findByUserId(@Param("userId") String userId);

    /**
     * Check if profile exists for user
     */
    boolean existsByUserId(String userId);

    /**
     * Delete profile by user ID
     */
    @Modifying
    @Query("DELETE FROM UserProfile p WHERE p.userId = :userId")
    int deleteByUserId(@Param("userId") String userId);

    // === Demographic and Analytics Queries ===

    /**
     * Find profiles by nationality for regional analytics
     * Uses index on nationality for performance
     */
    @Query("SELECT p FROM UserProfile p WHERE p.nationality = :nationality")
    Page<UserProfile> findByNationality(@Param("nationality") String nationality, Pageable pageable);

    /**
     * Count profiles by nationality for demographics reporting
     */
    @Query("SELECT p.nationality, COUNT(p) FROM UserProfile p WHERE p.nationality IS NOT NULL GROUP BY p.nationality")
    List<Object[]> countByNationality();

    /**
     * Find profiles by age range based on date of birth
     */
    @Query("SELECT p FROM UserProfile p WHERE p.dateOfBirth BETWEEN :startDate AND :endDate")
    Page<UserProfile> findByAgeRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable);

    /**
     * Find profiles by gender for demographic analysis
     */
    @Query("SELECT p FROM UserProfile p WHERE p.gender = :gender")
    Page<UserProfile> findByGender(@Param("gender") String gender, Pageable pageable);

    // === Profile Completeness and Quality ===

    /**
     * Find profiles missing critical information
     */
    @Query("SELECT p FROM UserProfile p WHERE " +
           "p.dateOfBirth IS NULL OR " +
           "p.nationality IS NULL OR " +
           "p.emergencyContactName IS NULL OR " +
           "p.emergencyContactPhone IS NULL")
    List<UserProfile> findIncompleteProfiles();

    /**
     * Find profiles with missing travel documents
     */
    @Query("SELECT p FROM UserProfile p WHERE " +
           "JSON_EXTRACT(p.profileData, '$.passportNumber') IS NULL OR " +
           "JSON_EXTRACT(p.profileData, '$.passportNumber') = ''")
    List<UserProfile> findProfilesWithoutPassport();

    /**
     * Find profiles with missing emergency contacts
     */
    @Query("SELECT p FROM UserProfile p WHERE " +
           "p.emergencyContactName IS NULL OR p.emergencyContactName = '' OR " +
           "p.emergencyContactPhone IS NULL OR p.emergencyContactPhone = ''")
    List<UserProfile> findProfilesWithoutEmergencyContact();

    // === Privacy and Verification ===

    /**
     * Find profiles by verification status
     */
    @Query("SELECT p FROM UserProfile p WHERE p.verificationStatus = :verificationStatus")
    Page<UserProfile> findByVerificationStatus(
            @Param("verificationStatus") String verificationStatus,
            Pageable pageable);

    /**
     * Count profiles by verification status for metrics
     */
    @Query("SELECT p.verificationStatus, COUNT(p) FROM UserProfile p GROUP BY p.verificationStatus")
    List<Object[]> countByVerificationStatus();

    /**
     * Find unverified profiles older than threshold
     */
    @Query("SELECT p FROM UserProfile p WHERE " +
           "p.verificationStatus = 'UNVERIFIED' AND " +
           "p.createdAt < :threshold")
    List<UserProfile> findUnverifiedProfilesOlderThan(@Param("threshold") Instant threshold);

    // === Travel and Emergency Data ===

    /**
     * Find profiles by passport number for travel verification
     */
    @Query("SELECT p FROM UserProfile p WHERE " +
           "JSON_EXTRACT(p.profileData, '$.passportNumber') = :passportNumber")
    Optional<UserProfile> findByPassportNumber(@Param("passportNumber") String passportNumber);

    /**
     * Check if passport number is already in use
     */
    @Query("SELECT COUNT(p) > 0 FROM UserProfile p WHERE " +
           "JSON_EXTRACT(p.profileData, '$.passportNumber') = :passportNumber")
    boolean existsByPassportNumber(@Param("passportNumber") String passportNumber);

    /**
     * Find profiles with expiring passports (if expiry data exists in JSONB)
     */
    @Query("SELECT p FROM UserProfile p WHERE " +
           "JSON_EXTRACT(p.profileData, '$.passportExpiry') IS NOT NULL AND " +
           "STR_TO_DATE(JSON_UNQUOTE(JSON_EXTRACT(p.profileData, '$.passportExpiry')), '%Y-%m-%d') < :expiryThreshold")
    List<UserProfile> findProfilesWithExpiringPassports(@Param("expiryThreshold") LocalDate expiryThreshold);

    // === Search and Filtering ===

    /**
     * Search profiles by multiple criteria for admin interface
     */
    @Query("SELECT p FROM UserProfile p WHERE " +
           "(:nationality IS NULL OR p.nationality = :nationality) AND " +
           "(:gender IS NULL OR p.gender = :gender) AND " +
           "(:verificationStatus IS NULL OR p.verificationStatus = :verificationStatus) AND " +
           "(:minAge IS NULL OR p.dateOfBirth <= :maxBirthDate) AND " +
           "(:maxAge IS NULL OR p.dateOfBirth >= :minBirthDate)")
    Page<UserProfile> searchProfiles(
            @Param("nationality") String nationality,
            @Param("gender") String gender,
            @Param("verificationStatus") String verificationStatus,
            @Param("minAge") Integer minAge,
            @Param("maxAge") Integer maxAge,
            @Param("minBirthDate") LocalDate minBirthDate,
            @Param("maxBirthDate") LocalDate maxBirthDate,
            Pageable pageable);

    /**
     * Find profiles by partial name match in JSONB data
     */
    @Query("SELECT p FROM UserProfile p WHERE " +
           "JSON_UNQUOTE(JSON_EXTRACT(p.profileData, '$.displayName')) LIKE CONCAT('%', :name, '%')")
    Page<UserProfile> findByDisplayNameContaining(@Param("name") String name, Pageable pageable);

    // === Time-based Queries ===

    /**
     * Find recently created profiles for onboarding tracking
     */
    @Query("SELECT p FROM UserProfile p WHERE p.createdAt >= :since ORDER BY p.createdAt DESC")
    List<UserProfile> findRecentlyCreatedProfiles(@Param("since") Instant since);

    /**
     * Find profiles created within date range
     */
    @Query("SELECT p FROM UserProfile p WHERE p.createdAt BETWEEN :startDate AND :endDate ORDER BY p.createdAt DESC")
    Page<UserProfile> findProfilesCreatedBetween(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            Pageable pageable);

    /**
     * Find stale profiles not updated recently
     */
    @Query("SELECT p FROM UserProfile p WHERE p.updatedAt < :threshold")
    List<UserProfile> findStaleProfiles(@Param("threshold") Instant threshold);

    // === Bulk Operations ===

    /**
     * Bulk update verification status
     */
    @Modifying
    @Query("UPDATE UserProfile p SET p.verificationStatus = :verificationStatus, " +
           "p.updatedAt = CURRENT_TIMESTAMP, p.updatedBy = :updatedBy " +
           "WHERE p.id IN :profileIds")
    int updateVerificationStatusForProfiles(
            @Param("profileIds") List<String> profileIds,
            @Param("verificationStatus") String verificationStatus,
            @Param("updatedBy") String updatedBy);

    /**
     * Bulk update profile metadata
     */
    @Modifying
    @Query("UPDATE UserProfile p SET p.updatedAt = CURRENT_TIMESTAMP, p.updatedBy = :updatedBy " +
           "WHERE p.userId IN :userIds")
    int touchProfilesForUsers(
            @Param("userIds") List<String> userIds,
            @Param("updatedBy") String updatedBy);

    // === Analytics and Reporting ===

    /**
     * Get profile statistics for dashboard
     */
    @Query("SELECT " +
           "COUNT(*) as totalProfiles, " +
           "COUNT(CASE WHEN p.verificationStatus = 'VERIFIED' THEN 1 END) as verifiedProfiles, " +
           "COUNT(CASE WHEN JSON_EXTRACT(p.profileData, '$.passportNumber') IS NOT NULL THEN 1 END) as profilesWithPassport, " +
           "COUNT(CASE WHEN p.emergencyContactName IS NOT NULL THEN 1 END) as profilesWithEmergencyContact, " +
           "COUNT(DISTINCT p.nationality) as uniqueNationalities " +
           "FROM UserProfile p")
    ProfileStatistics getProfileStatistics();

    /**
     * Interface for profile statistics projection
     */
    interface ProfileStatistics {
        long getTotalProfiles();
        long getVerifiedProfiles();
        long getProfilesWithPassport();
        long getProfilesWithEmergencyContact();
        long getUniqueNationalities();
    }

    /**
     * Get age distribution for analytics
     */
    @Query("SELECT " +
           "CASE " +
           "    WHEN DATEDIFF(CURRENT_DATE, p.dateOfBirth) / 365.25 < 25 THEN 'Under 25' " +
           "    WHEN DATEDIFF(CURRENT_DATE, p.dateOfBirth) / 365.25 < 35 THEN '25-34' " +
           "    WHEN DATEDIFF(CURRENT_DATE, p.dateOfBirth) / 365.25 < 45 THEN '35-44' " +
           "    WHEN DATEDIFF(CURRENT_DATE, p.dateOfBirth) / 365.25 < 55 THEN '45-54' " +
           "    ELSE '55+' " +
           "END as ageGroup, " +
           "COUNT(*) as count " +
           "FROM UserProfile p " +
           "WHERE p.dateOfBirth IS NOT NULL " +
           "GROUP BY ageGroup")
    List<Object[]> getAgeDistribution();

    // === Data Quality and Validation ===

    /**
     * Find profiles with potentially invalid data
     */
    @Query("SELECT p FROM UserProfile p WHERE " +
           "(p.dateOfBirth IS NOT NULL AND p.dateOfBirth > CURRENT_DATE) OR " +
           "(p.dateOfBirth IS NOT NULL AND DATEDIFF(CURRENT_DATE, p.dateOfBirth) / 365.25 > 120)")
    List<UserProfile> findProfilesWithInvalidDates();

    /**
     * Find duplicate passport numbers (data integrity check)
     */
    @Query("SELECT JSON_UNQUOTE(JSON_EXTRACT(p.profileData, '$.passportNumber')), COUNT(*) FROM UserProfile p WHERE " +
           "JSON_EXTRACT(p.profileData, '$.passportNumber') IS NOT NULL " +
           "GROUP BY JSON_UNQUOTE(JSON_EXTRACT(p.profileData, '$.passportNumber')) HAVING COUNT(*) > 1")
    List<Object[]> findDuplicatePassportNumbers();

    /**
     * Find profiles missing required fields for specific verification levels
     */
    @Query("SELECT p FROM UserProfile p WHERE " +
           "p.verificationStatus = 'VERIFIED' AND " +
           "(p.dateOfBirth IS NULL OR p.nationality IS NULL OR JSON_EXTRACT(p.profileData, '$.passportNumber') IS NULL)")
    List<UserProfile> findVerifiedProfilesWithMissingData();

    // === Missing Methods for Service Support ===

    /**
     * Find verified profiles with pagination
     */
    Page<UserProfile> findByVerifiedTrue(Pageable pageable);

    /**
     * Find unverified profiles with pagination
     */
    Page<UserProfile> findByVerifiedFalse(Pageable pageable);

    /**
     * Count verified profiles
     */
    long countByVerifiedTrue();

    /**
     * Count profiles by specific nationality
     */
    @Query("SELECT COUNT(*) FROM UserProfile p WHERE p.nationality = :nationality")
    long countByNationality(@Param("nationality") String nationality);
}