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
public class PartnerCreateReq {
    @NotBlank String name;
    @NotNull  UUID ownerUserId;
    String legalName;
    String taxNumber;
}
