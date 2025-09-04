package com.cena.traveloka.inventory.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class PropertyCreateReq {
    @NotNull private UUID partnerId;

    @NotBlank private String kind;           // hotel|homestay|villa|restaurant|meeting_room
    @NotBlank private String name;
    private String description;

    @NotBlank private String countryCode;    // 2 ky tu
    @NotBlank private String city;
    @NotBlank private String addressLine;
    private String postalCode;

    private Double lat;
    private Double lng;

    private String timezone;                 // optional, default 'Asia/Ho_Chi_Minh'
}
