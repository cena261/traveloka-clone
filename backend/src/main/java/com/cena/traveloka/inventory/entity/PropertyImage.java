package com.cena.traveloka.inventory.entity;

import jakarta.persistence.*;
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
@Table(name="property_image", schema="inventory")
public class PropertyImage {
    @Id UUID id;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="property_id", nullable=false)
    Property property;

    @Column(nullable=false) String url;
    @Column(name="sort_order", nullable=false) Integer sortOrder;

    @Column(name="created_at", insertable=false, updatable=false) OffsetDateTime createdAt;

    @PrePersist void pre(){ if(id==null) id=UUID.randomUUID(); if(sortOrder==null) sortOrder=0; }
}


