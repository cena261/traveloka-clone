package com.cena.traveloka.inventory.dto.response;

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
    String url;
    Integer sortOrder;
    OffsetDateTime createdAt;
}
