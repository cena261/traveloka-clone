package com.cena.traveloka.catalog.inventory.dto.request;

import com.cena.traveloka.catalog.inventory.entity.Partner;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PartnerUpdateReq {
    @Size(max = 100, message = "Partner name must not exceed 100 characters")
    String name;

    @Size(max = 150, message = "Legal name must not exceed 150 characters")
    String legalName;

    @Size(max = 50, message = "Tax number must not exceed 50 characters")
    String taxNumber;

    @Email(message = "Email must be valid")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    String email;

    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    String phoneNumber;

    @Size(max = 200, message = "Address must not exceed 200 characters")
    String address;

    @Size(max = 50, message = "City must not exceed 50 characters")
    String city;

    @Size(max = 2, message = "Country code must be 2 characters")
    String country;

    @Size(max = 10, message = "Postal code must not exceed 10 characters")
    String postalCode;

    @Size(max = 100, message = "Website must not exceed 100 characters")
    String website;

    @DecimalMin(value = "0.00", message = "Commission rate must be non-negative")
    @DecimalMax(value = "100.00", message = "Commission rate cannot exceed 100%")
    BigDecimal commissionRate;

    Partner.PartnerStatus status;

    @Future(message = "Contract end date must be in the future")
    LocalDate contractEndDate;
}
