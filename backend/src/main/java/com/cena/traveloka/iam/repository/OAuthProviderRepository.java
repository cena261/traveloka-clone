package com.cena.traveloka.iam.repository;

import com.cena.traveloka.iam.entity.OAuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OAuthProviderRepository extends JpaRepository<OAuthProvider, UUID> {

    List<OAuthProvider> findByUserId(UUID userId);

    Optional<OAuthProvider> findByUserIdAndProvider(UUID userId, String provider);

    Optional<OAuthProvider> findByProviderAndProviderUserId(String provider, String providerUserId);

    boolean existsByUserIdAndProvider(UUID userId, String provider);

    List<OAuthProvider> findByProvider(String provider);

    void deleteByUserIdAndProvider(UUID userId, String provider);

    void deleteByUserId(UUID userId);

    List<OAuthProvider> findByEmail(String email);
}
