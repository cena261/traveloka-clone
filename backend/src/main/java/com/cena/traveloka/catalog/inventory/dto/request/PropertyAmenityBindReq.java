package com.cena.traveloka.catalog.inventory.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PropertyAmenityBindReq {
    @NotEmpty(message = "Amenity IDs are required")
    List<UUID> amenityIds;

    @NotEmpty(message = "Amenity configurations are required")
    @Size(min = 1, message = "At least one amenity configuration is required")
    List<AmenityConfig> amenityConfigs;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class AmenityConfig {
        Boolean isFree;

        @DecimalMin(value = "0.00", message = "Additional cost must be non-negative")
        BigDecimal additionalCost;

        LocalTime availableFrom;
        LocalTime availableTo;

        @Size(max = 100, message = "Seasonal availability must not exceed 100 characters")
        String seasonalAvailability;

        @Size(max = 1000, message = "Notes must not exceed 1000 characters")
        String notes;
    }
}
