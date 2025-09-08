package com.cena.traveloka.inventory.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomTypeCreateReq {
    @NotNull
    UUID propertyId;

    @NotBlank String name;
    String description;

    @NotNull Integer capacityAdult;
    Integer capacityChild = 0;

    @NotNull Long basePriceCents;     // BIGINT
    String currency = "VND";          // 3 ky tu
    Boolean refundable = true;

    Integer totalUnits = 0;
}
