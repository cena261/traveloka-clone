package com.cena.traveloka.catalog.inventory.dto.response;

import com.cena.traveloka.catalog.inventory.entity.Partner;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
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
    String businessRegistrationNumber;
    String email;
    String phoneNumber;
    String address;
    String city;
    String country;
    String postalCode;
    String website;
    BigDecimal commissionRate;
    Partner.PartnerStatus status;
    LocalDate contractStartDate;
    LocalDate contractEndDate;
    Double performanceRating;
    Integer totalBookings;
    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;
}
