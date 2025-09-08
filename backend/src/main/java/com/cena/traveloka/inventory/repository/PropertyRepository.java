package com.cena.traveloka.inventory.repository;

import com.cena.traveloka.inventory.entity.Property;
import com.cena.traveloka.inventory.entity.Partner;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface PropertyRepository extends JpaRepository<Property, UUID> {
    Page<Property> findByPartner(Partner partner, Pageable pageable);
}
