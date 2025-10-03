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

/**
 * T037: UserDetailDto
 * Detailed user information DTO including profile and roles.
 *
 * Constitutional Compliance:
 * - Principle IV: Entity Immutability - DTO separates from entity
 * - Used in GET /api/v1/users/me and admin endpoints
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDetailDto {

    /**
     * User ID.
     */
    private UUID id;

    /**
     * Keycloak user ID.
     */
    private UUID keycloakId;

    /**
     * Username.
     */
    private String username;

    /**
     * Email address.
     */
    private String email;

    /**
     * Phone number.
     */
    private String phone;

    /**
     * First name.
     */
    private String firstName;

    /**
     * Last name.
     */
    private String lastName;

    /**
     * Display name.
     */
    private String displayName;

    /**
     * Avatar URL.
     */
    private String avatarUrl;

    /**
     * Date of birth.
     */
    private LocalDate dateOfBirth;

    /**
     * Gender.
     */
    private Gender gender;

    /**
     * Nationality (ISO 3166-1 alpha-2).
     */
    private String nationality;

    /**
     * Preferred language.
     */
    private String preferredLanguage;

    /**
     * Preferred currency.
     */
    private String preferredCurrency;

    /**
     * Timezone.
     */
    private String timezone;

    /**
     * Account status.
     */
    private Status status;

    /**
     * Email verification status.
     */
    private Boolean emailVerified;

    /**
     * Phone verification status.
     */
    private Boolean phoneVerified;

    /**
     * Two-factor authentication enabled status.
     */
    private Boolean twoFactorEnabled;

    /**
     * Account locked status.
     */
    private Boolean accountLocked;

    /**
     * Lock reason (if locked).
     */
    private String lockReason;

    /**
     * Locked until timestamp (if locked).
     */
    private OffsetDateTime lockedUntil;

    /**
     * Last login timestamp.
     */
    private OffsetDateTime lastLoginAt;

    /**
     * Total login count.
     */
    private Integer loginCount;

    /**
     * Account creation timestamp.
     */
    private OffsetDateTime createdAt;

    /**
     * User roles.
     */
    private List<RoleDto> roles;

    /**
     * Extended profile information.
     */
    private ProfileDto profile;
}
