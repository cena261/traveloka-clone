package com.cena.traveloka.inventory.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class RoomTypeCreateReq {
    @NotNull private UUID propertyId;

    @NotBlank private String name;
    private String description;

    @NotNull private Integer capacityAdult;
    private Integer capacityChild = 0;

    @NotNull private Long basePriceCents;     // BIGINT
    private String currency = "VND";          // 3 ky tu
    private Boolean refundable = true;

    private Integer totalUnits = 0;
}
