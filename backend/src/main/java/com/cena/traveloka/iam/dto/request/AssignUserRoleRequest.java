package com.cena.traveloka.iam.dto.request;

import com.cena.traveloka.iam.validation.ValidUserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.Instant;

/**
 * Request DTO for assigning roles to users
 */
@Data
@ValidUserRole
public class AssignUserRoleRequest {

    @NotBlank(message = "User ID is required")
    private String userId;

    @NotBlank(message = "Role name is required")
    @Size(max = 50, message = "Role name must not exceed 50 characters")
    private String roleName;

    @Size(max = 500, message = "Assignment reason must not exceed 500 characters")
    private String reason;

    private Instant validFrom;

    private Instant validUntil;

    @NotNull(message = "Active status is required")
    private Boolean active = true;

    @Size(max = 255, message = "Assigned by must not exceed 255 characters")
    private String assignedBy;
}