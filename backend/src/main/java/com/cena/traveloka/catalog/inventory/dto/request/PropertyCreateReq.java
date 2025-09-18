package com.cena.traveloka.catalog.inventory.dto.request;

import com.cena.traveloka.catalog.inventory.entity.Property;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PropertyCreateReq {
    @NotNull(message = "Partner ID is required")
    UUID partnerId;

    @NotBlank(message = "Property code is required")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "Property code can only contain letters, numbers, underscores, and hyphens")
    @Size(min = 3, max = 20, message = "Property code must be between 3 and 20 characters")
    String propertyCode;

    @NotNull(message = "Property kind is required")
    Property.PropertyKind kind;

    @NotBlank(message = "Property name is required")
    @Size(max = 100, message = "Property name must not exceed 100 characters")
    String name;

    Map<String, String> description;

    @NotBlank(message = "Country code is required")
    @Size(min = 2, max = 2, message = "Country code must be 2 characters")
    String countryCode;

    @NotBlank(message = "City is required")
    @Size(max = 50, message = "City must not exceed 50 characters")
    String city;

    @NotBlank(message = "Address line is required")
    @Size(max = 200, message = "Address line must not exceed 200 characters")
    String addressLine;

    @Size(max = 10, message = "Postal code must not exceed 10 characters")
    String postalCode;

    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    Double lat;

    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    Double lng;

    @Min(value = 1, message = "Star rating must be at least 1")
    @Max(value = 5, message = "Star rating must be at most 5")
    Integer starRating;

    @Size(max = 50, message = "Timezone must not exceed 50 characters")
    String timezone;
}
