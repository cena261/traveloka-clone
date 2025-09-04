package com.cena.traveloka.inventory.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class PropertyAmenityBindReq {
    @NotEmpty
    private List<UUID> amenityIds;
}
