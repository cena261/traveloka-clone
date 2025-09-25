package com.cena.traveloka.iam.mapper;

import com.cena.traveloka.iam.dto.request.UserCreateReq;
import com.cena.traveloka.iam.dto.request.UserUpdateReq;
import com.cena.traveloka.iam.dto.response.UserRes;
import com.cena.traveloka.iam.dto.response.UserSummaryRes;
import com.cena.traveloka.iam.entity.AppUser;
import org.mapstruct.*;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * MapStruct mapper for User entity and DTOs
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface UserMapper {

    // === Entity to Response DTOs ===

    /**
     * Map AppUser entity to UserRes DTO
     */
    @Mapping(target = "id", source = "id")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "firstName", source = "firstName")
    @Mapping(target = "lastName", source = "lastName")
    @Mapping(target = "phoneNumber", source = "phoneNumber")
    @Mapping(target = "dateOfBirth", ignore = true) // Not in AppUser entity
    @Mapping(target = "gender", ignore = true) // Not in AppUser entity
    @Mapping(target = "country", ignore = true) // Not in AppUser entity
    @Mapping(target = "preferredLanguage", source = "locale")
    @Mapping(target = "timeZone", ignore = true) // Not in AppUser entity
    @Mapping(target = "status", source = "status")
    @Mapping(target = "emailVerified", ignore = true) // Not in AppUser entity
    @Mapping(target = "phoneVerified", ignore = true) // Not in AppUser entity
    @Mapping(target = "profileCompleteness", source = "profileCompleteness")
    @Mapping(target = "lastLoginAt", ignore = true) // Not in AppUser entity
    @Mapping(target = "lastLoginIp", ignore = true) // Not in AppUser entity
    @Mapping(target = "lastSyncAt", source = "lastSyncAt", qualifiedByName = "offsetDateTimeToInstant")
    @Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "offsetDateTimeToInstant")
    @Mapping(target = "updatedAt", source = "updatedAt", qualifiedByName = "offsetDateTimeToInstant")
    @Mapping(target = "createdBy", ignore = true) // Not in AppUser entity
    @Mapping(target = "updatedBy", ignore = true) // Not in AppUser entity
    UserRes toUserRes(AppUser user);

    /**
     * Map AppUser entity to UserSummaryRes DTO
     */
    @Mapping(target = "id", source = "id")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "firstName", source = "firstName")
    @Mapping(target = "lastName", source = "lastName")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "profileCompleteness", source = "profileCompleteness")
    @Mapping(target = "lastLoginAt", ignore = true) // Not in AppUser entity
    @Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "offsetDateTimeToInstant")
    UserSummaryRes toUserSummaryRes(AppUser user);

    /**
     * Map list of AppUser entities to list of UserRes DTOs
     */
    List<UserRes> toUserResList(List<AppUser> users);

    /**
     * Map list of AppUser entities to list of UserSummaryRes DTOs
     */
    List<UserSummaryRes> toUserSummaryResList(List<AppUser> users);

    // === Request DTOs to Entity ===

    /**
     * Map UserCreateReq DTO to AppUser entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "email", source = "email")
    @Mapping(target = "firstName", source = "firstName")
    @Mapping(target = "lastName", source = "lastName")
    @Mapping(target = "phoneNumber", source = "phoneNumber")
    @Mapping(target = "keycloakId", source = "keycloakId")
    @Mapping(target = "status", ignore = true) // Set by service
    @Mapping(target = "profileCompleteness", ignore = true) // Calculated by entity
    @Mapping(target = "lastSyncAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true) // Set by JPA
    @Mapping(target = "updatedAt", ignore = true) // Set by JPA
    @Mapping(target = "username", ignore = true) // Set by service
    @Mapping(target = "displayName", ignore = true) // Set by service
    @Mapping(target = "locale", ignore = true) // Set by service
    @Mapping(target = "isActive", ignore = true) // Set by service
    AppUser toAppUser(UserCreateReq request);

    /**
     * Update AppUser entity from UserUpdateReq DTO
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "email", source = "email")
    @Mapping(target = "firstName", source = "firstName")
    @Mapping(target = "lastName", source = "lastName")
    @Mapping(target = "phoneNumber", source = "phoneNumber")
    @Mapping(target = "keycloakId", ignore = true) // Not updatable via this DTO
    @Mapping(target = "status", ignore = true) // Not updatable via this DTO
    @Mapping(target = "profileCompleteness", ignore = true) // Calculated by entity
    @Mapping(target = "lastSyncAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true) // Set by JPA
    @Mapping(target = "username", ignore = true) // Not updatable via this DTO
    @Mapping(target = "displayName", ignore = true) // Not updatable via this DTO
    @Mapping(target = "locale", ignore = true) // Not updatable via this DTO
    @Mapping(target = "isActive", ignore = true) // Not updatable via this DTO
    void updateAppUserFromDto(UserUpdateReq request, @MappingTarget AppUser user);

    // === Custom Mapping Methods ===

    /**
     * Convert OffsetDateTime to Instant
     */
    @Named("offsetDateTimeToInstant")
    default Instant offsetDateTimeToInstant(OffsetDateTime offsetDateTime) {
        return offsetDateTime != null ? offsetDateTime.toInstant() : null;
    }

    /**
     * After mapping method to ensure profile completeness calculation
     */
    @AfterMapping
    default void afterMapping(@MappingTarget AppUser user) {
        if (user != null) {
            // Note: updateProfileCompleteness() method doesn't exist in our entity
            // This would need to be implemented in the service layer
        }
    }

    /**
     * Before mapping method for audit trail
     */
    @BeforeMapping
    default void beforeMapping(UserCreateReq request, @MappingTarget AppUser user) {
        if (user != null) {
            user.setCreatedAt(OffsetDateTime.now());
            user.setUpdatedAt(OffsetDateTime.now());
        }
    }

    @BeforeMapping
    default void beforeMapping(UserUpdateReq request, @MappingTarget AppUser user) {
        if (user != null) {
            user.setUpdatedAt(OffsetDateTime.now());
        }
    }
}
