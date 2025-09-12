package com.cena.traveloka.catalog.inventory.repository;

import com.cena.traveloka.catalog.inventory.entity.Property;
import com.cena.traveloka.catalog.inventory.entity.PropertyImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PropertyImageRepository extends JpaRepository<PropertyImage, UUID> {
    List<PropertyImage> findByPropertyOrderBySortOrderAsc(Property property);

    Optional<PropertyImage> findTopByPropertyOrderBySortOrderDesc(Property property);
}

