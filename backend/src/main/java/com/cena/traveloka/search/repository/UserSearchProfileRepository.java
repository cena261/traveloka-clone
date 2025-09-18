package com.cena.traveloka.search.repository;

import com.cena.traveloka.search.entity.UserSearchProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserSearchProfileRepository extends JpaRepository<UserSearchProfile, UUID> {
    Optional<UserSearchProfile> findByUserId(String userId);
}