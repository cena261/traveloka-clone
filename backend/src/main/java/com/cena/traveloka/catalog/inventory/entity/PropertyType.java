package com.cena.traveloka.catalog.inventory.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

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
@Table(name = "property_type", schema = "inventory")
public class PropertyType {

    @Id
    @Builder.Default
    UUID id = UUID.randomUUID();

    @Column(nullable = false, unique = true, length = 100)
    @NotBlank(message = "Property type name is required")
    @Size(min = 2, max = 100, message = "Property type name must be between 2 and 100 characters")
    String name;

    @Column(length = 500)
    String description;

    @Column(name = "sort_order")
    @Min(value = 0, message = "Sort order must be non-negative")
    @Builder.Default
    Integer sortOrder = 0;

    @Column(name = "is_active")
    @Builder.Default
    Boolean isActive = true;

    @OneToMany(mappedBy = "propertyType", fetch = FetchType.LAZY)
    @Builder.Default
    List<Property> properties = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (sortOrder == null) {
            sortOrder = 0;
        }
        if (isActive == null) {
            isActive = true;
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