package com.cena.traveloka.iam.entity;

import com.cena.traveloka.common.enums.DocumentType;
import com.cena.traveloka.common.enums.Status;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name="user_documents", schema="iam",
        uniqueConstraints = @UniqueConstraint(name="uk_user_doc", columnNames={"user_id","document_type","document_number"}),
        indexes = {
                @Index(name="idx_user_documents_user_id", columnList="user_id"),
                @Index(name="idx_user_documents_type", columnList="document_type"),
                @Index(name="idx_user_documents_expiry", columnList="expiry_date")
        })
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserDocument {
    @Id @GeneratedValue @JdbcTypeCode(SqlTypes.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="user_id", nullable=false)
    User user;

    @Enumerated(EnumType.STRING)
    @Column(name="document_type", nullable=false)
    DocumentType documentType;

    @Column(name="document_number", nullable=false, length=100)
    String documentNumber;

    @Column(name="issuing_country", length=2, nullable=false)
    String issuingCountry;

    LocalDate issueDate;
    LocalDate expiryDate;

    String firstName;
    String lastName;
    String middleName;
    LocalDate dateOfBirth;
    String placeOfBirth;
    @Column(length=2) String nationality;

    String frontImageUrl;
    String backImageUrl;

    Boolean isVerified = false;
    OffsetDateTime verifiedAt;
    String verifiedBy;
    String verificationMethod;

    @Enumerated(EnumType.STRING)
    Status status = Status.pending;

    Boolean isPrimary = false;

    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;
}
