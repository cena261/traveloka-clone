package com.cena.traveloka.inventory.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class PartnerCreateReq {
    @NotBlank private String name;
    @NotNull  private UUID ownerUserId;
    private String legalName;
    private String taxNumber;
}
