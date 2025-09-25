package com.cena.traveloka.iam.service;

import com.cena.traveloka.iam.entity.UserProfile;
import com.cena.traveloka.iam.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Period;
import java.util.List;
import java.util.Optional;

/**
 * Service class for UserProfile entity operations
 *
 * Provides comprehensive user profile management functionality including:
 * - Profile CRUD operations with validation
 * - Demographic data management
 * - Travel document verification
 * - Emergency contact management
 * - Profile analytics and reporting
 * - Data quality validation
 *
 * Key Features:
 * - Transactional operations for data consistency
 * - Redis caching for frequently accessed profiles
 * - Business validation for profile data
 * - Travel-specific data management
 * - Audit logging for profile changes
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;

    // === Core CRUD Operations ===

    /**
     * Create a new user profile with validation
     *
     * @param profile Profile entity to create
     * @return Created profile with generated ID
     * @throws IllegalArgumentException if profile data is invalid
     */
    public UserProfile createProfile(UserProfile profile) {
        log.info("Creating new profile for user: {}", profile.getUserId());

        validateProfile(profile);
        validateUniqueConstraints(profile);

        profile.setCreatedBy("SYSTEM");
        profile.setUpdatedBy("SYSTEM");

        UserProfile savedProfile = userProfileRepository.save(profile);
        log.info("Successfully created profile with ID: {}", savedProfile.getId());

        return savedProfile;
    }

    /**
     * Find profile by ID with caching
     *
     * @param profileId Profile ID
     * @return Profile if found
     */
    @Cacheable(value = "profiles", key = "#profileId")
    @Transactional(readOnly = true)
    public Optional<UserProfile> findById(String profileId) {
        log.debug("Finding profile by ID: {}", profileId);
        return userProfileRepository.findById(profileId);
    }

    /**
     * Find profile by user ID
     *
     * @param userId User ID
     * @return Profile if found
     */
    @Cacheable(value = "profiles", key = "'user:' + #userId")
    @Transactional(readOnly = true)
    public Optional<UserProfile> findByUserId(String userId) {
        log.debug("Finding profile by user ID: {}", userId);
        return userProfileRepository.findByUserId(userId);
    }

    /**
     * Update profile with cache eviction
     *
     * @param profile Profile to update
     * @return Updated profile
     */
    @CacheEvict(value = "profiles", allEntries = true)
    public UserProfile updateProfile(UserProfile profile) {
        log.info("Updating profile with ID: {}", profile.getId());

        validateProfile(profile);
        profile.setUpdatedBy("SYSTEM");

        UserProfile updatedProfile = userProfileRepository.save(profile);
        log.info("Successfully updated profile with ID: {}", updatedProfile.getId());

        return updatedProfile;
    }

    /**
     * Delete profile by user ID
     *
     * @param userId User ID whose profile to delete
     * @param deletedBy Who performed the deletion
     * @return Number of profiles deleted
     */
    @CacheEvict(value = "profiles", allEntries = true)
    public int deleteProfileByUserId(String userId, String deletedBy) {
        log.info("Deleting profile for user: {}", userId);

        int deletedCount = userProfileRepository.deleteByUserId(userId);
        log.info("Successfully deleted {} profile(s) for user: {}", deletedCount, userId);

        return deletedCount;
    }

    // === Demographic and Analytics ===

    /**
     * Find profiles by nationality
     *
     * @param nationality Nationality code
     * @param pageable Pagination parameters
     * @return Page of profiles
     */
    @Transactional(readOnly = true)
    public Page<UserProfile> findByNationality(String nationality, Pageable pageable) {
        log.debug("Finding profiles by nationality: {}", nationality);
        return userProfileRepository.findByNationality(nationality, pageable);
    }

    /**
     * Find profiles by age range
     *
     * @param minAge Minimum age
     * @param maxAge Maximum age
     * @param pageable Pagination parameters
     * @return Page of profiles
     */
    @Transactional(readOnly = true)
    public Page<UserProfile> findByAgeRange(int minAge, int maxAge, Pageable pageable) {
        log.debug("Finding profiles by age range: {} to {}", minAge, maxAge);

        LocalDate maxBirthDate = LocalDate.now().minusYears(minAge);
        LocalDate minBirthDate = LocalDate.now().minusYears(maxAge + 1);

        return userProfileRepository.findByAgeRange(minBirthDate, maxBirthDate, pageable);
    }

    /**
     * Find profiles by gender
     *
     * @param gender Gender
     * @param pageable Pagination parameters
     * @return Page of profiles
     */
    @Transactional(readOnly = true)
    public Page<UserProfile> findByGender(String gender, Pageable pageable) {
        log.debug("Finding profiles by gender: {}", gender);
        return userProfileRepository.findByGender(gender, pageable);
    }

    /**
     * Get nationality distribution for analytics
     *
     * @return List of nationality counts
     */
    @Transactional(readOnly = true)
    public List<Object[]> getNationalityDistribution() {
        log.debug("Getting nationality distribution");
        return userProfileRepository.countByNationality();
    }

    // === Profile Completeness and Quality ===

    /**
     * Find profiles with incomplete information
     *
     * @return List of incomplete profiles
     */
    @Transactional(readOnly = true)
    public List<UserProfile> findIncompleteProfiles() {
        log.debug("Finding incomplete profiles");
        return userProfileRepository.findIncompleteProfiles();
    }

    /**
     * Find profiles missing travel documents
     *
     * @return List of profiles without passport
     */
    @Transactional(readOnly = true)
    public List<UserProfile> findProfilesWithoutPassport() {
        log.debug("Finding profiles without passport - feature not implemented");
        return List.of(); // Return empty list as passport field doesn't exist
    }

    /**
     * Find profiles missing emergency contacts
     *
     * @return List of profiles without emergency contact
     */
    @Transactional(readOnly = true)
    public List<UserProfile> findProfilesWithoutEmergencyContact() {
        log.debug("Finding profiles without emergency contact");
        return userProfileRepository.findProfilesWithoutEmergencyContact();
    }

    /**
     * Calculate profile completeness score
     *
     * @param profile Profile to calculate completeness for
     * @return Completeness percentage (0-100)
     */
    public int calculateProfileCompleteness(UserProfile profile) {
        int totalFields = 7; // dateOfBirth, gender, nationality, emergency contact (name + phone), bio, avatar
        int filledFields = 0;

        if (profile.getDateOfBirth() != null) filledFields++;
        if (profile.getGender() != null) filledFields++;
        if (StringUtils.hasText(profile.getNationality())) filledFields++;
        if (StringUtils.hasText(profile.getEmergencyContactName())) filledFields++;
        if (StringUtils.hasText(profile.getEmergencyContactPhone())) filledFields++;
        if (StringUtils.hasText(profile.getBio())) filledFields++;
        if (StringUtils.hasText(profile.getAvatarUrl())) filledFields++;

        return Math.round(((float) filledFields / totalFields) * 100);
    }

    // === Verification Management ===

    /**
     * Find profiles by verification status
     *
     * @param verificationStatus Verification status
     * @param pageable Pagination parameters
     * @return Page of profiles
     */
    @Transactional(readOnly = true)
    public Page<UserProfile> findByVerificationStatus(String verificationStatus, Pageable pageable) {
        log.debug("Finding profiles by verification status: {}", verificationStatus);
        return userProfileRepository.findByVerificationStatus(verificationStatus, pageable);
    }

    /**
     * Find unverified profiles older than threshold
     *
     * @param thresholdDays Days since creation to consider stale
     * @return List of stale unverified profiles
     */
    @Transactional(readOnly = true)
    public List<UserProfile> findStaleUnverifiedProfiles(int thresholdDays) {
        log.debug("Finding unverified profiles older than {} days", thresholdDays);

        Instant threshold = Instant.now().minusSeconds(thresholdDays * 24 * 3600L);
        return userProfileRepository.findUnverifiedProfilesOlderThan(threshold);
    }

    /**
     * Update verification status for multiple profiles
     *
     * @param profileIds List of profile IDs
     * @param verificationStatus New verification status
     * @param updatedBy Who performed the update
     * @return Number of profiles updated
     */
    @CacheEvict(value = "profiles", allEntries = true)
    public int updateVerificationStatus(List<String> profileIds, String verificationStatus, String updatedBy) {
        log.info("Updating verification status for {} profiles to: {}", profileIds.size(), verificationStatus);

        int updatedCount = userProfileRepository.updateVerificationStatusForProfiles(profileIds, verificationStatus, updatedBy);
        log.info("Successfully updated verification status for {} profiles", updatedCount);

        return updatedCount;
    }

    /**
     * Get verification status distribution
     *
     * @return List of verification status counts
     */
    @Transactional(readOnly = true)
    public List<Object[]> getVerificationStatusDistribution() {
        log.debug("Getting verification status distribution");
        return userProfileRepository.countByVerificationStatus();
    }

    // === Travel Document Management ===

    /**
     * Find profile by passport number
     *
     * @param passportNumber Passport number
     * @return Profile if found
     */
    @Transactional(readOnly = true)
    public Optional<UserProfile> findByPassportNumber(String passportNumber) {
        log.debug("Finding profile by passport number - feature not implemented");
        return Optional.empty(); // Return empty as passport field doesn't exist
    }

    /**
     * Check if passport number is already in use
     *
     * @param passportNumber Passport number to check
     * @return true if passport number exists
     */
    @Transactional(readOnly = true)
    public boolean existsByPassportNumber(String passportNumber) {
        return false; // Return false as passport field doesn't exist
    }

    /**
     * Find profiles with expiring passports
     *
     * @param daysAhead Number of days ahead to check for expiry
     * @return List of profiles with expiring passports
     */
    @Transactional(readOnly = true)
    public List<UserProfile> findProfilesWithExpiringPassports(int daysAhead) {
        log.debug("Finding profiles with expiring passports - feature not implemented");
        return List.of(); // Return empty list as passport field doesn't exist
    }

    // === Search and Filtering ===

    /**
     * Search profiles by display name
     *
     * @param name Name pattern to search
     * @param pageable Pagination parameters
     * @return Page of matching profiles
     */
    @Transactional(readOnly = true)
    public Page<UserProfile> searchByDisplayName(String name, Pageable pageable) {
        log.debug("Searching profiles by display name: {}", name);
        return userProfileRepository.findByDisplayNameContaining(name, pageable);
    }

    /**
     * Advanced profile search with multiple criteria
     *
     * @param nationality Nationality filter (optional)
     * @param gender Gender filter (optional)
     * @param verificationStatus Verification status filter (optional)
     * @param minAge Minimum age filter (optional)
     * @param maxAge Maximum age filter (optional)
     * @param pageable Pagination parameters
     * @return Page of matching profiles
     */
    @Transactional(readOnly = true)
    public Page<UserProfile> searchProfiles(String nationality, String gender, String verificationStatus,
                                          Integer minAge, Integer maxAge, Pageable pageable) {
        log.debug("Advanced profile search with filters - nationality: {}, gender: {}, verification: {}, age: {}-{}",
                 nationality, gender, verificationStatus, minAge, maxAge);

        LocalDate minBirthDate = null;
        LocalDate maxBirthDate = null;

        if (maxAge != null) {
            minBirthDate = LocalDate.now().minusYears(maxAge + 1);
        }

        if (minAge != null) {
            maxBirthDate = LocalDate.now().minusYears(minAge);
        }

        return userProfileRepository.searchProfiles(nationality, gender, verificationStatus,
                                                  minAge, maxAge, minBirthDate, maxBirthDate, pageable);
    }

    // === Time-based Operations ===

    /**
     * Find recently created profiles
     *
     * @param hours Number of hours to look back
     * @return List of recently created profiles
     */
    @Transactional(readOnly = true)
    public List<UserProfile> findRecentlyCreatedProfiles(int hours) {
        log.debug("Finding profiles created in the last {} hours", hours);

        Instant since = Instant.now().minusSeconds(hours * 3600L);
        return userProfileRepository.findRecentlyCreatedProfiles(since);
    }

    /**
     * Find stale profiles not updated recently
     *
     * @param thresholdDays Days since last update to consider stale
     * @return List of stale profiles
     */
    @Transactional(readOnly = true)
    public List<UserProfile> findStaleProfiles(int thresholdDays) {
        log.debug("Finding profiles not updated in {} days", thresholdDays);

        Instant threshold = Instant.now().minusSeconds(thresholdDays * 24 * 3600L);
        return userProfileRepository.findStaleProfiles(threshold);
    }

    // === Analytics and Reporting ===

    /**
     * Get profile statistics for dashboard
     *
     * @return Profile statistics
     */
    @Transactional(readOnly = true)
    public UserProfileRepository.ProfileStatistics getProfileStatistics() {
        log.debug("Retrieving profile statistics");
        return userProfileRepository.getProfileStatistics();
    }

    /**
     * Get age distribution for analytics
     *
     * @return List of age group counts
     */
    @Transactional(readOnly = true)
    public List<Object[]> getAgeDistribution() {
        log.debug("Getting age distribution");
        return userProfileRepository.getAgeDistribution();
    }

    /**
     * Calculate user age from profile
     *
     * @param profile Profile to calculate age for
     * @return Age in years, or null if date of birth not available
     */
    public Integer calculateAge(UserProfile profile) {
        if (profile.getDateOfBirth() == null) {
            return null;
        }

        return Period.between(profile.getDateOfBirth(), LocalDate.now()).getYears();
    }

    // === Data Quality and Validation ===

    /**
     * Find profiles with invalid dates
     *
     * @return List of profiles with invalid data
     */
    @Transactional(readOnly = true)
    public List<UserProfile> findProfilesWithInvalidDates() {
        log.debug("Finding profiles with invalid dates");
        return userProfileRepository.findProfilesWithInvalidDates();
    }

    /**
     * Find duplicate passport numbers
     *
     * @return List of passport numbers with duplicates
     */
    @Transactional(readOnly = true)
    public List<Object[]> findDuplicatePassportNumbers() {
        log.debug("Finding duplicate passport numbers - feature not implemented");
        return List.of(); // Return empty list as passport field doesn't exist
    }

    /**
     * Find verified profiles with missing critical data
     *
     * @return List of verified profiles with incomplete data
     */
    @Transactional(readOnly = true)
    public List<UserProfile> findVerifiedProfilesWithMissingData() {
        log.debug("Finding verified profiles with missing data");
        return userProfileRepository.findVerifiedProfilesWithMissingData();
    }

    // === Validation Methods ===

    /**
     * Validate profile data
     *
     * @param profile Profile to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateProfile(UserProfile profile) {
        if (profile.getUserId() == null) {
            throw new IllegalArgumentException("User ID is required");
        }

        if (profile.getDateOfBirth() != null) {
            if (profile.getDateOfBirth().isAfter(LocalDate.now())) {
                throw new IllegalArgumentException("Date of birth cannot be in the future");
            }

            int age = Period.between(profile.getDateOfBirth(), LocalDate.now()).getYears();
            if (age > 120) {
                throw new IllegalArgumentException("Invalid date of birth: age cannot exceed 120 years");
            }

            if (age < 13) {
                throw new IllegalArgumentException("User must be at least 13 years old");
            }
        }

        if (profile.getEmergencyContactPhone() != null &&
            !profile.getEmergencyContactPhone().matches("^\\+?[1-9]\\d{1,14}$")) {
            throw new IllegalArgumentException("Invalid emergency contact phone format");
        }

        if (profile.getBio() != null && profile.getBio().length() > 500) {
            throw new IllegalArgumentException("Bio cannot exceed 500 characters");
        }

        if (profile.getAvatarUrl() != null &&
            !profile.getAvatarUrl().matches("^https?://.*\\.(jpg|jpeg|png|gif)$")) {
            throw new IllegalArgumentException("Avatar URL must be a valid HTTP/HTTPS image URL");
        }
    }

    /**
     * Validate unique constraints
     *
     * @param profile Profile to validate
     * @throws IllegalArgumentException if constraint violation
     */
    private void validateUniqueConstraints(UserProfile profile) {
        if (userProfileRepository.existsByUserId(profile.getUserId().toString())) {
            throw new IllegalArgumentException("Profile already exists for user: " + profile.getUserId());
        }

        // Note: Passport validation removed as passport field doesn't exist in current entity
    }

    /**
     * Check if profile exists for user
     *
     * @param userId User ID to check
     * @return true if profile exists
     */
    @Transactional(readOnly = true)
    public boolean existsByUserId(String userId) {
        return userProfileRepository.existsByUserId(userId);
    }

    // === Missing Methods for Controller Support ===

    /**
     * Delete user profile by userId
     *
     * @param userId User ID
     * @param deletedBy User who performed the deletion
     * @return true if profile was deleted
     */
    @CacheEvict(value = "userProfiles", key = "#userId")
    public boolean deleteProfile(String userId, String deletedBy) {
        log.info("Deleting profile for user: {} by: {}", userId, deletedBy);

        Optional<UserProfile> profileOpt = userProfileRepository.findByUserId(userId);
        if (profileOpt.isEmpty()) {
            throw new IllegalArgumentException("Profile not found for user: " + userId);
        }

        UserProfile profile = profileOpt.get();
        profile.setUpdatedBy(deletedBy);
        profile.setUpdatedAt(OffsetDateTime.now());

        userProfileRepository.delete(profile);
        return true;
    }

    /**
     * Verify user profile
     *
     * @param userId User ID
     * @param verificationMethod Method used for verification
     * @param verifiedBy User who performed verification
     * @return Updated profile
     */
    @CacheEvict(value = "userProfiles", key = "#userId")
    public UserProfile verifyProfile(String userId, String verificationMethod, String verifiedBy) {
        log.info("Verifying profile for user: {} with method: {} by: {}", userId, verificationMethod, verifiedBy);

        Optional<UserProfile> profileOpt = findByUserId(userId);
        if (profileOpt.isEmpty()) {
            throw new IllegalArgumentException("Profile not found for user: " + userId);
        }

        UserProfile profile = profileOpt.get();
        // setVerified method doesn't exist - using verification_status instead
        profile.setVerificationStatus("VERIFIED");
        profile.setVerifiedAt(OffsetDateTime.now());
        profile.setUpdatedBy(verifiedBy);
        profile.setUpdatedAt(OffsetDateTime.now());

        return userProfileRepository.save(profile);
    }

    /**
     * Unverify user profile
     *
     * @param userId User ID
     * @param unverifiedBy User who removed verification
     * @return Updated profile
     */
    @CacheEvict(value = "userProfiles", key = "#userId")
    public UserProfile unverifyProfile(String userId, String unverifiedBy) {
        log.info("Removing verification for user: {} by: {}", userId, unverifiedBy);

        Optional<UserProfile> profileOpt = findByUserId(userId);
        if (profileOpt.isEmpty()) {
            throw new IllegalArgumentException("Profile not found for user: " + userId);
        }

        UserProfile profile = profileOpt.get();
        // setVerified method doesn't exist - using verification_status instead
        profile.setVerificationStatus("UNVERIFIED");
        profile.setVerifiedAt(null);
        profile.setUpdatedBy(unverifiedBy);
        profile.setUpdatedAt(OffsetDateTime.now());

        return userProfileRepository.save(profile);
    }

    /**
     * Find verified profiles
     *
     * @param pageable Pagination parameters
     * @return Page of verified profiles
     */
    @Transactional(readOnly = true)
    public Page<UserProfile> findVerifiedProfiles(Pageable pageable) {
        log.debug("Finding verified profiles");
        return userProfileRepository.findByVerifiedTrue(pageable);
    }

    /**
     * Find unverified profiles
     *
     * @param pageable Pagination parameters
     * @return Page of unverified profiles
     */
    @Transactional(readOnly = true)
    public Page<UserProfile> findUnverifiedProfiles(Pageable pageable) {
        log.debug("Finding unverified profiles");
        return userProfileRepository.findByVerifiedFalse(pageable);
    }

    /**
     * Get profile demographics
     *
     * @return Demographic statistics
     */
    @Transactional(readOnly = true)
    public Object getProfileDemographics() {
        log.debug("Getting profile demographics");
        return userProfileRepository.getProfileStatistics();
    }

    /**
     * Get verification statistics
     *
     * @return Verification statistics
     */
    @Transactional(readOnly = true)
    public Object getVerificationStatistics() {
        log.debug("Getting verification statistics");

        long totalProfiles = userProfileRepository.count();
        long verifiedProfiles = userProfileRepository.countByVerifiedTrue();
        long unverifiedProfiles = totalProfiles - verifiedProfiles;

        return new Object() {
            public final long total = totalProfiles;
            public final long verified = verifiedProfiles;
            public final long unverified = unverifiedProfiles;
            public final double verificationRate = totalProfiles > 0 ? (double) verifiedProfiles / totalProfiles * 100 : 0;
        };
    }

    /**
     * Count verified profiles
     *
     * @return Number of verified profiles
     */
    @Transactional(readOnly = true)
    public long countVerifiedProfiles() {
        log.debug("Counting verified profiles");
        return userProfileRepository.countByVerifiedTrue();
    }

    /**
     * Count profiles by nationality
     *
     * @param nationality Nationality to count
     * @return Number of profiles with specified nationality
     */
    @Transactional(readOnly = true)
    public long countByNationality(String nationality) {
        log.debug("Counting profiles by nationality: {}", nationality);
        return userProfileRepository.countByNationality(nationality);
    }

    /**
     * Search profiles by controller parameters (overloaded method)
     *
     * @param nationality Nationality filter
     * @param country Country filter
     * @param occupation Occupation filter
     * @param verified Verification status filter
     * @param pageable Pagination parameters
     * @return Page of matching profiles
     */
    @Transactional(readOnly = true)
    public Page<UserProfile> searchProfiles(String nationality, String country, String occupation, Boolean verified, Pageable pageable) {
        log.debug("Searching profiles with controller filters - nationality: {}, country: {}, occupation: {}, verified: {}",
                nationality, country, occupation, verified);

        // Convert Boolean verified to String verificationStatus
        String verificationStatus = null;
        if (verified != null) {
            verificationStatus = verified ? "VERIFIED" : "UNVERIFIED";
        }

        // For now, map country to gender (this might need adjustment based on your actual data model)
        String gender = null; // country is not gender, so leaving as null

        // Since we don't have minAge/maxAge from controller, use null values
        Integer minAge = null;
        Integer maxAge = null;

        return searchProfiles(nationality, gender, verificationStatus, minAge, maxAge, pageable);
    }
}