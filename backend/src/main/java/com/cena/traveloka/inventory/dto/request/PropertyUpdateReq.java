package com.cena.traveloka.inventory.dto.request;

import lombok.Data;

@Data
public class PropertyUpdateReq {
    private String kind;
    private String name;
    private String description;

    private String countryCode;
    private String city;
    private String addressLine;
    private String postalCode;

    private Double lat;
    private Double lng;

    private String status;     // draft|active|inactive
    private String timezone;
}
