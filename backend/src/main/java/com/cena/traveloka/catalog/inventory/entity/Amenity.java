package com.cena.traveloka.catalog.inventory.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "amenity", schema = "inventory")
public class Amenity {

    @Id
    @Builder.Default
    UUID id = UUID.randomUUID();

    @Column(nullable = false, unique = true, length = 50)
    @NotBlank(message = "Amenity code is required")
    String code;

    @Column(nullable = false, length = 100)
    @NotBlank(message = "Amenity name is required")
    String name;

    @Column(length = 500)
    String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    @Builder.Default
    AmenityCategory category = AmenityCategory.GENERAL;

    @Column(length = 50)
    String icon;

    @Column(name = "is_popular")
    @Builder.Default
    Boolean isPopular = false;

    @Column(name = "sort_order")
    @Builder.Default
    Integer sortOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (category == null) {
            category = AmenityCategory.GENERAL;
        }
        if (isPopular == null) {
            isPopular = false;
        }
        if (sortOrder == null) {
            sortOrder = 0;
        }
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public enum AmenityCategory {
        ACCOMMODATION, BUSINESS, CONNECTIVITY, FOOD_DRINK, GENERAL, RECREATION, TRANSPORT
    }
}

