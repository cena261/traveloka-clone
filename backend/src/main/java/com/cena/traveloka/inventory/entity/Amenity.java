package com.cena.traveloka.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name="amenity", schema="inventory")
public class Amenity {
    @Id private UUID id;
    @Column(nullable=false, unique=true) private String code;
    @Column(nullable=false) private String name;

    @PrePersist void pre(){ if(id==null) id=UUID.randomUUID(); }
}

