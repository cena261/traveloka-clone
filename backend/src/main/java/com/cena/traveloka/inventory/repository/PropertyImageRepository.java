package com.cena.traveloka.inventory.repository;

import com.cena.traveloka.inventory.entity.Property;
import com.cena.traveloka.inventory.entity.PropertyImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PropertyImageRepository extends JpaRepository<PropertyImage, UUID> {
    List<PropertyImage> findByPropertyOrderBySortOrderAsc(Property property);

    Optional<PropertyImage> findTopByPropertyOrderBySortOrderDesc(Property property);
}

