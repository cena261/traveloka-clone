package com.cena.traveloka.inventory.repository;

import com.cena.traveloka.inventory.entity.Partner;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface PartnerRepository extends JpaRepository<Partner, UUID> {}
