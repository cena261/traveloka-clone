package com.cena.traveloka.common.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@MappedSuperclass
public abstract class BaseEntity implements Serializable {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "id", columnDefinition = "BINARY(16)")
    private UUID id;

    @Version
    @Column(name = "version")
    private Long version;

    protected BaseEntity() {
    }

    protected BaseEntity(UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public boolean isNew() {
        return id == null;
    }

    @PrePersist
    protected void generateId() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        BaseEntity that = (BaseEntity) obj;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? Objects.hash(id) : super.hashCode();
    }

    @Override
    public String toString() {
        return String.format("%s{id=%s, version=%d}",
            getClass().getSimpleName(),
            id != null ? id.toString() : "null",
            version != null ? version : 0L
        );
    }
}