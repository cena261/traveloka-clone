package com.cena.traveloka.catalog.inventory.repository;

import com.cena.traveloka.catalog.inventory.entity.PropertyAmenity;
import com.cena.traveloka.catalog.inventory.entity.Property;
import com.cena.traveloka.catalog.inventory.entity.Amenity;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.*;

@Repository
public interface PropertyAmenityRepository extends JpaRepository<PropertyAmenity, UUID> {

    @Cacheable(value = "propertyAmenities", key = "#property.id")
    List<PropertyAmenity> findByProperty(Property property);

    List<PropertyAmenity> findByAmenity(Amenity amenity);

    Optional<PropertyAmenity> findByPropertyAndAmenity(Property property, Amenity amenity);

    @Query("SELECT pa FROM PropertyAmenity pa WHERE " +
           "pa.property = :property AND " +
           "pa.isFree = :isFree")
    List<PropertyAmenity> findByPropertyAndIsFree(@Param("property") Property property,
                                                 @Param("isFree") Boolean isFree);

    @Query("SELECT pa FROM PropertyAmenity pa WHERE " +
           "pa.property = :property AND " +
           "pa.amenity.category = :category")
    List<PropertyAmenity> findByPropertyAndAmenityCategory(@Param("property") Property property,
                                                          @Param("category") Amenity.AmenityCategory category);

    @Query("SELECT pa FROM PropertyAmenity pa WHERE " +
           "pa.property = :property AND " +
           "pa.amenity.isPopular = true")
    List<PropertyAmenity> findPopularAmenitiesByProperty(@Param("property") Property property);

    @Query("SELECT pa FROM PropertyAmenity pa WHERE " +
           "pa.property = :property AND " +
           "pa.additionalCost IS NOT NULL AND " +
           "pa.additionalCost > 0")
    List<PropertyAmenity> findPaidAmenitiesByProperty(@Param("property") Property property);

    @Query("SELECT COUNT(pa) FROM PropertyAmenity pa WHERE " +
           "pa.property = :property AND " +
           "pa.amenity.category = :category")
    long countByPropertyAndAmenityCategory(@Param("property") Property property,
                                          @Param("category") Amenity.AmenityCategory category);

    @Query("SELECT SUM(pa.additionalCost) FROM PropertyAmenity pa WHERE " +
           "pa.property = :property AND " +
           "pa.additionalCost IS NOT NULL")
    BigDecimal getTotalAdditionalCostByProperty(@Param("property") Property property);

    @Query("SELECT pa.amenity FROM PropertyAmenity pa WHERE " +
           "pa.property = :property " +
           "ORDER BY pa.amenity.category, pa.amenity.sortOrder, pa.amenity.name")
    List<Amenity> findAmenitiesByPropertyOrdered(@Param("property") Property property);

    boolean existsByPropertyAndAmenity(Property property, Amenity amenity);
}