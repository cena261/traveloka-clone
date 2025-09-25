package com.cena.traveloka.iam.dto.response;

import com.cena.traveloka.iam.entity.AppUser;
import com.cena.traveloka.iam.enums.UserStatus;
import lombok.Data;

import java.time.Instant;

/**
 * Response DTO for user summary information (used in lists)
 */
@Data
public class UserSummaryRes {

    private String id;

    private String email;

    private String firstName;

    private String lastName;

    private UserStatus status;

    private Integer profileCompleteness;

    private Instant lastLoginAt;

    private Instant createdAt;
}