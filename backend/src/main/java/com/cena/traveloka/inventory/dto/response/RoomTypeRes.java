package com.cena.traveloka.inventory.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomTypeRes {
    UUID id;
    UUID propertyId;

    String name;
    String description;

    Integer capacityAdult;
    Integer capacityChild;

    Long basePriceCents;
    String currency;
    Boolean refundable;

    Integer totalUnits;
}
