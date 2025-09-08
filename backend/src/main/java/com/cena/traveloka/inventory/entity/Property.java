package com.cena.traveloka.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "property", schema = "inventory")
public class Property {

    @Id
    @Column(nullable = false)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id", nullable = false)
    Partner partner;

    @Column(nullable = false, length = 30)
    String kind;

    @Column(nullable = false)
    String name;

    @Column(columnDefinition = "text")
    String description;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "country_code", nullable = false, length = 2)
    String countryCode;

    @Column(nullable = false)
    String city;

    @Column(name = "address_line", nullable = false)
    String addressLine;

    @Column(name = "postal_code")
    String postalCode;

    @Column(name = "lat")
    Double latitude;

    @Column(name = "lng")
    Double longitude;

    @Column(name = "rating_avg", precision = 3, scale = 2)
    BigDecimal ratingAvg;

    @Column(name = "rating_count")
    Integer ratingCount;

    @Column(nullable = false, length = 20)
    String status; // draft|active|inactive

    String timezone; // default: Asia/Ho_Chi_Minh

    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, orphanRemoval = true)
    List<PropertyImage> images = new ArrayList<>();

    @ManyToMany
    @JoinTable(
            schema = "inventory",
            name = "amenity_map",
            joinColumns = @JoinColumn(name = "property_id"),
            inverseJoinColumns = @JoinColumn(name = "amenity_id")
    )
    Set<Amenity> amenities = new HashSet<>();

    @Column(name = "created_at", insertable = false, updatable = false)
    OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false)
    OffsetDateTime updatedAt;

    @PrePersist
    void pre() {
        if (id == null) id = UUID.randomUUID();
        if (status == null) status = "draft";
        if (timezone == null) timezone = "Asia/Ho_Chi_Minh";
    }
}
