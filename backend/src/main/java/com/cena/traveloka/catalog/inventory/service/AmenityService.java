package com.cena.traveloka.catalog.inventory.service;

import com.cena.traveloka.common.exception.AppException;
import com.cena.traveloka.common.exception.ErrorCode;
import com.cena.traveloka.catalog.inventory.dto.request.AmenityCreateReq;
import com.cena.traveloka.catalog.inventory.dto.request.AmenityUpdateReq;
import com.cena.traveloka.catalog.inventory.dto.response.AmenityRes;
import com.cena.traveloka.catalog.inventory.entity.Amenity;
import com.cena.traveloka.catalog.inventory.repository.AmenityRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Validated
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AmenityService {
    AmenityRepository repo;

    @Transactional
    @CacheEvict(value = "amenities", allEntries = true)
    public AmenityRes create(@NotNull AmenityCreateReq req) {
        log.info("Creating new amenity: {} - {}", req.getCode(), req.getName());

        validateAmenityCreation(req);

        Amenity amenity = Amenity.builder()
                .code(req.getCode())
                .name(req.getName())
                .description(req.getDescription())
                .category(req.getCategory() != null ? req.getCategory() : Amenity.AmenityCategory.GENERAL)
                .icon(req.getIcon())
                .isPopular(req.getIsPopular() != null ? req.getIsPopular() : false)
                .sortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0)
                .build();

        amenity = repo.save(amenity);
        log.info("Amenity created successfully with ID: {}", amenity.getId());

        return toRes(amenity);
    }

    @Cacheable(value = "amenities", key = "'all'")
    public List<AmenityRes> list() {
        return repo.findAllOrderedByCategoryAndSort().stream().map(this::toRes).toList();
    }

    public Page<AmenityRes> findByCategoryAndSearch(Amenity.AmenityCategory category, String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("category").and(Sort.by("sortOrder")).and(Sort.by("name")));
        return repo.findByCategoryAndSearch(category, search, pageable).map(this::toRes);
    }

    @Cacheable(value = "amenities", key = "'category-' + #category")
    public List<AmenityRes> findByCategory(Amenity.AmenityCategory category) {
        return repo.findByCategoryOrderedByPopularityAndSort(category).stream().map(this::toRes).toList();
    }

    @Cacheable(value = "amenities", key = "'popular'")
    public List<AmenityRes> getPopularAmenities() {
        return repo.findPopularAmenities().stream().map(this::toRes).toList();
    }

    public List<AmenityRes> searchByKeyword(String keyword) {
        return repo.searchByKeyword(keyword).stream().map(this::toRes).toList();
    }

    public Optional<AmenityRes> findByCode(String code) {
        return repo.findByCode(code).map(this::toRes);
    }

    public List<Amenity.AmenityCategory> getDistinctCategories() {
        return repo.findDistinctCategories();
    }

    @Transactional
    @CacheEvict(value = "amenities", allEntries = true)
    public AmenityRes update(@NotNull UUID id, @NotNull AmenityUpdateReq req) {
        log.info("Updating amenity: {}", id);

        Amenity amenity = repo.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Amenity not found: " + id));

        validateAmenityUpdate(amenity, req);

        if (req.getName() != null) amenity.setName(req.getName());
        if (req.getDescription() != null) amenity.setDescription(req.getDescription());
        if (req.getCategory() != null) amenity.setCategory(req.getCategory());
        if (req.getIcon() != null) amenity.setIcon(req.getIcon());
        if (req.getIsPopular() != null) amenity.setIsPopular(req.getIsPopular());
        if (req.getSortOrder() != null) amenity.setSortOrder(req.getSortOrder());

        amenity = repo.save(amenity);
        log.info("Amenity updated successfully: {}", id);

        return toRes(amenity);
    }

    @Transactional
    @CacheEvict(value = "amenities", allEntries = true)
    public void delete(@NotNull UUID id) {
        Amenity amenity = repo.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Amenity not found: " + id));

        repo.delete(amenity);
        log.info("Amenity deleted: {}", id);
    }

    public long countByCategory(Amenity.AmenityCategory category) {
        return repo.countByCategory(category);
    }

    private void validateAmenityCreation(AmenityCreateReq req) {
        if (repo.existsByCode(req.getCode())) {
            throw new AppException(ErrorCode.ALREADY_EXISTS,
                "Amenity with code already exists: " + req.getCode());
        }

        if (repo.existsByName(req.getName())) {
            throw new AppException(ErrorCode.ALREADY_EXISTS,
                "Amenity with name already exists: " + req.getName());
        }
    }

    private void validateAmenityUpdate(Amenity existing, AmenityUpdateReq req) {
        if (req.getName() != null && !req.getName().equals(existing.getName())) {
            if (repo.existsByName(req.getName())) {
                throw new AppException(ErrorCode.ALREADY_EXISTS,
                    "Amenity with name already exists: " + req.getName());
            }
        }
    }

    private AmenityRes toRes(Amenity amenity) {
        return AmenityRes.builder()
                .id(amenity.getId())
                .code(amenity.getCode())
                .name(amenity.getName())
                .description(amenity.getDescription())
                .category(amenity.getCategory())
                .icon(amenity.getIcon())
                .isPopular(amenity.getIsPopular())
                .sortOrder(amenity.getSortOrder())
                .createdAt(amenity.getCreatedAt())
                .updatedAt(amenity.getUpdatedAt())
                .build();
    }
}
