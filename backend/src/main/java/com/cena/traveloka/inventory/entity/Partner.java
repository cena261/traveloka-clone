package com.cena.traveloka.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "partner", schema = "inventory")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Partner {
    @Id @Column(nullable=false) private UUID id;

    @Column(name="owner_user_id", nullable=false) private UUID ownerUserId;

    @Column(nullable=false) private String name;
    @Column(name="legal_name") private String legalName;
    @Column(name="tax_number") private String taxNumber;

    @Column(nullable=false, length=20) private String status; // 'active' | 'suspended' | 'pending'

    @Column(name="created_at", updatable=false, insertable=false) private OffsetDateTime createdAt;
    @Column(name="updated_at", insertable=false) private OffsetDateTime updatedAt;

    @PrePersist void pre(){ if(id==null) id=UUID.randomUUID(); if(status==null) status="active"; }
}
