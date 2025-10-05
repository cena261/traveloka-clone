package com.cena.traveloka.common.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.ZonedDateTime;
import java.util.UUID;

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

    protected AuditableEntity() {
        super();
    }

    protected AuditableEntity(UUID id) {
        super(id);
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public ZonedDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(ZonedDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public ZonedDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(ZonedDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public String getDeletedBy() {
        return deletedBy;
    }

    public void setDeletedBy(String deletedBy) {
        this.deletedBy = deletedBy;
    }

    public Boolean isDeleted() {
        return deleted != null && deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    public void softDelete(String deletedBy) {
        this.deleted = true;
        this.deletedAt = ZonedDateTime.now();
        this.deletedBy = deletedBy;
    }

    public void restore() {
        this.deleted = false;
        this.deletedAt = null;
        this.deletedBy = null;
    }

    public boolean isCreatedWithinDays(int days) {
        if (createdAt == null) {
            return false;
        }
        ZonedDateTime cutoff = ZonedDateTime.now().minusDays(days);
        return createdAt.isAfter(cutoff);
    }

    public boolean isModifiedWithinDays(int days) {
        if (updatedAt == null) {
            return false;
        }
        ZonedDateTime cutoff = ZonedDateTime.now().minusDays(days);
        return updatedAt.isAfter(cutoff);
    }

    public long getAgeInDays() {
        if (createdAt == null) {
            return -1;
        }
        return java.time.Duration.between(createdAt, ZonedDateTime.now()).toDays();
    }

    public boolean isModified() {
        return updatedAt != null && createdAt != null && updatedAt.isAfter(createdAt);
    }

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

    @PreUpdate
    protected void onUpdate() {
        updatedAt = ZonedDateTime.now();
    }

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