package com.cena.traveloka.inventory.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Getter @Setter @NoArgsConstructor
@AllArgsConstructor @Builder
@Entity
@Table(name="room_unit", schema="inventory")
public class RoomUnit {
    @Id private UUID id;
    @ManyToOne(fetch= FetchType.LAZY) @JoinColumn(name="room_type_id", nullable=false)
    private RoomType roomType;
    @Column(nullable=false) private String code;
}

