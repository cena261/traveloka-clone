package com.cena.traveloka.catalog.inventory.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
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
@Table(name = "room_type", schema = "inventory", indexes = {
    @Index(name = "room_type_prop_idx", columnList = "property_id")
})
public class RoomType {

    @Id
    @Builder.Default
    UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    @NotNull(message = "Property is required")
    Property property;

    @Column(nullable = false, length = 100)
    @NotBlank(message = "Room type name is required")
    @Size(min = 2, max = 100, message = "Room type name must be between 2 and 100 characters")
    String name;

    @Column(columnDefinition = "text")
    String description;

    @Column(name = "capacity_adult", nullable = false)
    @NotNull(message = "Adult capacity is required")
    @Min(value = 1, message = "Adult capacity must be at least 1")
    @Max(value = 20, message = "Adult capacity cannot exceed 20")
    Integer capacityAdult;

    @Column(name = "capacity_child", nullable = false)
    @Min(value = 0, message = "Child capacity must be non-negative")
    @Max(value = 10, message = "Child capacity cannot exceed 10")
    @Builder.Default
    Integer capacityChild = 0;

    @Column(name = "max_occupancy")
    @Min(value = 1, message = "Max occupancy must be at least 1")
    @Max(value = 20, message = "Max occupancy cannot exceed 20")
    Integer maxOccupancy;

    @Enumerated(EnumType.STRING)
    @Column(name = "bed_type", length = 20)
    BedType bedType;

    @Column(name = "bed_count")
    @Min(value = 1, message = "Bed count must be at least 1")
    @Max(value = 10, message = "Bed count cannot exceed 10")
    Integer bedCount;

    @Column(name = "room_size", precision = 8, scale = 2)
    @DecimalMin(value = "5.0", message = "Room size must be at least 5 square meters")
    @DecimalMax(value = "1000.0", message = "Room size cannot exceed 1000 square meters")
    BigDecimal roomSize;

    @Column(name = "has_balcony")
    @Builder.Default
    Boolean hasBalcony = false;

    @Column(name = "has_sea_view")
    @Builder.Default
    Boolean hasSeaView = false;

    @Column(name = "has_city_view")
    @Builder.Default
    Boolean hasCityView = false;

    @Column(name = "smoking_allowed")
    @Builder.Default
    Boolean smokingAllowed = false;

    @Column(name = "base_price_cents", nullable = false)
    @NotNull(message = "Base price is required")
    @Min(value = 0, message = "Base price must be non-negative")
    Long basePriceCents;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    String currency = "VND";

    @Column(nullable = false)
    @Builder.Default
    Boolean refundable = true;

    @Column(name = "is_active")
    @Builder.Default
    Boolean isActive = true;

    @Column(name = "total_units", nullable = false)
    @Min(value = 0, message = "Total units must be non-negative")
    @Builder.Default
    Integer totalUnits = 0;

    @OneToMany(mappedBy = "roomType", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    List<RoomUnit> units = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (capacityChild == null) {
            capacityChild = 0;
        }
        if (hasBalcony == null) {
            hasBalcony = false;
        }
        if (hasSeaView == null) {
            hasSeaView = false;
        }
        if (hasCityView == null) {
            hasCityView = false;
        }
        if (smokingAllowed == null) {
            smokingAllowed = false;
        }
        if (refundable == null) {
            refundable = true;
        }
        if (isActive == null) {
            isActive = true;
        }
        if (totalUnits == null) {
            totalUnits = 0;
        }
        if (currency == null) {
            currency = "VND";
        }
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public enum BedType {
        SINGLE, DOUBLE, QUEEN, KING, TWIN, SOFA_BED
    }
}

