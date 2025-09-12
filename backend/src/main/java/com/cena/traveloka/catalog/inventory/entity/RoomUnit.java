package com.cena.traveloka.catalog.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name="room_unit", schema="inventory")
public class RoomUnit {
    @Id UUID id;
    @ManyToOne(fetch= FetchType.LAZY) @JoinColumn(name="room_type_id", nullable=false)
    RoomType roomType;
    @Column(nullable=false) String code;
}

