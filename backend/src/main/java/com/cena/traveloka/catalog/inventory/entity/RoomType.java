package com.cena.traveloka.catalog.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name="room_type", schema="inventory")
public class RoomType {
    @Id UUID id;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="property_id", nullable=false)
    Property property;

    @Column(nullable=false) String name;
    @Column(columnDefinition="text") String description;

    @Column(name="capacity_adult", nullable=false) Integer capacityAdult;
    @Column(name="capacity_child", nullable=false) Integer capacityChild;

    @Column(name="base_price_cents", nullable=false) Long basePriceCents;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency", nullable = false, length = 3)
    String currency;
    @Column(nullable=false) Boolean refundable = true;

    @Column(name="total_units", nullable=false) Integer totalUnits = 0;

    @PrePersist void pre(){ if(id==null) id=UUID.randomUUID(); }
}

