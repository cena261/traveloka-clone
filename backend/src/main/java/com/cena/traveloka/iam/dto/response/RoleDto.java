package com.cena.traveloka.iam.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleDto {

    private UUID id;

    private String name;

    private String displayName;

    private String description;

    private Boolean isSystem;

    private Integer priority;
}
