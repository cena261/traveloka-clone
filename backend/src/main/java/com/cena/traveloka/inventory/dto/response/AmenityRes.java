package com.cena.traveloka.inventory.dto.response;

import lombok.*;

import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AmenityRes {
    private UUID id;
    private String code;
    private String name;
}
