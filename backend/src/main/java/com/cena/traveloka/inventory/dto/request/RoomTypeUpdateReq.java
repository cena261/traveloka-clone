package com.cena.traveloka.inventory.dto.request;

import lombok.Data;

@Data
public class RoomTypeUpdateReq {
    private String name;
    private String description;

    private Integer capacityAdult;
    private Integer capacityChild;

    private Long basePriceCents;
    private String currency;
    private Boolean refundable;

    private Integer totalUnits;
}
