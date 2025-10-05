package com.cena.traveloka.iam.repository;

import com.cena.traveloka.iam.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByToken(String token);

    Optional<PasswordResetToken> findByTokenAndUsedFalseAndExpiresAtAfter(String token, OffsetDateTime now);

    List<PasswordResetToken> findByUserId(UUID userId);

    List<PasswordResetToken> findByUserIdAndUsedFalse(UUID userId);

    void deleteByExpiresAtBefore(OffsetDateTime now);

    List<PasswordResetToken> findByExpiresAtBefore(OffsetDateTime now);

    long countByUserIdAndUsedFalse(UUID userId);

    void deleteByUserId(UUID userId);
}
