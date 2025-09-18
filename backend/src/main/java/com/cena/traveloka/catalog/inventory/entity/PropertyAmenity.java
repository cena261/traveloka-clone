package com.cena.traveloka.catalog.inventory.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "property_amenity", schema = "inventory",
       uniqueConstraints = {@UniqueConstraint(columnNames = {"property_id", "amenity_id"})})
public class PropertyAmenity {

    @Id
    @Builder.Default
    UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    @NotNull(message = "Property is required")
    Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "amenity_id", nullable = false)
    @NotNull(message = "Amenity is required")
    Amenity amenity;

    @Column(name = "is_free")
    @Builder.Default
    Boolean isFree = true;

    @Column(name = "additional_cost", precision = 10, scale = 2)
    @DecimalMin(value = "0.00", message = "Additional cost must be non-negative")
    BigDecimal additionalCost;

    @Column(name = "available_from")
    LocalTime availableFrom;

    @Column(name = "available_to")
    LocalTime availableTo;

    @Column(name = "seasonal_availability", length = 100)
    String seasonalAvailability;

    @Column(columnDefinition = "text")
    String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (isFree == null) {
            isFree = true;
        }
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}