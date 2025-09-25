package com.cena.traveloka.iam.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;
import com.cena.traveloka.iam.enums.DeviceType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.net.Inet4Address;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * UserSession entity mapped to iam.user_sessions table
 * Matches PostgreSQL schema exactly from V9 migration
 */
@Entity
@Table(schema = "iam", name = "user_sessions")
public class UserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "bigserial")
    private Long id;

    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    @NotNull
    private UUID userId;

    @Column(name = "session_id", nullable = false, unique = true, columnDefinition = "text")
    @NotNull
    private String sessionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", columnDefinition = "text")
    private DeviceType deviceType;

    @Column(name = "device_name", columnDefinition = "text")
    private String deviceName;

    @Column(name = "ip_address", columnDefinition = "inet")
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "text")
    private String userAgent;

    @Column(name = "location", columnDefinition = "text")
    private String location;

    @JdbcTypeCode(SqlTypes.JSON)
    @ElementCollection
    @CollectionTable(name = "user_session_metadata",
                     joinColumns = @JoinColumn(name = "user_session_id"),
                     schema = "iam")
    @MapKeyColumn(name = "metadata_key")
    @Column(name = "metadata_value", columnDefinition = "jsonb")
    private Map<String, Object> metadata = Map.of();

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
    @Generated(GenerationTime.INSERT)
    private OffsetDateTime createdAt;

    @Column(name = "last_activity_at", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime lastActivityAt;

    @Column(name = "expires_at", nullable = false, columnDefinition = "timestamptz")
    @NotNull
    private OffsetDateTime expiresAt;

    @Column(name = "status", nullable = false, columnDefinition = "text")
    private String status = "ACTIVE";

    // JPA relationship (optional)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private AppUser user;

    // Constructors
    public UserSession() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public DeviceType getDeviceType() { return deviceType; }
    public void setDeviceType(DeviceType deviceType) { this.deviceType = deviceType; }

    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getLastActivityAt() { return lastActivityAt; }
    public void setLastActivityAt(OffsetDateTime lastActivityAt) { this.lastActivityAt = lastActivityAt; }

    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(OffsetDateTime expiresAt) { this.expiresAt = expiresAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public AppUser getUser() { return user; }
    public void setUser(AppUser user) { this.user = user; }

    @PrePersist
    void prePersist() {
        if (lastActivityAt == null) {
            lastActivityAt = OffsetDateTime.now();
        }
    }
}