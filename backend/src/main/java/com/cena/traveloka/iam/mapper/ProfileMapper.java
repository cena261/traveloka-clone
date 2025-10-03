package com.cena.traveloka.iam.mapper;

import com.cena.traveloka.iam.dto.response.ProfileDto;
import com.cena.traveloka.iam.entity.UserProfile;
import org.mapstruct.*;

import java.math.BigDecimal;

/**
 * T043: ProfileMapper
 * MapStruct mapper for UserProfile entity ↔ ProfileDto conversion.
 *
 * Constitutional Compliance:
 * - Principle IV: Entity Immutability - Mapper separates entities from DTOs
 * - Principle X: Code Quality - MapStruct for type-safe mapping
 * - Used by UserMapper for nested profile mapping
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ProfileMapper {

    /**
     * Convert UserProfile entity to ProfileDto.
     * Used in UserDetailDto for extended profile information (FR-019).
     *
     * @param profile UserProfile entity
     * @return ProfileDto with profile information
     */
    @Mapping(target = "totalSpent", expression = "java(convertBigDecimalToDouble(profile.getTotalSpent()))")
    ProfileDto toDto(UserProfile profile);

    /**
     * Convert ProfileDto to UserProfile entity.
     * Used when creating new profile.
     *
     * @param dto ProfileDto
     * @return UserProfile entity
     */
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "totalSpent", expression = "java(convertDoubleToBigDecimal(dto.getTotalSpent()))")
    @Mapping(target = "notificationPreferences", ignore = true)
    @Mapping(target = "privacySettings", ignore = true)
    @Mapping(target = "accessibilitySettings", ignore = true)
    @Mapping(target = "facebookUrl", ignore = true)
    @Mapping(target = "twitterUrl", ignore = true)
    @Mapping(target = "linkedinUrl", ignore = true)
    @Mapping(target = "instagramUrl", ignore = true)
    @Mapping(target = "frequentFlyerNumbers", ignore = true)
    @Mapping(target = "hotelLoyaltyPrograms", ignore = true)
    @Mapping(target = "memberSince", ignore = true)
    @Mapping(target = "lastProfileUpdate", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    UserProfile toEntity(ProfileDto dto);

    /**
     * Update UserProfile entity from ProfileDto.
     * Only updates non-null fields.
     *
     * @param dto ProfileDto with updated values
     * @param profile Existing UserProfile entity to update
     */
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "totalSpent", expression = "java(convertDoubleToBigDecimal(dto.getTotalSpent()))")
    @Mapping(target = "totalBookings", ignore = true)
    @Mapping(target = "loyaltyPoints", ignore = true)
    @Mapping(target = "loyaltyTier", ignore = true)
    @Mapping(target = "profileCompletionPercentage", ignore = true)
    @Mapping(target = "notificationPreferences", ignore = true)
    @Mapping(target = "privacySettings", ignore = true)
    @Mapping(target = "accessibilitySettings", ignore = true)
    @Mapping(target = "frequentFlyerNumbers", ignore = true)
    @Mapping(target = "hotelLoyaltyPrograms", ignore = true)
    @Mapping(target = "facebookUrl", ignore = true)
    @Mapping(target = "twitterUrl", ignore = true)
    @Mapping(target = "linkedinUrl", ignore = true)
    @Mapping(target = "instagramUrl", ignore = true)
    @Mapping(target = "memberSince", ignore = true)
    @Mapping(target = "lastProfileUpdate", expression = "java(java.time.OffsetDateTime.now())")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", expression = "java(java.time.OffsetDateTime.now())")
    void updateFromDto(ProfileDto dto, @MappingTarget UserProfile profile);

    /**
     * Helper method to convert BigDecimal to Double.
     * UserProfile.totalSpent is BigDecimal, ProfileDto.totalSpent is Double.
     *
     * @param value BigDecimal value
     * @return Double value or null
     */
    default Double convertBigDecimalToDouble(BigDecimal value) {
        return value != null ? value.doubleValue() : null;
    }

    /**
     * Helper method to convert Double to BigDecimal.
     * ProfileDto.totalSpent is Double, UserProfile.totalSpent is BigDecimal.
     *
     * @param value Double value
     * @return BigDecimal value or null
     */
    default BigDecimal convertDoubleToBigDecimal(Double value) {
        return value != null ? BigDecimal.valueOf(value) : null;
    }
}
