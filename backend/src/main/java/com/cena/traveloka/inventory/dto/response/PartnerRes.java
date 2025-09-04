package com.cena.traveloka.inventory.dto.response;

import lombok.*;

import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PartnerRes {
    private UUID id;
    private UUID ownerUserId;
    private String name;
    private String legalName;
    private String taxNumber;
    private String status;
}
