package com.cena.traveloka.catalog.inventory.dto.response;

import com.cena.traveloka.catalog.inventory.entity.RoomType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomTypeRes {
    UUID id;
    UUID propertyId;
    String typeCode;
    String typeName;
    String description;
    Integer maxOccupancy;
    Integer adultCapacity;
    Integer childCapacity;
    Integer infantCapacity;
    BigDecimal basePrice;
    String currency;
    Boolean isRefundable;
    BigDecimal roomSize;
    String bedType;
    RoomType.SmokingPolicy smokingPolicy;
    RoomType.RoomTypeStatus status;
    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;
}
