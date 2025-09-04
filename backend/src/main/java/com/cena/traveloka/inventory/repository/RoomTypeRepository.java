package com.cena.traveloka.inventory.repository;

import com.cena.traveloka.inventory.entity.RoomType;
import com.cena.traveloka.inventory.entity.Property;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface RoomTypeRepository extends JpaRepository<RoomType, UUID> {
    List<RoomType> findByProperty(Property property);
}
