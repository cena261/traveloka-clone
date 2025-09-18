package com.cena.traveloka.catalog.inventory.repository;

import com.cena.traveloka.catalog.inventory.entity.RoomType;
import com.cena.traveloka.catalog.inventory.entity.Property;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.*;

@Repository
public interface RoomTypeRepository extends JpaRepository<RoomType, UUID> {

    List<RoomType> findByProperty(Property property);

    @Cacheable(value = "roomTypes", key = "#property.id + '-' + #typeCode")
    Optional<RoomType> findByPropertyAndTypeCode(Property property, String typeCode);

    @Query("SELECT rt FROM RoomType rt WHERE " +
           "rt.property = :property AND " +
           "rt.status = :status")
    List<RoomType> findByPropertyAndStatus(@Param("property") Property property,
                                          @Param("status") RoomType.RoomTypeStatus status);

    @Query("SELECT rt FROM RoomType rt WHERE " +
           "rt.property = :property AND " +
           "rt.basePrice BETWEEN :minPrice AND :maxPrice AND " +
           "rt.status = 'ACTIVE'")
    List<RoomType> findByPropertyAndPriceRange(@Param("property") Property property,
                                              @Param("minPrice") BigDecimal minPrice,
                                              @Param("maxPrice") BigDecimal maxPrice);

    @Query("SELECT rt FROM RoomType rt WHERE " +
           "rt.property = :property AND " +
           "rt.maxOccupancy >= :minOccupancy AND " +
           "rt.status = 'ACTIVE'")
    List<RoomType> findByPropertyAndMinOccupancy(@Param("property") Property property,
                                                @Param("minOccupancy") Integer minOccupancy);

    @Query("SELECT rt FROM RoomType rt JOIN rt.roomUnits ru WHERE " +
           "rt.property = :property AND " +
           "ru.status = 'AVAILABLE' AND " +
           "rt.status = 'ACTIVE' " +
           "GROUP BY rt HAVING COUNT(ru) > 0")
    List<RoomType> findAvailableRoomTypesByProperty(@Param("property") Property property);

    @Query("SELECT rt FROM RoomType rt WHERE " +
           "rt.property.partner.id = :partnerId AND " +
           "rt.status = 'ACTIVE'")
    List<RoomType> findByPartnerId(@Param("partnerId") UUID partnerId);

    @Query("SELECT COUNT(ru) FROM RoomUnit ru WHERE " +
           "ru.roomType = :roomType AND " +
           "ru.status = 'AVAILABLE'")
    long countAvailableUnits(@Param("roomType") RoomType roomType);

    @Query("SELECT AVG(rt.basePrice) FROM RoomType rt WHERE " +
           "rt.property = :property AND " +
           "rt.status = 'ACTIVE'")
    BigDecimal getAveragePriceByProperty(@Param("property") Property property);

    boolean existsByPropertyAndTypeCode(Property property, String typeCode);
}
