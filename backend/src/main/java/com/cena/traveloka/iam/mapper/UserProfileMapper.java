package com.cena.traveloka.iam.mapper;

import com.cena.traveloka.iam.dto.request.UserProfileCreateReq;
import com.cena.traveloka.iam.dto.request.UserProfileUpdateReq;
import com.cena.traveloka.iam.dto.response.UserProfileRes;
import com.cena.traveloka.iam.entity.UserProfile;
import org.mapstruct.*;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Map;

/**
 * MapStruct mapper for UserProfile entity and DTOs
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface UserProfileMapper {

    // === Entity to Response DTOs ===

    /**
     * Map UserProfile entity to UserProfileRes DTO
     */
    @Mapping(target = "id", source = "id")
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "displayName", source = ".", qualifiedByName = "extractDisplayName")
    @Mapping(target = "bio", source = "bio")
    @Mapping(target = "avatarUrl", source = "avatarUrl")
    @Mapping(target = "nationality", source = "nationality")
    @Mapping(target = "occupation", source = ".", qualifiedByName = "extractOccupation")
    @Mapping(target = "company", source = ".", qualifiedByName = "extractCompany")
    @Mapping(target = "address", source = ".", qualifiedByName = "extractAddress")
    @Mapping(target = "city", source = ".", qualifiedByName = "extractCity")
    @Mapping(target = "state", source = ".", qualifiedByName = "extractState")
    @Mapping(target = "postalCode", source = ".", qualifiedByName = "extractPostalCode")
    @Mapping(target = "country", source = ".", qualifiedByName = "extractCountry")
    @Mapping(target = "emergencyContact", source = "emergencyContactPhone")
    @Mapping(target = "emergencyContactName", source = "emergencyContactName")
    @Mapping(target = "emergencyContactRelationship", source = ".", qualifiedByName = "extractEmergencyContactRelationship")
    @Mapping(target = "dateOfBirth", source = "dateOfBirth")
    @Mapping(target = "age", source = "dateOfBirth", qualifiedByName = "calculateAge")
    @Mapping(target = "maritalStatus", source = ".", qualifiedByName = "extractMaritalStatus")
    @Mapping(target = "travelDocuments", source = ".", qualifiedByName = "extractTravelDocuments")
    @Mapping(target = "loyaltyPrograms", source = ".", qualifiedByName = "extractLoyaltyPrograms")
    @Mapping(target = "customFields", source = "profileData")
    @Mapping(target = "verified", source = "verifiedAt", qualifiedByName = "isVerified")
    @Mapping(target = "verificationDate", source = "verifiedAt", qualifiedByName = "offsetDateTimeToInstant")
    @Mapping(target = "verificationMethod", source = "verificationStatus")
    @Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "offsetDateTimeToInstant")
    @Mapping(target = "updatedAt", source = "updatedAt", qualifiedByName = "offsetDateTimeToInstant")
    @Mapping(target = "createdBy", source = "createdBy")
    @Mapping(target = "updatedBy", source = "updatedBy")
    UserProfileRes toUserProfileRes(UserProfile profile);

    /**
     * Map list of UserProfile entities to list of UserProfileRes DTOs
     */
    List<UserProfileRes> toUserProfileResList(List<UserProfile> profiles);

    // === Request DTOs to Entity ===

    /**
     * Map UserProfileCreateReq DTO to UserProfile entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "bio", source = "bio")
    @Mapping(target = "avatarUrl", source = "avatarUrl")
    @Mapping(target = "nationality", source = "nationality")
    @Mapping(target = "dateOfBirth", source = "dateOfBirth")
    @Mapping(target = "emergencyContactName", source = "emergencyContactName")
    @Mapping(target = "emergencyContactPhone", source = "emergencyContact")
    @Mapping(target = "profileData", source = ".", qualifiedByName = "mapRequestToProfileData")
    @Mapping(target = "verifiedAt", ignore = true) // Set by service
    @Mapping(target = "verificationStatus", ignore = true)
    @Mapping(target = "gender", ignore = true) // Handle separately
    @Mapping(target = "createdAt", ignore = true) // Set by JPA
    @Mapping(target = "updatedAt", ignore = true) // Set by JPA
    @Mapping(target = "createdBy", ignore = true) // Set by service
    @Mapping(target = "updatedBy", ignore = true) // Set by service
    // No version field in entity - removed
    @Mapping(target = "user", ignore = true)
    UserProfile toUserProfile(UserProfileCreateReq request);

    /**
     * Update UserProfile entity from UserProfileUpdateReq DTO
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true) // Not updatable
    @Mapping(target = "bio", source = "bio")
    @Mapping(target = "avatarUrl", source = "avatarUrl")
    @Mapping(target = "nationality", source = "nationality")
    @Mapping(target = "dateOfBirth", source = "dateOfBirth")
    @Mapping(target = "emergencyContactName", source = "emergencyContactName")
    @Mapping(target = "emergencyContactPhone", source = "emergencyContact")
    @Mapping(target = "profileData", source = ".", qualifiedByName = "mapRequestToProfileData")
    @Mapping(target = "verifiedAt", source = "verified", qualifiedByName = "setVerificationTime")
    @Mapping(target = "verificationStatus", ignore = true) // Set by service when verified
    @Mapping(target = "gender", ignore = true) // Handle separately
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true) // Set by JPA
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true) // Set by service
    // No version field in entity - removed
    @Mapping(target = "user", ignore = true)
    void updateUserProfileFromDto(UserProfileUpdateReq request, @MappingTarget UserProfile profile);

    // === Custom Mapping Methods ===

    /**
     * Convert OffsetDateTime to Instant
     */
    @Named("offsetDateTimeToInstant")
    default Instant offsetDateTimeToInstant(OffsetDateTime offsetDateTime) {
        return offsetDateTime != null ? offsetDateTime.toInstant() : null;
    }

    /**
     * Convert OffsetDateTime to boolean (verified status)
     */
    @Named("isVerified")
    default Boolean isVerified(OffsetDateTime verifiedAt) {
        return verifiedAt != null;
    }

    /**
     * Set verification time when verified is true
     */
    @Named("setVerificationTime")
    default OffsetDateTime setVerificationTime(Boolean verified) {
        return verified != null && verified ? OffsetDateTime.now() : null;
    }

    /**
     * Calculate age from date of birth
     */
    @Named("calculateAge")
    default Integer calculateAge(LocalDate dateOfBirth) {
        if (dateOfBirth == null) {
            return null;
        }
        return Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

    /**
     * Extract display name from user profile
     */
    @Named("extractDisplayName")
    default String extractDisplayName(UserProfile profile) {
        if (profile == null || profile.getProfileData() == null) {
            return null;
        }
        Object displayName = profile.getProfileData().get("displayName");
        return displayName != null ? displayName.toString() : null;
    }

    /**
     * Extract occupation from profile data
     */
    @Named("extractOccupation")
    default String extractOccupation(UserProfile profile) {
        return extractStringFromProfileData(profile, "occupation");
    }

    /**
     * Extract company from profile data
     */
    @Named("extractCompany")
    default String extractCompany(UserProfile profile) {
        return extractStringFromProfileData(profile, "company");
    }

    /**
     * Extract address from profile data
     */
    @Named("extractAddress")
    default String extractAddress(UserProfile profile) {
        return extractStringFromProfileData(profile, "address");
    }

    /**
     * Extract city from profile data
     */
    @Named("extractCity")
    default String extractCity(UserProfile profile) {
        return extractStringFromProfileData(profile, "city");
    }

    /**
     * Extract state from profile data
     */
    @Named("extractState")
    default String extractState(UserProfile profile) {
        return extractStringFromProfileData(profile, "state");
    }

    /**
     * Extract postal code from profile data
     */
    @Named("extractPostalCode")
    default String extractPostalCode(UserProfile profile) {
        return extractStringFromProfileData(profile, "postalCode");
    }

    /**
     * Extract country from profile data
     */
    @Named("extractCountry")
    default String extractCountry(UserProfile profile) {
        return extractStringFromProfileData(profile, "country");
    }

    /**
     * Extract emergency contact relationship from profile data
     */
    @Named("extractEmergencyContactRelationship")
    default String extractEmergencyContactRelationship(UserProfile profile) {
        return extractStringFromProfileData(profile, "emergencyContactRelationship");
    }

    /**
     * Extract marital status from profile data
     */
    @Named("extractMaritalStatus")
    default String extractMaritalStatus(UserProfile profile) {
        return extractStringFromProfileData(profile, "maritalStatus");
    }

    /**
     * Extract travel documents from profile data
     */
    @Named("extractTravelDocuments")
    @SuppressWarnings("unchecked")
    default java.util.Map<String, Object> extractTravelDocuments(UserProfile profile) {
        if (profile == null || profile.getProfileData() == null) {
            return null;
        }
        Object travelDocuments = profile.getProfileData().get("travelDocuments");
        return travelDocuments instanceof java.util.Map ? (java.util.Map<String, Object>) travelDocuments : null;
    }

    /**
     * Extract loyalty programs from profile data
     */
    @Named("extractLoyaltyPrograms")
    @SuppressWarnings("unchecked")
    default java.util.Map<String, Object> extractLoyaltyPrograms(UserProfile profile) {
        if (profile == null || profile.getProfileData() == null) {
            return null;
        }
        Object loyaltyPrograms = profile.getProfileData().get("loyaltyPrograms");
        return loyaltyPrograms instanceof java.util.Map ? (java.util.Map<String, Object>) loyaltyPrograms : null;
    }

    /**
     * Map request to profile data (for create/update requests)
     */
    @Named("mapRequestToProfileData")
    default java.util.Map<String, Object> mapRequestToProfileData(Object request) {
        // This will be handled in the service layer for complex mapping
        // Return null here as profileData will be set manually
        return null;
    }

    /**
     * Helper method to extract string values from profile data
     */
    default String extractStringFromProfileData(UserProfile profile, String key) {
        if (profile == null || profile.getProfileData() == null) {
            return null;
        }
        Object value = profile.getProfileData().get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * After mapping method for verification handling
     */
    @AfterMapping
    default void afterMapping(@MappingTarget UserProfile profile, UserProfileUpdateReq request) {
        if (profile != null && request != null && request.getVerified() != null && request.getVerified()) {
            // Set verification timestamp when marking as verified
            if (profile.getVerifiedAt() == null) {
                profile.setVerifiedAt(OffsetDateTime.now());
                profile.setVerificationStatus("MANUAL");
            }
        }
    }

    /**
     * Before mapping method for audit trail
     */
    @BeforeMapping
    default void beforeMapping(UserProfileCreateReq request, @MappingTarget UserProfile profile) {
        if (profile != null) {
            profile.setCreatedAt(OffsetDateTime.now());
            profile.setUpdatedAt(OffsetDateTime.now());
        }
    }

    @BeforeMapping
    default void beforeMapping(UserProfileUpdateReq request, @MappingTarget UserProfile profile) {
        if (profile != null) {
            profile.setUpdatedAt(OffsetDateTime.now());
        }
    }
}