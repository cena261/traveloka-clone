package com.cena.traveloka.inventory.repository;

import com.cena.traveloka.inventory.entity.Property;
import com.cena.traveloka.inventory.entity.Partner;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface PropertyRepository extends JpaRepository<Property, UUID> {
    Page<Property> findByPartner(Partner partner, Pageable pageable);
}
