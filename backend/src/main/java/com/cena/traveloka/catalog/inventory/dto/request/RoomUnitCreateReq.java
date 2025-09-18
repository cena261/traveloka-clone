package com.cena.traveloka.catalog.inventory.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomUnitCreateReq {
    @NotNull(message = "Room type ID is required")
    UUID roomTypeId;

    @NotBlank(message = "Room code is required")
    @Pattern(regexp = "^[A-Za-z0-9\\-]+$", message = "Room code can only contain letters, numbers, and hyphens")
    @Size(max = 20, message = "Room code must not exceed 20 characters")
    String code;

    @Size(max = 20, message = "Room number must not exceed 20 characters")
    String roomNumber;

    @Min(value = -5, message = "Floor number must be at least -5")
    @Max(value = 200, message = "Floor number cannot exceed 200")
    Integer floorNumber;

    LocalDate lastMaintenanceDate;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    String notes;
}