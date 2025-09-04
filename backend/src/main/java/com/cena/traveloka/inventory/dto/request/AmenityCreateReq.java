package com.cena.traveloka.inventory.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AmenityCreateReq {
    @NotBlank private String code;   // unique
    @NotBlank private String name;
}
