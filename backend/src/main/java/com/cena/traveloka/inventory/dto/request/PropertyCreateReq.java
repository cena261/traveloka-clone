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
public class PropertyCreateReq {
    @NotNull
    private UUID partnerId;

    @NotBlank String kind;           // hotel|homestay|villa|restaurant|meeting_room
    @NotBlank String name;
    String description;

    @NotBlank String countryCode;    // 2 ky tu
    @NotBlank String city;
    @NotBlank String addressLine;
    String postalCode;

    Double lat;
    Double lng;

    String timezone;                 // optional, default 'Asia/Ho_Chi_Minh'
}
