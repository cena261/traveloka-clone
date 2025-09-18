package com.cena.traveloka.catalog.inventory.dto.request;

import com.cena.traveloka.catalog.inventory.entity.RoomType;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomTypeUpdateReq {
    @Size(max = 100, message = "Type name must not exceed 100 characters")
    String typeName;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    String description;

    @Min(value = 1, message = "Maximum occupancy must be at least 1")
    @Max(value = 10, message = "Maximum occupancy cannot exceed 10")
    Integer maxOccupancy;

    @Min(value = 1, message = "Adult capacity must be at least 1")
    @Max(value = 8, message = "Adult capacity cannot exceed 8")
    Integer adultCapacity;

    @Min(value = 0, message = "Child capacity cannot be negative")
    @Max(value = 4, message = "Child capacity cannot exceed 4")
    Integer childCapacity;

    @Min(value = 0, message = "Infant capacity cannot be negative")
    @Max(value = 2, message = "Infant capacity cannot exceed 2")
    Integer infantCapacity;

    @DecimalMin(value = "0.00", message = "Base price must be non-negative")
    BigDecimal basePrice;

    @Size(min = 3, max = 3, message = "Currency must be 3 characters")
    String currency;

    Boolean isRefundable;

    @DecimalMin(value = "0.00", message = "Room size must be non-negative")
    BigDecimal roomSize;

    @Size(max = 50, message = "Bed type must not exceed 50 characters")
    String bedType;

    RoomType.SmokingPolicy smokingPolicy;

    RoomType.RoomTypeStatus status;
}
