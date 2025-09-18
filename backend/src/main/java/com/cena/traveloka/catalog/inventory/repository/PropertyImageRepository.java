package com.cena.traveloka.catalog.inventory.repository;

import com.cena.traveloka.catalog.inventory.entity.Property;
import com.cena.traveloka.catalog.inventory.entity.PropertyImage;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PropertyImageRepository extends JpaRepository<PropertyImage, UUID> {

    @Cacheable(value = "propertyImages", key = "#property.id")
    List<PropertyImage> findByPropertyOrderBySortOrderAsc(Property property);

    Optional<PropertyImage> findTopByPropertyOrderBySortOrderDesc(Property property);

    @Query("SELECT pi FROM PropertyImage pi WHERE " +
           "pi.property = :property AND " +
           "pi.imageType = :imageType " +
           "ORDER BY pi.sortOrder ASC")
    List<PropertyImage> findByPropertyAndImageType(@Param("property") Property property,
                                                  @Param("imageType") PropertyImage.ImageType imageType);

    @Query("SELECT pi FROM PropertyImage pi WHERE " +
           "pi.property = :property AND " +
           "pi.isPrimary = true")
    Optional<PropertyImage> findPrimaryImageByProperty(@Param("property") Property property);

    @Query("SELECT COUNT(pi) FROM PropertyImage pi WHERE " +
           "pi.property = :property AND " +
           "pi.imageType = :imageType")
    long countByPropertyAndImageType(@Param("property") Property property,
                                   @Param("imageType") PropertyImage.ImageType imageType);

    @Query("SELECT pi FROM PropertyImage pi WHERE " +
           "pi.property = :property AND " +
           "pi.imageType = :imageType " +
           "ORDER BY pi.sortOrder ASC")
    List<PropertyImage> findGalleryImages(@Param("property") Property property,
                                         @Param("imageType") PropertyImage.ImageType imageType);

    @Query("SELECT MAX(pi.sortOrder) FROM PropertyImage pi WHERE pi.property = :property")
    Integer findMaxSortOrderByProperty(@Param("property") Property property);

    @Query("SELECT pi FROM PropertyImage pi WHERE " +
           "pi.property.partner.id = :partnerId " +
           "ORDER BY pi.property.id, pi.sortOrder ASC")
    List<PropertyImage> findByPartnerId(@Param("partnerId") UUID partnerId);

    boolean existsByPropertyAndIsPrimary(Property property, Boolean isPrimary);
    boolean existsByImageUrl(String imageUrl);
}

