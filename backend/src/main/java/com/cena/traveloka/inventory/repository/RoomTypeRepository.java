package com.cena.traveloka.inventory.repository;

import com.cena.traveloka.inventory.entity.RoomType;
import com.cena.traveloka.inventory.entity.Property;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface RoomTypeRepository extends JpaRepository<RoomType, UUID> {
    List<RoomType> findByProperty(Property property);
}
