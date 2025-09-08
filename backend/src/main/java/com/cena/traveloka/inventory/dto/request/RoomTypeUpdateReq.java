package com.cena.traveloka.inventory.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomTypeUpdateReq {
    String name;
    String description;

    Integer capacityAdult;
    Integer capacityChild;

    Long basePriceCents;
    String currency;
    Boolean refundable;

    Integer totalUnits;
}
