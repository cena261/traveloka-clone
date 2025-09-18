package com.cena.traveloka.catalog.inventory.repository;

import com.cena.traveloka.catalog.inventory.entity.Property;
import com.cena.traveloka.catalog.inventory.entity.Partner;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface PropertyRepository extends JpaRepository<Property, UUID> {

    Page<Property> findByPartner(Partner partner, Pageable pageable);

    @Cacheable(value = "properties", key = "#propertyCode")
    Optional<Property> findByPropertyCode(String propertyCode);

    @Query("SELECT p FROM Property p WHERE p.status = :status")
    Page<Property> findByStatus(@Param("status") Property.PropertyStatus status, Pageable pageable);

    @Query("SELECT p FROM Property p WHERE " +
           "(COALESCE(:status) IS NULL OR p.status = :status) AND " +
           "(COALESCE(:search) IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Property> findByStatusAndSearch(@Param("status") Property.PropertyStatus status,
                                        @Param("search") String search,
                                        Pageable pageable);

    @Query(value = "SELECT p.* FROM inventory.property p WHERE " +
                   "ST_DWithin(p.geog, ST_SetSRID(ST_Point(:lng, :lat), 4326), :radiusMeters) AND " +
                   "p.status = 'ACTIVE'",
           nativeQuery = true)
    List<Property> findPropertiesWithinRadius(@Param("lat") double latitude,
                                             @Param("lng") double longitude,
                                             @Param("radiusMeters") double radiusMeters);

    @Query(value = "SELECT p.*, " +
                   "ST_Distance(p.geog, ST_SetSRID(ST_Point(:lng, :lat), 4326)) as distance " +
                   "FROM inventory.property p WHERE " +
                   "ST_DWithin(p.geog, ST_SetSRID(ST_Point(:lng, :lat), 4326), :radiusMeters) AND " +
                   "p.status = 'ACTIVE' " +
                   "ORDER BY distance",
           nativeQuery = true)
    List<Property> findNearestProperties(@Param("lat") double latitude,
                                        @Param("lng") double longitude,
                                        @Param("radiusMeters") double radiusMeters);

    @Query("SELECT p FROM Property p WHERE " +
           "p.partner = :partner AND " +
           "p.averageRating >= :minRating AND " +
           "p.status = 'ACTIVE'")
    Page<Property> findPartnerPropertiesByRating(@Param("partner") Partner partner,
                                                @Param("minRating") Double minRating,
                                                Pageable pageable);

    @Query("SELECT p FROM Property p WHERE " +
           "p.totalBookings >= :minBookings AND " +
           "p.averageRating >= :minRating AND " +
           "p.status = 'ACTIVE'")
    Page<Property> findTopPerformingProperties(@Param("minBookings") Integer minBookings,
                                              @Param("minRating") Double minRating,
                                              Pageable pageable);

    @Query("SELECT COUNT(p) FROM Property p WHERE p.partner = :partner AND p.status = :status")
    long countByPartnerAndStatus(@Param("partner") Partner partner,
                                 @Param("status") Property.PropertyStatus status);

    @Query("SELECT AVG(p.averageRating) FROM Property p WHERE p.partner = :partner AND p.status = 'ACTIVE'")
    Double getAverageRatingByPartner(@Param("partner") Partner partner);

    boolean existsByPropertyCode(String propertyCode);
    boolean existsByPartnerAndPropertyCode(Partner partner, String propertyCode);
}
