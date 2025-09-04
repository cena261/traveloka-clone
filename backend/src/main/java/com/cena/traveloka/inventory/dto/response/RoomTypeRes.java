package com.cena.traveloka.inventory.dto.response;

import lombok.*;

import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RoomTypeRes {
    private UUID id;
    private UUID propertyId;

    private String name;
    private String description;

    private Integer capacityAdult;
    private Integer capacityChild;

    private Long basePriceCents;
    private String currency;
    private Boolean refundable;

    private Integer totalUnits;
}
