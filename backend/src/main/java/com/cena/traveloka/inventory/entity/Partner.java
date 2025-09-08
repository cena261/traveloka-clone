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
@Table(name = "partner", schema = "inventory")
public class Partner {
    @Id @Column(nullable=false) UUID id;

    @Column(name="owner_user_id", nullable=false) UUID ownerUserId;

    @Column(nullable=false) String name;
    @Column(name="legal_name") String legalName;
    @Column(name="tax_number") String taxNumber;

    @Column(nullable=false, length=20) String status; // 'active' | 'suspended' | 'pending'

    @Column(name="created_at", updatable=false, insertable=false) OffsetDateTime createdAt;
    @Column(name="updated_at", insertable=false) OffsetDateTime updatedAt;

    @PrePersist void pre(){ if(id==null) id=UUID.randomUUID(); if(status==null) status="active"; }
}
