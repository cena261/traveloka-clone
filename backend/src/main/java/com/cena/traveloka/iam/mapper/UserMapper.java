package com.cena.traveloka.iam.mapper;

import com.cena.traveloka.iam.dto.request.RegisterRequest;
import com.cena.traveloka.iam.dto.request.UpdateProfileRequest;
import com.cena.traveloka.iam.dto.response.UserDetailDto;
import com.cena.traveloka.iam.dto.response.UserDto;
import com.cena.traveloka.iam.entity.User;
import org.mapstruct.*;

@Mapper(
    componentModel = "spring",
    uses = {RoleMapper.class, ProfileMapper.class},
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface UserMapper {

    UserDto toDto(User user);

    @Mapping(target = "roles", source = "roles")
    @Mapping(target = "profile", source = "profile")
    UserDetailDto toDetailDto(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "keycloakId", ignore = true)
    @Mapping(target = "displayName", expression = "java(request.getFirstName() + \" \" + request.getLastName())")
    @Mapping(target = "status", constant = "pending")
    @Mapping(target = "emailVerified", constant = "false")
    @Mapping(target = "phoneVerified", constant = "false")
    @Mapping(target = "twoFactorEnabled", constant = "false")
    @Mapping(target = "accountLocked", constant = "false")
    @Mapping(target = "lockReason", ignore = true)
    @Mapping(target = "lockedUntil", ignore = true)
    @Mapping(target = "lastLoginAt", ignore = true)
    @Mapping(target = "lastLoginIp", ignore = true)
    @Mapping(target = "loginCount", constant = "0")
    @Mapping(target = "failedLoginAttempts", constant = "0")
    @Mapping(target = "passwordChangedAt", ignore = true)
    @Mapping(target = "termsAcceptedAt", expression = "java(java.time.OffsetDateTime.now())")
    @Mapping(target = "privacyAcceptedAt", expression = "java(java.time.OffsetDateTime.now())")
    @Mapping(target = "marketingConsent", constant = "false")
    @Mapping(target = "createdAt", expression = "java(java.time.OffsetDateTime.now())")
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", constant = "system")
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "isDeleted", constant = "false")
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "profile", ignore = true)
    @Mapping(target = "addresses", ignore = true)
    @Mapping(target = "documents", ignore = true)
    @Mapping(target = "sessions", ignore = true)
    @Mapping(target = "roles", ignore = true)
    User toEntity(RegisterRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "keycloakId", ignore = true)
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "displayName", expression = "java(buildDisplayName(request.getFirstName(), request.getLastName(), user))")
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "emailVerified", ignore = true)
    @Mapping(target = "phoneVerified", ignore = true)
    @Mapping(target = "twoFactorEnabled", ignore = true)
    @Mapping(target = "accountLocked", ignore = true)
    @Mapping(target = "lockReason", ignore = true)
    @Mapping(target = "lockedUntil", ignore = true)
    @Mapping(target = "lastLoginAt", ignore = true)
    @Mapping(target = "lastLoginIp", ignore = true)
    @Mapping(target = "loginCount", ignore = true)
    @Mapping(target = "failedLoginAttempts", ignore = true)
    @Mapping(target = "passwordChangedAt", ignore = true)
    @Mapping(target = "termsAcceptedAt", ignore = true)
    @Mapping(target = "privacyAcceptedAt", ignore = true)
    @Mapping(target = "marketingConsent", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", expression = "java(java.time.OffsetDateTime.now())")
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "profile", ignore = true)
    @Mapping(target = "addresses", ignore = true)
    @Mapping(target = "documents", ignore = true)
    @Mapping(target = "sessions", ignore = true)
    @Mapping(target = "roles", ignore = true)
    void updateFromRequest(UpdateProfileRequest request, @MappingTarget User user);

    default String buildDisplayName(String newFirstName, String newLastName, User existingUser) {
        String firstName = newFirstName != null ? newFirstName : existingUser.getFirstName();
        String lastName = newLastName != null ? newLastName : existingUser.getLastName();
        return firstName + " " + lastName;
    }
}
