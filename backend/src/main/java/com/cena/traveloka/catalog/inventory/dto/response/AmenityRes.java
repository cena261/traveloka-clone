package com.cena.traveloka.catalog.inventory.dto.response;

import com.cena.traveloka.catalog.inventory.entity.Amenity;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AmenityRes {
    UUID id;
    String code;
    String name;
    String description;
    Amenity.AmenityCategory category;
    String icon;
    Boolean isPopular;
    Integer sortOrder;
    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;
}
