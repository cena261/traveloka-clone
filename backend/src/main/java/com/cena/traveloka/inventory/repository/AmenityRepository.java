package com.cena.traveloka.inventory.repository;

import com.cena.traveloka.inventory.entity.Amenity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface AmenityRepository extends JpaRepository<Amenity, UUID> {
    Optional<Amenity> findByCode(String code);
}
