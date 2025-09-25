package com.cena.traveloka.iam.dto.request;

import com.cena.traveloka.iam.entity.AppUser;
import com.cena.traveloka.iam.enums.UserStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * Request DTO for searching users with filters
 */
@Data
public class UserSearchReq {

    private String email;

    private String firstName;

    private String lastName;

    private UserStatus status;

    @Min(value = 0, message = "Minimum completeness must be at least 0")
    @Max(value = 100, message = "Minimum completeness must be at most 100")
    private Integer minCompleteness;

    private String country;

    private String preferredLanguage;

    private Boolean emailVerified;

    private Boolean phoneVerified;

    @Min(value = 0, message = "Page must be non-negative")
    private Integer page = 0;

    @Min(value = 1, message = "Size must be at least 1")
    @Max(value = 100, message = "Size must be at most 100")
    private Integer size = 20;

    private String sortBy = "createdAt";

    private String sortDirection = "DESC";
}