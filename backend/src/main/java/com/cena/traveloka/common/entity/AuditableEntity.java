package com.cena.traveloka.common.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Auditable entity extending BaseEntity with audit trail functionality.
 * Features:
 * - Automatic creation and modification timestamps
 * - User tracking for who created/modified the entity
 * - Soft delete functionality with deletion tracking
 * - Built-in support for Spring Data JPA auditing
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditableEntity extends BaseEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "deleted_at")
    private ZonedDateTime deletedAt;

    @Column(name = "deleted_by")
    private String deletedBy;

    @Column(name = "is_deleted", nullable = false)
    private Boolean deleted = false;

    /**
     * Default constructor required by JPA
     */
    protected AuditableEntity() {
        super();
    }

    /**
     * Constructor for creating new auditable entities
     */
    protected AuditableEntity(UUID id) {
        super(id);
    }

    /**
     * Get the creation timestamp
     * @return creation timestamp in UTC
     */
    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Set the creation timestamp
     * @param createdAt creation timestamp
     */
    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Get the last modification timestamp
     * @return last modification timestamp in UTC
     */
    public ZonedDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Set the last modification timestamp
     * @param updatedAt last modification timestamp
     */
    public void setUpdatedAt(ZonedDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Get the user who created this entity
     * @return creator user identifier
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * Set the user who created this entity
     * @param createdBy creator user identifier
     */
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * Get the user who last modified this entity
     * @return last modifier user identifier
     */
    public String getUpdatedBy() {
        return updatedBy;
    }

    /**
     * Set the user who last modified this entity
     * @param updatedBy last modifier user identifier
     */
    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    /**
     * Get the soft deletion timestamp
     * @return deletion timestamp, null if not deleted
     */
    public ZonedDateTime getDeletedAt() {
        return deletedAt;
    }

    /**
     * Set the soft deletion timestamp
     * @param deletedAt deletion timestamp
     */
    public void setDeletedAt(ZonedDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    /**
     * Get the user who soft deleted this entity
     * @return deleter user identifier
     */
    public String getDeletedBy() {
        return deletedBy;
    }

    /**
     * Set the user who soft deleted this entity
     * @param deletedBy deleter user identifier
     */
    public void setDeletedBy(String deletedBy) {
        this.deletedBy = deletedBy;
    }

    /**
     * Check if this entity is soft deleted
     * @return true if entity is deleted, false otherwise
     */
    public Boolean isDeleted() {
        return deleted != null && deleted;
    }

    /**
     * Set the soft deletion flag
     * @param deleted deletion flag
     */
    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    /**
     * Perform soft delete on this entity
     * Sets deletion flag, timestamp, and deleter information
     * @param deletedBy user performing the deletion
     */
    public void softDelete(String deletedBy) {
        this.deleted = true;
        this.deletedAt = ZonedDateTime.now();
        this.deletedBy = deletedBy;
    }

    /**
     * Restore a soft deleted entity
     * Clears deletion flag, timestamp, and deleter information
     */
    public void restore() {
        this.deleted = false;
        this.deletedAt = null;
        this.deletedBy = null;
    }

    /**
     * Check if this entity was created within the specified number of days
     * @param days number of days to check
     * @return true if entity was created within the specified days
     */
    public boolean isCreatedWithinDays(int days) {
        if (createdAt == null) {
            return false;
        }
        ZonedDateTime cutoff = ZonedDateTime.now().minusDays(days);
        return createdAt.isAfter(cutoff);
    }

    /**
     * Check if this entity was modified within the specified number of days
     * @param days number of days to check
     * @return true if entity was modified within the specified days
     */
    public boolean isModifiedWithinDays(int days) {
        if (updatedAt == null) {
            return false;
        }
        ZonedDateTime cutoff = ZonedDateTime.now().minusDays(days);
        return updatedAt.isAfter(cutoff);
    }

    /**
     * Get the age of this entity in days since creation
     * @return age in days, or -1 if creation date is not set
     */
    public long getAgeInDays() {
        if (createdAt == null) {
            return -1;
        }
        return java.time.Duration.between(createdAt, ZonedDateTime.now()).toDays();
    }

    /**
     * Check if this entity has been modified since creation
     * @return true if entity has been modified
     */
    public boolean isModified() {
        return updatedAt != null && createdAt != null && updatedAt.isAfter(createdAt);
    }

    /**
     * Pre-persist callback to set creation timestamp if not already set
     */
    @PrePersist
    protected void onCreate() {
        super.generateId();
        if (createdAt == null) {
            createdAt = ZonedDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = createdAt;
        }
        if (deleted == null) {
            deleted = false;
        }
    }

    /**
     * Pre-update callback to set modification timestamp
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = ZonedDateTime.now();
    }

    /**
     * String representation including audit information
     * @return string containing class name, UUID, and audit timestamps
     */
    @Override
    public String toString() {
        return String.format("%s{id=%s, version=%d, createdAt=%s, updatedAt=%s, deleted=%b}",
            getClass().getSimpleName(),
            getId() != null ? getId().toString() : "null",
            getVersion() != null ? getVersion() : 0L,
            createdAt != null ? createdAt.toString() : "null",
            updatedAt != null ? updatedAt.toString() : "null",
            deleted != null ? deleted : false
        );
    }
}