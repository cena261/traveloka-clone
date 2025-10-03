package com.cena.traveloka.iam.dto.response;

import com.cena.traveloka.common.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * T036: UserDto
 * Basic user information DTO for API responses.
 *
 * Constitutional Compliance:
 * - Principle IV: Entity Immutability - DTO separates from entity
 * - Used in API responses (login, user list, etc.)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {

    /**
     * User ID.
     */
    private UUID id;

    /**
     * Username.
     */
    private String username;

    /**
     * Email address.
     */
    private String email;

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
     * Account status (PENDING, ACTIVE, INACTIVE, SUSPENDED).
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
}
