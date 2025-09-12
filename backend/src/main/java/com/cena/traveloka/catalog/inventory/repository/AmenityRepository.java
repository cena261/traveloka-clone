package com.cena.traveloka.catalog.inventory.repository;

import com.cena.traveloka.catalog.inventory.entity.Amenity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface AmenityRepository extends JpaRepository<Amenity, UUID> {
    Optional<Amenity> findByCode(String code);
}
