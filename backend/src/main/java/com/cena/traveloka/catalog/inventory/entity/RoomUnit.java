package com.cena.traveloka.catalog.inventory.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "room_unit", schema = "inventory",
       indexes = {@Index(name = "room_unit_rt_idx", columnList = "room_type_id")},
       uniqueConstraints = {@UniqueConstraint(name = "room_unit_type_code_uk", columnNames = {"room_type_id", "code"})})
public class RoomUnit {

    @Id
    @Builder.Default
    UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_type_id", nullable = false)
    @NotNull(message = "Room type is required")
    RoomType roomType;

    @Column(nullable = false, length = 20)
    @NotBlank(message = "Room code is required")
    @Pattern(regexp = "^[A-Za-z0-9\\-]+$", message = "Room code can only contain letters, numbers, and hyphens")
    String code;

    @Column(name = "room_number", length = 20)
    String roomNumber;

    @Column(name = "floor_number")
    @Min(value = -5, message = "Floor number must be at least -5")
    @Max(value = 200, message = "Floor number cannot exceed 200")
    Integer floorNumber;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    RoomStatus status = RoomStatus.AVAILABLE;

    @Column(name = "last_maintenance_date")
    LocalDate lastMaintenanceDate;

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
        if (status == null) {
            status = RoomStatus.AVAILABLE;
        }
        if (roomNumber == null && code != null) {
            roomNumber = code;
        }
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public enum RoomStatus {
        AVAILABLE, OCCUPIED, MAINTENANCE, OUT_OF_ORDER
    }
}

