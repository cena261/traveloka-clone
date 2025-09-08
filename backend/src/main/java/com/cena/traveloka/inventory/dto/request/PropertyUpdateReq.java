package com.cena.traveloka.inventory.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PropertyUpdateReq {
    String kind;
    String name;
    String description;

    String countryCode;
    String city;
    String addressLine;
    String postalCode;

    Double lat;
    Double lng;

    String status;     // draft|active|inactive
    String timezone;
}
