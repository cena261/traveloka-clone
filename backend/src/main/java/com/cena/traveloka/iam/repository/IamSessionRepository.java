package com.cena.traveloka.iam.repository;

import com.cena.traveloka.iam.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IamSessionRepository extends JpaRepository<Session, UUID> {

    Optional<Session> findBySessionToken(String sessionToken);

    List<Session> findByUserIdAndIsActiveTrue(UUID userId);

    long countByUserIdAndIsActiveTrue(UUID userId);

    Optional<Session> findFirstByUserIdAndIsActiveTrueOrderByCreatedAtAsc(UUID userId);

    List<Session> findByIsActiveTrueAndExpiresAtBefore(OffsetDateTime now);

    List<Session> findByIsSuspiciousTrue();

    List<Session> findByRequires2faTrueAndTwoFaCompletedFalse();

    List<Session> findByIpAddress(String ipAddress);

    void deleteByUserId(UUID userId);

    List<Session> findByDeviceId(String deviceId);
}
