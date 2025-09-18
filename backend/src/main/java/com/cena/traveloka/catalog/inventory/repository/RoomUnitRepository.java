package com.cena.traveloka.catalog.inventory.repository;

import com.cena.traveloka.catalog.inventory.entity.RoomUnit;
import com.cena.traveloka.catalog.inventory.entity.RoomType;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.*;

@Repository
public interface RoomUnitRepository extends JpaRepository<RoomUnit, UUID> {

    List<RoomUnit> findByRoomType(RoomType roomType);

    @Cacheable(value = "roomUnits", key = "#roomType.id + '-' + #code")
    Optional<RoomUnit> findByRoomTypeAndCode(RoomType roomType, String code);

    @Query("SELECT ru FROM RoomUnit ru WHERE " +
           "ru.roomType = :roomType AND " +
           "ru.status = :status")
    List<RoomUnit> findByRoomTypeAndStatus(@Param("roomType") RoomType roomType,
                                          @Param("status") RoomUnit.RoomStatus status);

    @Query("SELECT ru FROM RoomUnit ru WHERE " +
           "ru.roomType.property.id = :propertyId AND " +
           "ru.status = 'AVAILABLE'")
    List<RoomUnit> findAvailableUnitsByProperty(@Param("propertyId") UUID propertyId);

    @Query("SELECT ru FROM RoomUnit ru WHERE " +
           "ru.roomType = :roomType AND " +
           "ru.floorNumber = :floorNumber")
    List<RoomUnit> findByRoomTypeAndFloorNumber(@Param("roomType") RoomType roomType,
                                               @Param("floorNumber") Integer floorNumber);

    @Query("SELECT ru FROM RoomUnit ru WHERE " +
           "ru.roomType = :roomType AND " +
           "ru.lastMaintenanceDate <= :beforeDate AND " +
           "ru.status != 'MAINTENANCE'")
    List<RoomUnit> findUnitsNeedingMaintenance(@Param("roomType") RoomType roomType,
                                              @Param("beforeDate") LocalDate beforeDate);

    @Query("SELECT ru FROM RoomUnit ru WHERE " +
           "ru.roomType.property.partner.id = :partnerId " +
           "ORDER BY ru.roomType.property.name, ru.roomType.typeName, ru.code")
    List<RoomUnit> findByPartnerId(@Param("partnerId") UUID partnerId);

    @Query("SELECT COUNT(ru) FROM RoomUnit ru WHERE " +
           "ru.roomType = :roomType AND " +
           "ru.status = :status")
    long countByRoomTypeAndStatus(@Param("roomType") RoomType roomType,
                                 @Param("status") RoomUnit.RoomStatus status);

    @Query("SELECT DISTINCT ru.floorNumber FROM RoomUnit ru WHERE " +
           "ru.roomType.property.id = :propertyId " +
           "ORDER BY ru.floorNumber")
    List<Integer> findDistinctFloorNumbersByProperty(@Param("propertyId") UUID propertyId);

    @Query("SELECT ru FROM RoomUnit ru WHERE " +
           "ru.roomType.property.id = :propertyId AND " +
           "ru.floorNumber = :floorNumber AND " +
           "ru.status = 'AVAILABLE'")
    List<RoomUnit> findAvailableUnitsByPropertyAndFloor(@Param("propertyId") UUID propertyId,
                                                       @Param("floorNumber") Integer floorNumber);

    boolean existsByRoomTypeAndCode(RoomType roomType, String code);
    boolean existsByRoomTypeAndRoomNumber(RoomType roomType, String roomNumber);
}