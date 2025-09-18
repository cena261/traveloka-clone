package com.cena.traveloka.catalog.inventory.repository;

import com.cena.traveloka.catalog.inventory.entity.Amenity;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface AmenityRepository extends JpaRepository<Amenity, UUID> {

    @Cacheable(value = "amenities", key = "#code")
    Optional<Amenity> findByCode(String code);

    @Cacheable(value = "amenities", key = "'category-' + #category")
    List<Amenity> findByCategory(Amenity.AmenityCategory category);

    @Cacheable(value = "amenities", key = "'popular'")
    @Query("SELECT a FROM Amenity a WHERE a.isPopular = true ORDER BY a.sortOrder ASC")
    List<Amenity> findPopularAmenities();

    @Query("SELECT a FROM Amenity a WHERE " +
           "(COALESCE(:category) IS NULL OR a.category = :category) AND " +
           "(COALESCE(:search) IS NULL OR LOWER(a.name) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Amenity> findByCategoryAndSearch(@Param("category") Amenity.AmenityCategory category,
                                         @Param("search") String search,
                                         Pageable pageable);

    @Query("SELECT a FROM Amenity a ORDER BY a.category ASC, a.sortOrder ASC, a.name ASC")
    List<Amenity> findAllOrderedByCategoryAndSort();

    @Query("SELECT a FROM Amenity a WHERE " +
           "a.category = :category " +
           "ORDER BY a.isPopular DESC, a.sortOrder ASC, a.name ASC")
    List<Amenity> findByCategoryOrderedByPopularityAndSort(@Param("category") Amenity.AmenityCategory category);

    @Query("SELECT DISTINCT a.category FROM Amenity a ORDER BY a.category")
    List<Amenity.AmenityCategory> findDistinctCategories();

    @Query("SELECT COUNT(a) FROM Amenity a WHERE a.category = :category")
    long countByCategory(@Param("category") Amenity.AmenityCategory category);

    @Query("SELECT a FROM Amenity a WHERE " +
           "LOWER(a.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(a.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Amenity> searchByKeyword(@Param("keyword") String keyword);

    boolean existsByCode(String code);
    boolean existsByName(String name);
}
