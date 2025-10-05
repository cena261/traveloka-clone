package com.cena.traveloka.iam.repository;

import com.cena.traveloka.iam.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {

    Optional<EmailVerificationToken> findByToken(String token);

    Optional<EmailVerificationToken> findByTokenAndVerifiedFalseAndExpiresAtAfter(String token, OffsetDateTime now);

    List<EmailVerificationToken> findByUserId(UUID userId);

    List<EmailVerificationToken> findByUserIdAndVerifiedFalse(UUID userId);

    List<EmailVerificationToken> findByEmail(String email);

    Optional<EmailVerificationToken> findFirstByUserIdAndVerifiedFalseOrderByCreatedAtDesc(UUID userId);

    void deleteByExpiresAtBefore(OffsetDateTime now);

    List<EmailVerificationToken> findByExpiresAtBefore(OffsetDateTime now);

    long countByUserIdAndVerifiedFalse(UUID userId);

    void deleteByUserId(UUID userId);

    boolean existsByUserIdAndVerifiedFalse(UUID userId);

    void deleteByUserIdAndVerifiedFalse(UUID userId);
}
