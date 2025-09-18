package com.cena.traveloka.catalog.inventory.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "property", schema = "inventory", indexes = {
    @Index(name = "property_partner_idx", columnList = "partner_id"),
    @Index(name = "property_city_idx", columnList = "country_code, city"),
    @Index(name = "property_geo_idx", columnList = "geog")
})
public class Property {

    @Id
    @Column(nullable = false)
    @Builder.Default
    UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id", nullable = false)
    @NotNull(message = "Partner is required")
    Partner partner;

    @Column(nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    @NotNull(message = "Property kind is required")
    PropertyKind kind;

    @Column(nullable = false, length = 200)
    @NotBlank(message = "Property name is required")
    @Size(min = 2, max = 200, message = "Property name must be between 2 and 200 characters")
    String name;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    Map<String, String> description;

    @Column(name = "star_rating")
    @Min(value = 1, message = "Star rating must be at least 1")
    @Max(value = 5, message = "Star rating cannot exceed 5")
    Integer starRating;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "country_code", nullable = false, length = 2)
    @NotBlank(message = "Country code is required")
    @Pattern(regexp = "^[A-Z]{2}$", message = "Country code must be 2 uppercase letters")
    String countryCode;

    @Column(nullable = false, length = 100)
    @NotBlank(message = "City is required")
    String city;

    @Column(name = "address_line", nullable = false, length = 500)
    @NotBlank(message = "Address is required")
    String addressLine;

    @Column(name = "postal_code", length = 20)
    String postalCode;

    @Column(name = "lat")
    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    Double latitude;

    @Column(name = "lng")
    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    Double longitude;

    @Column(name = "geog", columnDefinition = "geography(Point,4326)")
    Point geography;

    @Column(name = "phone_number", length = 20)
    @Pattern(regexp = "^[+]?[0-9\\s\\-\\(\\)]+$", message = "Invalid phone number format")
    String phoneNumber;

    @Column(length = 255)
    @Email(message = "Valid email address is required")
    String email;

    @Column(length = 500)
    String website;

    @Column(name = "check_in_time")
    LocalTime checkInTime;

    @Column(name = "check_out_time")
    LocalTime checkOutTime;

    @Column(name = "total_rooms")
    @Min(value = 1, message = "Total rooms must be at least 1")
    @Max(value = 10000, message = "Total rooms cannot exceed 10,000")
    Integer totalRooms;

    @Column(name = "rating_avg", precision = 3, scale = 2)
    @DecimalMin(value = "0.00", message = "Rating average must be non-negative")
    @DecimalMax(value = "5.00", message = "Rating average cannot exceed 5.00")
    @Builder.Default
    BigDecimal ratingAvg = BigDecimal.valueOf(0.00);

    @Column(name = "rating_count")
    @Min(value = 0, message = "Rating count must be non-negative")
    @Builder.Default
    Integer ratingCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    PropertyStatus status = PropertyStatus.DRAFT;

    @Column(length = 50)
    @Builder.Default
    String timezone = "Asia/Ho_Chi_Minh";

    @Column(name = "verification_notes", columnDefinition = "text")
    String verificationNotes;

    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    List<PropertyImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    List<RoomType> roomTypes = new ArrayList<>();

    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    List<PropertyAmenity> propertyAmenities = new ArrayList<>();

    @ManyToMany
    @JoinTable(
            schema = "inventory",
            name = "amenity_map",
            joinColumns = @JoinColumn(name = "property_id"),
            inverseJoinColumns = @JoinColumn(name = "amenity_id")
    )
    @Builder.Default
    Set<Amenity> amenities = new HashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    OffsetDateTime updatedAt;

    @Column(name = "created_by")
    UUID createdBy;

    @Column(name = "updated_by")
    UUID updatedBy;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (status == null) {
            status = PropertyStatus.DRAFT;
        }
        if (timezone == null) {
            timezone = "Asia/Ho_Chi_Minh";
        }
        if (ratingAvg == null) {
            ratingAvg = BigDecimal.valueOf(0.00);
        }
        if (ratingCount == null) {
            ratingCount = 0;
        }
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public enum PropertyStatus {
        DRAFT, PENDING_VERIFICATION, ACTIVE, INACTIVE, SUSPENDED
    }

    public enum PropertyKind {
        hotel, homestay, villa, restaurant, meeting_room
    }
}
