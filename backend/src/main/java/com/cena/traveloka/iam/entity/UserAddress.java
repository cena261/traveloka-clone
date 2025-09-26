package com.cena.traveloka.iam.entity;

import com.cena.traveloka.common.enums.AddressType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;

import java.util.UUID;

@Entity
@Table(name = "user_addresses", schema = "iam",
        indexes = {
                @Index(name="idx_user_addresses_user_id", columnList = "user_id"),
                @Index(name="idx_user_addresses_type", columnList = "type")
        })
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserAddress {
    @Id @GeneratedValue @JdbcTypeCode(SqlTypes.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="user_id", nullable=false)
    User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    AddressType type;

    Boolean isPrimary = false;
    String label;

    @Column(nullable=false) String streetLine1;
    String streetLine2;
    @Column(nullable=false) String city;
    String stateProvince;
    String postalCode;
    @Column(name="country_code", length = 2, nullable=false)
    String countryCode;

    @Column(precision = 10, scale = 8) java.math.BigDecimal latitude;
    @Column(precision = 11, scale = 8) java.math.BigDecimal longitude;

    @Column(columnDefinition = "geometry(Point,4326)")
    Point location;

    Boolean verified = false;
    java.time.OffsetDateTime verifiedAt;
    java.time.OffsetDateTime createdAt;
    java.time.OffsetDateTime updatedAt;
}
