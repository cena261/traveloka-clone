package com.cena.traveloka.catalog.inventory.service;

import com.cena.traveloka.common.exception.AppException;
import com.cena.traveloka.common.exception.ErrorCode;
import com.cena.traveloka.catalog.inventory.dto.request.RoomTypeCreateReq;
import com.cena.traveloka.catalog.inventory.dto.request.RoomTypeUpdateReq;
import com.cena.traveloka.catalog.inventory.dto.response.RoomTypeRes;
import com.cena.traveloka.catalog.inventory.entity.Property;
import com.cena.traveloka.catalog.inventory.entity.RoomType;
import com.cena.traveloka.catalog.inventory.repository.PropertyRepository;
import com.cena.traveloka.catalog.inventory.repository.RoomTypeRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Validated
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoomTypeService {
    RoomTypeRepository repo;
    PropertyRepository propertyRepo;

    @Transactional
    @CacheEvict(value = "roomTypes", allEntries = true)
    public RoomTypeRes create(@NotNull RoomTypeCreateReq req) {
        log.info("Creating new room type: {} for property: {}", req.getTypeName(), req.getPropertyId());

        Property property = propertyRepo.findById(req.getPropertyId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Property not found: " + req.getPropertyId()));

        validateRoomTypeCreation(property, req);

        RoomType roomType = RoomType.builder()
                .property(property)
                .typeCode(req.getTypeCode())
                .typeName(req.getTypeName())
                .description(req.getDescription())
                .maxOccupancy(req.getMaxOccupancy())
                .adultCapacity(req.getAdultCapacity())
                .childCapacity(req.getChildCapacity() != null ? req.getChildCapacity() : 0)
                .infantCapacity(req.getInfantCapacity() != null ? req.getInfantCapacity() : 0)
                .basePrice(req.getBasePrice())
                .currency(req.getCurrency() != null ? req.getCurrency() : "VND")
                .isRefundable(req.getIsRefundable() != null ? req.getIsRefundable() : true)
                .roomSize(req.getRoomSize())
                .bedType(req.getBedType())
                .smokingPolicy(req.getSmokingPolicy())
                .status(RoomType.RoomTypeStatus.ACTIVE)
                .build();

        roomType = repo.save(roomType);
        log.info("Room type created successfully with ID: {}", roomType.getId());

        return toRes(roomType);
    }

    @Cacheable(value = "roomTypes", key = "'property-' + #propertyId")
    public List<RoomTypeRes> listByProperty(@NotNull UUID propertyId) {
        Property property = propertyRepo.findById(propertyId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Property not found: " + propertyId));
        return repo.findByProperty(property).stream().map(this::toRes).collect(Collectors.toList());
    }

    public List<RoomTypeRes> findByPropertyAndStatus(UUID propertyId, RoomType.RoomTypeStatus status) {
        Property property = propertyRepo.findById(propertyId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Property not found: " + propertyId));
        return repo.findByPropertyAndStatus(property, status).stream().map(this::toRes).toList();
    }

    public List<RoomTypeRes> findByPropertyAndPriceRange(UUID propertyId, BigDecimal minPrice, BigDecimal maxPrice) {
        Property property = propertyRepo.findById(propertyId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Property not found: " + propertyId));
        return repo.findByPropertyAndPriceRange(property, minPrice, maxPrice).stream().map(this::toRes).toList();
    }

    public List<RoomTypeRes> findAvailableRoomTypes(UUID propertyId) {
        Property property = propertyRepo.findById(propertyId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Property not found: " + propertyId));
        return repo.findAvailableRoomTypesByProperty(property).stream().map(this::toRes).toList();
    }

    public Optional<RoomTypeRes> findByPropertyAndTypeCode(UUID propertyId, String typeCode) {
        Property property = propertyRepo.findById(propertyId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Property not found: " + propertyId));
        return repo.findByPropertyAndTypeCode(property, typeCode).map(this::toRes);
    }

    @Transactional
    @CacheEvict(value = "roomTypes", allEntries = true)
    public RoomTypeRes update(@NotNull UUID id, @NotNull RoomTypeUpdateReq req) {
        log.info("Updating room type: {}", id);

        RoomType roomType = repo.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "RoomType not found: " + id));

        validateRoomTypeUpdate(roomType, req);

        if (req.getTypeName() != null) roomType.setTypeName(req.getTypeName());
        if (req.getDescription() != null) roomType.setDescription(req.getDescription());
        if (req.getMaxOccupancy() != null) roomType.setMaxOccupancy(req.getMaxOccupancy());
        if (req.getAdultCapacity() != null) roomType.setAdultCapacity(req.getAdultCapacity());
        if (req.getChildCapacity() != null) roomType.setChildCapacity(req.getChildCapacity());
        if (req.getInfantCapacity() != null) roomType.setInfantCapacity(req.getInfantCapacity());
        if (req.getBasePrice() != null) roomType.setBasePrice(req.getBasePrice());
        if (req.getCurrency() != null) roomType.setCurrency(req.getCurrency());
        if (req.getIsRefundable() != null) roomType.setIsRefundable(req.getIsRefundable());
        if (req.getRoomSize() != null) roomType.setRoomSize(req.getRoomSize());
        if (req.getBedType() != null) roomType.setBedType(req.getBedType());
        if (req.getSmokingPolicy() != null) roomType.setSmokingPolicy(req.getSmokingPolicy());
        if (req.getStatus() != null) roomType.setStatus(req.getStatus());

        roomType = repo.save(roomType);
        log.info("Room type updated successfully: {}", id);

        return toRes(roomType);
    }

    @Transactional
    @CacheEvict(value = "roomTypes", allEntries = true)
    public RoomTypeRes deactivateRoomType(@NotNull UUID id) {
        RoomType roomType = repo.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "RoomType not found: " + id));

        if (roomType.getStatus() != RoomType.RoomTypeStatus.ACTIVE) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                "Only active room types can be deactivated");
        }

        roomType.setStatus(RoomType.RoomTypeStatus.INACTIVE);
        roomType = repo.save(roomType);

        log.info("Room type deactivated: {}", id);
        return toRes(roomType);
    }

    @Transactional
    @CacheEvict(value = "roomTypes", allEntries = true)
    public void delete(@NotNull UUID id) {
        RoomType roomType = repo.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "RoomType not found: " + id));

        if (roomType.getStatus() == RoomType.RoomTypeStatus.ACTIVE) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                "Cannot delete active room type. Deactivate first.");
        }

        long availableUnits = repo.countAvailableUnits(roomType);
        if (availableUnits > 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                "Cannot delete room type with available units");
        }

        repo.delete(roomType);
        log.info("Room type deleted: {}", id);
    }

    public long getAvailableUnitsCount(UUID roomTypeId) {
        RoomType roomType = repo.findById(roomTypeId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "RoomType not found: " + roomTypeId));
        return repo.countAvailableUnits(roomType);
    }

    public BigDecimal getAveragePriceByProperty(UUID propertyId) {
        Property property = propertyRepo.findById(propertyId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Property not found: " + propertyId));
        return repo.getAveragePriceByProperty(property);
    }

    private void validateRoomTypeCreation(Property property, RoomTypeCreateReq req) {
        if (repo.existsByPropertyAndTypeCode(property, req.getTypeCode())) {
            throw new AppException(ErrorCode.ALREADY_EXISTS,
                "Room type with code already exists for this property: " + req.getTypeCode());
        }
    }

    private void validateRoomTypeUpdate(RoomType existing, RoomTypeUpdateReq req) {
        if (existing.getStatus() == RoomType.RoomTypeStatus.ACTIVE &&
            req.getStatus() != null && req.getStatus() != RoomType.RoomTypeStatus.ACTIVE) {
            log.warn("Attempting to change status of active room type: {}", existing.getId());
        }
    }

    private RoomTypeRes toRes(RoomType roomType) {
        return RoomTypeRes.builder()
                .id(roomType.getId())
                .propertyId(roomType.getProperty().getId())
                .typeCode(roomType.getTypeCode())
                .typeName(roomType.getTypeName())
                .description(roomType.getDescription())
                .maxOccupancy(roomType.getMaxOccupancy())
                .adultCapacity(roomType.getAdultCapacity())
                .childCapacity(roomType.getChildCapacity())
                .infantCapacity(roomType.getInfantCapacity())
                .basePrice(roomType.getBasePrice())
                .currency(roomType.getCurrency())
                .isRefundable(roomType.getIsRefundable())
                .roomSize(roomType.getRoomSize())
                .bedType(roomType.getBedType())
                .smokingPolicy(roomType.getSmokingPolicy())
                .status(roomType.getStatus())
                .createdAt(roomType.getCreatedAt())
                .updatedAt(roomType.getUpdatedAt())
                .build();
    }
}
