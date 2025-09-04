package com.cena.traveloka.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name="property_image", schema="inventory")
public class PropertyImage {
    @Id private UUID id;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="property_id", nullable=false)
    private Property property;

    @Column(nullable=false) private String url;
    @Column(name="sort_order", nullable=false) private Integer sortOrder;

    @Column(name="created_at", insertable=false, updatable=false) private OffsetDateTime createdAt;

    @PrePersist void pre(){ if(id==null) id=UUID.randomUUID(); if(sortOrder==null) sortOrder=0; }
}


