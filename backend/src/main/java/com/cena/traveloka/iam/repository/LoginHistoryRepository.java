package com.cena.traveloka.iam.repository;

import com.cena.traveloka.iam.entity.LoginHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {

    List<LoginHistory> findByUserId(UUID userId);

    List<LoginHistory> findBySuccess(boolean success);

    List<LoginHistory> findByUserIdAndSuccessFalse(UUID userId);

    long countByUserIdAndSuccessFalseAndAttemptedAtAfter(UUID userId, OffsetDateTime after);

    List<LoginHistory> findByEmail(String email);

    List<LoginHistory> findByUsername(String username);

    List<LoginHistory> findByIpAddress(String ipAddress);

    List<LoginHistory> findByIsSuspiciousTrue();

    List<LoginHistory> findByProvider(String provider);

    List<LoginHistory> findByAttemptedAtBetween(OffsetDateTime start, OffsetDateTime end);

    List<LoginHistory> findByUserIdOrderByAttemptedAtDesc(UUID userId, Pageable pageable);

    long countByUserId(UUID userId);

    List<LoginHistory> findByRequired2faTrue();
}
