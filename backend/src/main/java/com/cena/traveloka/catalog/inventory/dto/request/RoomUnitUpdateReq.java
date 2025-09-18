package com.cena.traveloka.catalog.inventory.dto.request;

import com.cena.traveloka.catalog.inventory.entity.RoomUnit;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomUnitUpdateReq {
    @Size(max = 20, message = "Room number must not exceed 20 characters")
    String roomNumber;

    @Min(value = -5, message = "Floor number must be at least -5")
    @Max(value = 200, message = "Floor number cannot exceed 200")
    Integer floorNumber;

    RoomUnit.RoomStatus status;

    LocalDate lastMaintenanceDate;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    String notes;
}