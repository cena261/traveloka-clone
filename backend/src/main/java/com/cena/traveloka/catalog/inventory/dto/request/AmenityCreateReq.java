package com.cena.traveloka.catalog.inventory.dto.request;

import com.cena.traveloka.catalog.inventory.entity.Amenity;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AmenityCreateReq {
    @NotBlank(message = "Amenity code is required")
    @Size(max = 50, message = "Amenity code must not exceed 50 characters")
    String code;

    @NotBlank(message = "Amenity name is required")
    @Size(max = 100, message = "Amenity name must not exceed 100 characters")
    String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    String description;

    Amenity.AmenityCategory category;

    @Size(max = 50, message = "Icon must not exceed 50 characters")
    String icon;

    Boolean isPopular;

    Integer sortOrder;
}
