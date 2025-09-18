package com.cena.traveloka.catalog.inventory.dto.response;

import com.cena.traveloka.catalog.inventory.entity.RoomUnit;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomUnitRes {
    UUID id;
    UUID roomTypeId;
    String code;
    String roomNumber;
    Integer floorNumber;
    RoomUnit.RoomStatus status;
    LocalDate lastMaintenanceDate;
    String notes;
    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;
}