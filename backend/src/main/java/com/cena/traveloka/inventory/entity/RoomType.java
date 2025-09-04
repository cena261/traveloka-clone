package com.cena.traveloka.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name="room_type", schema="inventory")
public class RoomType {
    @Id private UUID id;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="property_id", nullable=false)
    private Property property;

    @Column(nullable=false) private String name;
    @Column(columnDefinition="text") private String description;

    @Column(name="capacity_adult", nullable=false) private Integer capacityAdult;
    @Column(name="capacity_child", nullable=false) private Integer capacityChild;

    @Column(name="base_price_cents", nullable=false) private Long basePriceCents;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;
    @Column(nullable=false) private Boolean refundable = true;

    @Column(name="total_units", nullable=false) private Integer totalUnits = 0;

    @PrePersist void pre(){ if(id==null) id=UUID.randomUUID(); }
}

