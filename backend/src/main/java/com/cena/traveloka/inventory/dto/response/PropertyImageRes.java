package com.cena.traveloka.inventory.dto.response;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PropertyImageRes {
    private UUID id;
    private UUID propertyId;
    private String url;
    private Integer sortOrder;
    private OffsetDateTime createdAt;
}
