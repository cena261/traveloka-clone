package com.cena.traveloka.iam.mapper;

import com.cena.traveloka.iam.dto.response.ProfileDto;
import com.cena.traveloka.iam.entity.UserProfile;
import org.mapstruct.*;

import java.math.BigDecimal;

@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ProfileMapper {

    @Mapping(target = "totalSpent", expression = "java(convertBigDecimalToDouble(profile.getTotalSpent()))")
    ProfileDto toDto(UserProfile profile);

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

    default Double convertBigDecimalToDouble(BigDecimal value) {
        return value != null ? value.doubleValue() : null;
    }

    default BigDecimal convertDoubleToBigDecimal(Double value) {
        return value != null ? BigDecimal.valueOf(value) : null;
    }
}
