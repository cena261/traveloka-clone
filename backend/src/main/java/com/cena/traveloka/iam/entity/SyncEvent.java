package com.cena.traveloka.iam.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;
import com.cena.traveloka.iam.enums.SyncDirection;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * SyncEvent entity mapped to iam.sync_events table
 * Matches PostgreSQL schema exactly from V9 migration
 */
@Entity
@Table(schema = "iam", name = "sync_events")
public class SyncEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "bigserial")
    private Long id;

    @Column(name = "event_type", nullable = false, columnDefinition = "text")
    @NotNull
    private String eventType;

    @Column(name = "keycloak_user_id", columnDefinition = "text")
    private String keycloakUserId;

    @Column(name = "local_user_id", columnDefinition = "uuid")
    private UUID localUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "sync_direction", nullable = false, columnDefinition = "text")
    @NotNull
    private SyncDirection syncDirection;

    @Column(name = "status", nullable = false, columnDefinition = "text")
    @NotNull
    private String status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload = Map.of();

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "processed_at", nullable = false, columnDefinition = "timestamptz")
    @Generated(GenerationTime.INSERT)
    private OffsetDateTime processedAt;

    @Column(name = "retry_count", nullable = false, columnDefinition = "integer")
    private Integer retryCount = 0;

    // JPA relationship (optional)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "local_user_id", insertable = false, updatable = false)
    private AppUser user;

    // Constructors
    public SyncEvent() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getKeycloakUserId() { return keycloakUserId; }
    public void setKeycloakUserId(String keycloakUserId) { this.keycloakUserId = keycloakUserId; }

    public UUID getLocalUserId() { return localUserId; }
    public void setLocalUserId(UUID localUserId) { this.localUserId = localUserId; }

    public SyncDirection getSyncDirection() { return syncDirection; }
    public void setSyncDirection(SyncDirection syncDirection) { this.syncDirection = syncDirection; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Map<String, Object> getPayload() { return payload; }
    public void setPayload(Map<String, Object> payload) { this.payload = payload; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public OffsetDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(OffsetDateTime processedAt) { this.processedAt = processedAt; }

    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }

    public AppUser getUser() { return user; }
    public void setUser(AppUser user) { this.user = user; }

    // Alias methods for backward compatibility with service code
    public String getEntityId() {
        return localUserId != null ? localUserId.toString() : null;
    }
    public void setEntityId(String entityId) {
        if (entityId != null) {
            this.localUserId = UUID.fromString(entityId);
        }
    }

    public Map<String, Object> getEventData() { return payload; }
    public void setEventData(Map<String, Object> eventData) { this.payload = eventData; }

    public String getIdempotencyKey() {
        return payload != null ? (String) payload.get("idempotencyKey") : null;
    }
    public void setIdempotencyKey(String idempotencyKey) {
        if (payload == null) payload = new java.util.HashMap<>();
        ((java.util.HashMap<String, Object>) payload).put("idempotencyKey", idempotencyKey);
    }

    public String getCreatedBy() {
        return payload != null ? (String) payload.get("createdBy") : null;
    }
    public void setCreatedBy(String createdBy) {
        if (payload == null) payload = new java.util.HashMap<>();
        ((java.util.HashMap<String, Object>) payload).put("createdBy", createdBy);
    }

    // Alias for syncDirection to match service expectations
    public SyncDirection getDirection() { return syncDirection; }
}