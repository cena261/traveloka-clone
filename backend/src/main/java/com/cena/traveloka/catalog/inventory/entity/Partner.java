package com.cena.traveloka.catalog.inventory.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "partner", schema = "inventory", indexes = {
    @Index(name = "partner_owner_idx", columnList = "owner_user_id"),
    @Index(name = "partner_email_idx", columnList = "email"),
    @Index(name = "partner_status_idx", columnList = "status")
})
public class Partner {

    @Id
    @Column(nullable = false)
    @Builder.Default
    UUID id = UUID.randomUUID();

    @Column(name = "owner_user_id", nullable = false)
    UUID ownerUserId;

    @Column(nullable = false, length = 100)
    @NotBlank(message = "Partner name is required")
    @Size(min = 2, max = 100, message = "Partner name must be between 2 and 100 characters")
    String name;

    @Column(name = "legal_name", length = 200)
    String legalName;

    @Column(name = "tax_number", length = 50)
    String taxNumber;

    @Column(nullable = false, unique = true, length = 255)
    @Email(message = "Valid email address is required")
    @NotBlank(message = "Email is required")
    String email;

    @Column(length = 20)
    @Pattern(regexp = "^[+]?[0-9\\s\\-\\(\\)]+$", message = "Invalid phone number format")
    String phone;

    @Column(name = "business_registration_number", length = 50, unique = true)
    @NotBlank(message = "Business registration number is required")
    String businessRegistrationNumber;

    @Column(name = "contract_start_date")
    @NotNull(message = "Contract start date is required")
    LocalDate contractStartDate;

    @Column(name = "contract_end_date")
    @NotNull(message = "Contract end date is required")
    LocalDate contractEndDate;

    @Column(name = "commission_rate", precision = 5, scale = 2)
    @NotNull(message = "Commission rate is required")
    @DecimalMin(value = "0.00", message = "Commission rate must be non-negative")
    @DecimalMax(value = "100.00", message = "Commission rate cannot exceed 100%")
    BigDecimal commissionRate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    PartnerStatus status = PartnerStatus.PENDING;

    @Column(name = "performance_rating", precision = 3, scale = 2)
    @DecimalMin(value = "1.00", message = "Performance rating must be at least 1.00")
    @DecimalMax(value = "5.00", message = "Performance rating cannot exceed 5.00")
    @Builder.Default
    BigDecimal performanceRating = BigDecimal.valueOf(0.00);

    @Column(name = "total_bookings")
    @Min(value = 0, message = "Total bookings must be non-negative")
    @Builder.Default
    Integer totalBookings = 0;

    @Column(name = "average_response_time")
    @Min(value = 0, message = "Average response time must be non-negative")
    @Builder.Default
    Integer averageResponseTime = 0;

    @OneToMany(mappedBy = "partner", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    List<Property> properties = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    OffsetDateTime updatedAt;

    @Column(name = "created_by")
    UUID createdBy;

    @Column(name = "updated_by")
    UUID updatedBy;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (status == null) {
            status = PartnerStatus.PENDING;
        }
        if (performanceRating == null) {
            performanceRating = BigDecimal.valueOf(0.00);
        }
        if (totalBookings == null) {
            totalBookings = 0;
        }
        if (averageResponseTime == null) {
            averageResponseTime = 0;
        }
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public enum PartnerStatus {
        PENDING, ACTIVE, SUSPENDED, TERMINATED
    }
}
