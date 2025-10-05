package com.cena.traveloka.iam.dto.response;

import com.cena.traveloka.common.enums.Gender;
import com.cena.traveloka.common.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDetailDto {

    private UUID id;

    private UUID keycloakId;

    private String username;

    private String email;

    private String phone;

    private String firstName;

    private String lastName;

    private String displayName;

    private String avatarUrl;

    private LocalDate dateOfBirth;

    private Gender gender;

    private String nationality;

    private String preferredLanguage;

    private String preferredCurrency;

    private String timezone;

    private Status status;

    private Boolean emailVerified;

    private Boolean phoneVerified;

    private Boolean twoFactorEnabled;

    private Boolean accountLocked;

    private String lockReason;

    private OffsetDateTime lockedUntil;

    private OffsetDateTime lastLoginAt;

    private Integer loginCount;

    private OffsetDateTime createdAt;

    private List<RoleDto> roles;

    private ProfileDto profile;
}
