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
@Table(name="amenity", schema="inventory")
public class Amenity {
    @Id UUID id;
    @Column(nullable=false, unique=true) String code;
    @Column(nullable=false) String name;

    @PrePersist void pre(){ if(id==null) id=UUID.randomUUID(); }
}

