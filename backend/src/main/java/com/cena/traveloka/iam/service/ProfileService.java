package com.cena.traveloka.iam.service;

import com.cena.traveloka.iam.entity.User;
import com.cena.traveloka.iam.entity.UserProfile;
import com.cena.traveloka.iam.repository.UserProfileRepository;
import com.cena.traveloka.iam.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ProfileService {

    private final UserProfileRepository profileRepository;
    private final UserRepository userRepository;

    public UserProfile createProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        if (profileRepository.existsByUserId(userId)) {
            throw new RuntimeException("Profile already exists for user: " + userId);
        }

        UserProfile profile = UserProfile.builder()
                .userId(userId)
                .user(user)
                .totalBookings(0)
                .totalSpent(BigDecimal.ZERO)
                .memberSince(LocalDate.now())
                .loyaltyPoints(0)
                .loyaltyTier("bronze")
                .profileCompletionPercentage(calculateCompletionPercentage(null))
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        UserProfile saved = profileRepository.save(profile);

        log.info("Profile created for user: {}", userId);
        return saved;
    }

    @Transactional(readOnly = true)
    public UserProfile getProfile(UUID userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Profile not found for user: " + userId));
    }

    public UserProfile updateProfile(UUID userId, UserProfile profile) {
        UserProfile existingProfile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Profile not found for user: " + userId));

        if (profile.getMiddleName() != null) existingProfile.setMiddleName(profile.getMiddleName());
        if (profile.getNickname() != null) existingProfile.setNickname(profile.getNickname());
        if (profile.getBio() != null) existingProfile.setBio(profile.getBio());
        if (profile.getOccupation() != null) existingProfile.setOccupation(profile.getOccupation());
        if (profile.getCompany() != null) existingProfile.setCompany(profile.getCompany());
        if (profile.getWebsite() != null) existingProfile.setWebsite(profile.getWebsite());
        if (profile.getFacebookUrl() != null) existingProfile.setFacebookUrl(profile.getFacebookUrl());
        if (profile.getTwitterUrl() != null) existingProfile.setTwitterUrl(profile.getTwitterUrl());
        if (profile.getLinkedinUrl() != null) existingProfile.setLinkedinUrl(profile.getLinkedinUrl());
        if (profile.getInstagramUrl() != null) existingProfile.setInstagramUrl(profile.getInstagramUrl());
        if (profile.getSpecialNeeds() != null) existingProfile.setSpecialNeeds(profile.getSpecialNeeds());

        if (profile.getNotificationPreferences() != null) {
            existingProfile.setNotificationPreferences(profile.getNotificationPreferences());
        }
        if (profile.getPrivacySettings() != null) {
            existingProfile.setPrivacySettings(profile.getPrivacySettings());
        }
        if (profile.getAccessibilitySettings() != null) {
            existingProfile.setAccessibilitySettings(profile.getAccessibilitySettings());
        }

        existingProfile.setProfileCompletionPercentage(calculateCompletionPercentage(existingProfile));
        existingProfile.setLastProfileUpdate(OffsetDateTime.now());
        existingProfile.setUpdatedAt(OffsetDateTime.now());

        UserProfile saved = profileRepository.save(existingProfile);

        log.info("Profile updated for user: {}", userId);
        return saved;
    }

    public void updatePreferredAirlines(UUID userId, List<String> airlines) {
        UserProfile profile = getProfile(userId);
        profile.setPreferredAirlines(airlines);
        profile.setLastProfileUpdate(OffsetDateTime.now());
        profile.setUpdatedAt(OffsetDateTime.now());
        profileRepository.save(profile);

        log.info("Updated preferred airlines for user: {}", userId);
    }

    public void updatePreferredHotels(UUID userId, List<String> hotels) {
        UserProfile profile = getProfile(userId);
        profile.setPreferredHotels(hotels);
        profile.setLastProfileUpdate(OffsetDateTime.now());
        profile.setUpdatedAt(OffsetDateTime.now());
        profileRepository.save(profile);

        log.info("Updated preferred hotels for user: {}", userId);
    }

    public void updateDietaryRestrictions(UUID userId, List<String> restrictions) {
        UserProfile profile = getProfile(userId);
        profile.setDietaryRestrictions(restrictions);
        profile.setLastProfileUpdate(OffsetDateTime.now());
        profile.setUpdatedAt(OffsetDateTime.now());
        profileRepository.save(profile);

        log.info("Updated dietary restrictions for user: {}", userId);
    }

    public void addLoyaltyPoints(UUID userId, int points) {
        UserProfile profile = getProfile(userId);
        profile.setLoyaltyPoints(profile.getLoyaltyPoints() + points);

        String newTier = calculateLoyaltyTier(profile.getLoyaltyPoints());
        if (!newTier.equals(profile.getLoyaltyTier())) {
            profile.setLoyaltyTier(newTier);
            log.info("User {} promoted to {} tier", userId, newTier);
        }

        profile.setUpdatedAt(OffsetDateTime.now());
        profileRepository.save(profile);

        log.info("Added {} loyalty points to user: {}", points, userId);
    }

    public void updateBookingStats(UUID userId, BigDecimal bookingAmount) {
        UserProfile profile = getProfile(userId);
        profile.setTotalBookings(profile.getTotalBookings() + 1);
        profile.setTotalSpent(profile.getTotalSpent().add(bookingAmount));
        profile.setUpdatedAt(OffsetDateTime.now());
        profileRepository.save(profile);

        log.info("Updated booking stats for user: {}", userId);
    }

    @Transactional(readOnly = true)
    public boolean profileExists(UUID userId) {
        return profileRepository.existsByUserId(userId);
    }

    public void deleteProfile(UUID userId) {
        UserProfile profile = getProfile(userId);
        profileRepository.delete(profile);

        log.info("Profile deleted for user: {}", userId);
    }

    private int calculateCompletionPercentage(UserProfile profile) {
        if (profile == null) return 10; // Base 10% for just creating profile

        int totalFields = 15;
        int completedFields = 0;

        if (profile.getMiddleName() != null && !profile.getMiddleName().isEmpty()) completedFields++;
        if (profile.getNickname() != null && !profile.getNickname().isEmpty()) completedFields++;
        if (profile.getBio() != null && !profile.getBio().isEmpty()) completedFields++;
        if (profile.getOccupation() != null && !profile.getOccupation().isEmpty()) completedFields++;
        if (profile.getCompany() != null && !profile.getCompany().isEmpty()) completedFields++;
        if (profile.getWebsite() != null && !profile.getWebsite().isEmpty()) completedFields++;
        if (profile.getFacebookUrl() != null && !profile.getFacebookUrl().isEmpty()) completedFields++;
        if (profile.getTwitterUrl() != null && !profile.getTwitterUrl().isEmpty()) completedFields++;
        if (profile.getLinkedinUrl() != null && !profile.getLinkedinUrl().isEmpty()) completedFields++;
        if (profile.getInstagramUrl() != null && !profile.getInstagramUrl().isEmpty()) completedFields++;
        if (profile.getPreferredAirlines() != null && !profile.getPreferredAirlines().isEmpty()) completedFields++;
        if (profile.getPreferredHotels() != null && !profile.getPreferredHotels().isEmpty()) completedFields++;
        if (profile.getDietaryRestrictions() != null && !profile.getDietaryRestrictions().isEmpty()) completedFields++;
        if (profile.getNotificationPreferences() != null) completedFields++;
        if (profile.getPrivacySettings() != null) completedFields++;

        return (int) ((completedFields / (double) totalFields) * 100);
    }

    private String calculateLoyaltyTier(int points) {
        if (points >= 10000) return "platinum";
        if (points >= 5000) return "gold";
        if (points >= 1000) return "silver";
        return "bronze";
    }
}
