package com.cena.traveloka.catalog.inventory.repository;

import com.cena.traveloka.catalog.inventory.entity.Partner;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PartnerRepository extends JpaRepository<Partner, UUID> {

    @Cacheable(value = "partners", key = "#email")
    Optional<Partner> findByEmail(String email);

    @Cacheable(value = "partners", key = "#businessRegistrationNumber")
    Optional<Partner> findByBusinessRegistrationNumber(String businessRegistrationNumber);

    @Query("SELECT p FROM Partner p WHERE p.status = :status")
    Page<Partner> findByStatus(@Param("status") Partner.PartnerStatus status, Pageable pageable);

    @Query("SELECT p FROM Partner p WHERE " +
           "(COALESCE(:status) IS NULL OR p.status = :status) AND " +
           "(COALESCE(:search) IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Partner> findByStatusAndSearch(@Param("status") Partner.PartnerStatus status,
                                       @Param("search") String search,
                                       Pageable pageable);

    @Query("SELECT p FROM Partner p WHERE p.performanceRating >= :minRating ORDER BY p.performanceRating DESC")
    List<Partner> findByPerformanceRatingGreaterThanEqual(@Param("minRating") Double minRating);

    @Query("SELECT p FROM Partner p WHERE " +
           "p.contractEndDate <= CURRENT_DATE + INTERVAL '30 days' AND " +
           "p.status = 'ACTIVE'")
    List<Partner> findPartnersWithExpiringContracts();

    @Query("SELECT COUNT(p) FROM Partner p WHERE p.status = :status")
    long countByStatus(@Param("status") Partner.PartnerStatus status);

    @Query("SELECT p FROM Partner p WHERE " +
           "p.totalBookings >= :minBookings AND " +
           "p.performanceRating >= :minRating AND " +
           "p.status = 'ACTIVE'")
    Page<Partner> findTopPerformingPartners(@Param("minBookings") Integer minBookings,
                                           @Param("minRating") Double minRating,
                                           Pageable pageable);

    boolean existsByEmail(String email);
    boolean existsByBusinessRegistrationNumber(String businessRegistrationNumber);
}
