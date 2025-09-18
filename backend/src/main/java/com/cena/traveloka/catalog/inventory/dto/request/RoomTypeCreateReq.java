package com.cena.traveloka.catalog.inventory.dto.request;

import com.cena.traveloka.catalog.inventory.entity.RoomType;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomTypeCreateReq {
    @NotNull(message = "Property ID is required")
    UUID propertyId;

    @NotBlank(message = "Type code is required")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "Type code can only contain letters, numbers, underscores, and hyphens")
    @Size(max = 20, message = "Type code must not exceed 20 characters")
    String typeCode;

    @NotBlank(message = "Type name is required")
    @Size(max = 100, message = "Type name must not exceed 100 characters")
    String typeName;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    String description;

    @NotNull(message = "Maximum occupancy is required")
    @Min(value = 1, message = "Maximum occupancy must be at least 1")
    @Max(value = 10, message = "Maximum occupancy cannot exceed 10")
    Integer maxOccupancy;

    @NotNull(message = "Adult capacity is required")
    @Min(value = 1, message = "Adult capacity must be at least 1")
    @Max(value = 8, message = "Adult capacity cannot exceed 8")
    Integer adultCapacity;

    @Min(value = 0, message = "Child capacity cannot be negative")
    @Max(value = 4, message = "Child capacity cannot exceed 4")
    Integer childCapacity;

    @Min(value = 0, message = "Infant capacity cannot be negative")
    @Max(value = 2, message = "Infant capacity cannot exceed 2")
    Integer infantCapacity;

    @NotNull(message = "Base price is required")
    @DecimalMin(value = "0.00", message = "Base price must be non-negative")
    BigDecimal basePrice;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be 3 characters")
    String currency;

    Boolean isRefundable;

    @DecimalMin(value = "0.00", message = "Room size must be non-negative")
    BigDecimal roomSize;

    @Size(max = 50, message = "Bed type must not exceed 50 characters")
    String bedType;

    RoomType.SmokingPolicy smokingPolicy;
}
