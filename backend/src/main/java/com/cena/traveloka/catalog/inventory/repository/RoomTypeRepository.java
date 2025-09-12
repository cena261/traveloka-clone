package com.cena.traveloka.catalog.inventory.repository;

import com.cena.traveloka.catalog.inventory.entity.RoomType;
import com.cena.traveloka.catalog.inventory.entity.Property;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface RoomTypeRepository extends JpaRepository<RoomType, UUID> {
    List<RoomType> findByProperty(Property property);
}
