package com.cena.traveloka.catalog.inventory.dto.request;

import com.cena.traveloka.catalog.inventory.entity.Property;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PropertyUpdateReq {
    Property.PropertyKind kind;

    @Size(max = 100, message = "Property name must not exceed 100 characters")
    String name;

    Map<String, String> description;

    @Size(min = 2, max = 2, message = "Country code must be 2 characters")
    String countryCode;

    @Size(max = 50, message = "City must not exceed 50 characters")
    String city;

    @Size(max = 200, message = "Address line must not exceed 200 characters")
    String addressLine;

    @Size(max = 10, message = "Postal code must not exceed 10 characters")
    String postalCode;

    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    Double lat;

    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    Double lng;

    @Min(value = 1, message = "Star rating must be at least 1")
    @Max(value = 5, message = "Star rating must be at most 5")
    Integer starRating;

    Property.PropertyStatus status;

    @Size(max = 50, message = "Timezone must not exceed 50 characters")
    String timezone;
}
