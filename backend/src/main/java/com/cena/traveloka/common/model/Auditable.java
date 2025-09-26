package com.cena.traveloka.common.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.OffsetDateTime;

@MappedSuperclass
@Getter @Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public abstract class Auditable {
    @Column(name = "created_at")
    OffsetDateTime createdAt;

    @Column(name = "updated_at")
    OffsetDateTime updatedAt;

    @Column(name = "created_by")
    String createdBy;

    @Column(name = "updated_by")
    String updatedBy;

    @PrePersist
    public void prePersist() {
        createdAt = createdAt == null ? OffsetDateTime.now() : createdAt;
        updatedAt = updatedAt == null ? createdAt : updatedAt;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}

