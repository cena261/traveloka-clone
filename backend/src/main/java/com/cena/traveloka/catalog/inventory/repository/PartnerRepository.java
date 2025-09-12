package com.cena.traveloka.catalog.inventory.repository;

import com.cena.traveloka.catalog.inventory.entity.Partner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PartnerRepository extends JpaRepository<Partner, UUID> {}
