package com.cena.traveloka.iam.repository;

import com.cena.traveloka.iam.entity.TwoFactorAuth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TwoFactorAuthRepository extends JpaRepository<TwoFactorAuth, UUID> {

    List<TwoFactorAuth> findByUserId(UUID userId);

    Optional<TwoFactorAuth> findByUserIdAndMethod(UUID userId, String method);

    List<TwoFactorAuth> findByUserIdAndIsActiveTrue(UUID userId);

    List<TwoFactorAuth> findByUserIdAndVerifiedTrue(UUID userId);

    Optional<TwoFactorAuth> findByUserIdAndIsPrimaryTrue(UUID userId);

    boolean existsByUserIdAndIsActiveTrueAndVerifiedTrue(UUID userId);

    void deleteByUserId(UUID userId);

    List<TwoFactorAuth> findByMethod(String method);
}
