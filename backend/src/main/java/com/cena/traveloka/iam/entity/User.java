package com.cena.traveloka.iam.entity;

import com.cena.traveloka.common.enums.Gender;
import com.cena.traveloka.common.enums.Status;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

@Entity
@Table(name = "users", schema = "iam",
        indexes = {
                @Index(name = "idx_users_email", columnList = "email"),
                @Index(name = "idx_users_phone", columnList = "phone"),
                @Index(name = "idx_users_username", columnList = "username"),
                @Index(name = "idx_users_keycloak_id", columnList = "keycloak_id"),
                @Index(name = "idx_users_status", columnList = "status"),
                @Index(name = "idx_users_created_at", columnList = "created_at")
        })
@Getter
@Setter
@Builder @NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User {
    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.UUID)
    UUID id;

    @Column(name="keycloak_id", unique = true)
    @JdbcTypeCode(SqlTypes.UUID)
    UUID keycloakId;

    @Column(nullable = false, unique = true, length = 100)
    String username;

    @Column(nullable = false, unique = true)
    String email;

    String phone;

    @Column(name="first_name", length = 100) String firstName;
    @Column(name="last_name", length = 100) String lastName;
    @Column(name="display_name", length = 200) String displayName;

    @Column(name="avatar_url") String avatarUrl;

    LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    Gender gender;

    @Column(name="nationality", length = 2) String nationality;
    @Column(name="preferred_language", length = 2) String preferredLanguage = "en";
    @Column(name="preferred_currency", length = 3) String preferredCurrency = "USD";

    @Column(length = 50) String timezone = "UTC";

    @Enumerated(EnumType.STRING)
    Status status = Status.pending;

    @Column(name="email_verified") Boolean emailVerified = false;
    @Column(name="phone_verified") Boolean phoneVerified = false;
    @Column(name="two_factor_enabled") Boolean twoFactorEnabled = false;
    @Column(name="account_locked") Boolean accountLocked = false;

    String lockReason;
    OffsetDateTime lockedUntil;

    OffsetDateTime lastLoginAt;
    @Column(name="last_login_ip") String lastLoginIp;
    Integer loginCount = 0;
    Integer failedLoginAttempts = 0;
    OffsetDateTime passwordChangedAt;
    OffsetDateTime termsAcceptedAt;
    OffsetDateTime privacyAcceptedAt;
    Boolean marketingConsent = false;

    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;
    String createdBy;
    String updatedBy;
    Boolean isDeleted = false;
    OffsetDateTime deletedAt;
    String deletedBy;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    UserProfile profile;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    Set<UserAddress> addresses = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    Set<UserDocument> documents = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    Set<Session> sessions = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "user_roles", schema = "iam",
            joinColumns = @JoinColumn(name="user_id"),
            inverseJoinColumns = @JoinColumn(name="role_id")
    )
    Set<Role> roles = new HashSet<>();
}