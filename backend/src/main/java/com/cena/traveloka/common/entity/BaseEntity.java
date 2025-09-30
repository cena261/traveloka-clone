package com.cena.traveloka.common.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Base entity class providing common fields and behavior for all entities.
 * Features:
 * - UUID-based primary key for better security and distribution
 * - Optimistic locking with @Version annotation
 * - Consistent equals/hashCode implementation
 * - Serializable for caching support
 */
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

    /**
     * Default constructor required by JPA
     */
    protected BaseEntity() {
    }

    /**
     * Constructor for creating new entities with generated UUID
     */
    protected BaseEntity(UUID id) {
        this.id = id;
    }

    /**
     * Get the unique identifier of this entity
     * @return UUID identifier
     */
    public UUID getId() {
        return id;
    }

    /**
     * Set the unique identifier of this entity
     * @param id UUID identifier
     */
    public void setId(UUID id) {
        this.id = id;
    }

    /**
     * Get the version number for optimistic locking
     * @return version number
     */
    public Long getVersion() {
        return version;
    }

    /**
     * Set the version number for optimistic locking
     * @param version version number
     */
    public void setVersion(Long version) {
        this.version = version;
    }

    /**
     * Indicates whether this entity is new (not yet persisted)
     * @return true if entity is new, false if persisted
     */
    public boolean isNew() {
        return id == null;
    }

    /**
     * Generate a new UUID for this entity if it doesn't have one
     */
    @PrePersist
    protected void generateId() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }

    /**
     * Equals implementation based on UUID
     * Two entities are equal if they have the same UUID
     */
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

    /**
     * HashCode implementation based on UUID
     * Uses UUID hashCode if available, otherwise uses class hashCode
     */
    @Override
    public int hashCode() {
        return id != null ? Objects.hash(id) : super.hashCode();
    }

    /**
     * String representation of the entity
     * @return string containing class name and UUID
     */
    @Override
    public String toString() {
        return String.format("%s{id=%s, version=%d}",
            getClass().getSimpleName(),
            id != null ? id.toString() : "null",
            version != null ? version : 0L
        );
    }
}