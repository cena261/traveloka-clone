package com.cena.traveloka.catalog.inventory.dto.response;

import com.cena.traveloka.catalog.inventory.entity.PropertyImage;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PropertyImageRes {
    UUID id;
    UUID propertyId;
    String imageUrl;
    PropertyImage.ImageType imageType;
    Integer sortOrder;
    String caption;
    String altText;
    Boolean isPrimary;
    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;
}
