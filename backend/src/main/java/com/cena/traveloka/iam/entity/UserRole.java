package com.cena.traveloka.iam.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * UserRole entity mapped to iam.user_roles table
 * Matches PostgreSQL schema exactly from V2 + V9 migrations
 */
@Entity
@Table(schema = "iam", name = "user_roles")
public class UserRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "bigserial")
    private Long id;

    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    @NotNull
    private UUID userId;

    @Column(name = "role_id", nullable = false, columnDefinition = "bigint")
    @NotNull
    private Long roleId;

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
    @Generated(GenerationTime.INSERT)
    private OffsetDateTime createdAt;

    // V9 additions
    @Column(name = "assigned_at", nullable = false, columnDefinition = "timestamptz")
    @Generated(GenerationTime.INSERT)
    private OffsetDateTime assignedAt;

    @Column(name = "assigned_by", columnDefinition = "text")
    private String assignedBy;

    @Column(name = "expires_at", columnDefinition = "timestamptz")
    private OffsetDateTime expiresAt;

    @Column(name = "status", nullable = false, columnDefinition = "text")
    private String status = "ACTIVE";

    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamptz", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "assignment_reason", columnDefinition = "text")
    private String assignmentReason;

    // JPA relationships (optional - use scalar references if preferred for performance)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", insertable = false, updatable = false)
    private Role role;

    // Constructors
    public UserRole() {}

    public UserRole(UUID userId, Long roleId) {
        this.userId = userId;
        this.roleId = roleId;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public Long getRoleId() { return roleId; }
    public void setRoleId(Long roleId) { this.roleId = roleId; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getAssignedAt() { return assignedAt; }
    public void setAssignedAt(OffsetDateTime assignedAt) { this.assignedAt = assignedAt; }

    public String getAssignedBy() { return assignedBy; }
    public void setAssignedBy(String assignedBy) { this.assignedBy = assignedBy; }

    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(OffsetDateTime expiresAt) { this.expiresAt = expiresAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    public AppUser getUser() { return user; }
    public void setUser(AppUser user) { this.user = user; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getAssignmentReason() { return assignmentReason; }
    public void setAssignmentReason(String assignmentReason) { this.assignmentReason = assignmentReason; }
}