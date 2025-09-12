package com.cena.traveloka.catalog.inventory.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PartnerRes {
    UUID id;
    UUID ownerUserId;
    String name;
    String legalName;
    String taxNumber;
    String status;
}
