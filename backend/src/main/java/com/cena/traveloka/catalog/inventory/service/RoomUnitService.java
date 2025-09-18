package com.cena.traveloka.catalog.inventory.service;

import com.cena.traveloka.common.exception.AppException;
import com.cena.traveloka.common.exception.ErrorCode;
import com.cena.traveloka.catalog.inventory.dto.request.RoomUnitCreateReq;
import com.cena.traveloka.catalog.inventory.dto.request.RoomUnitUpdateReq;
import com.cena.traveloka.catalog.inventory.dto.response.RoomUnitRes;
import com.cena.traveloka.catalog.inventory.entity.RoomType;
import com.cena.traveloka.catalog.inventory.entity.RoomUnit;
import com.cena.traveloka.catalog.inventory.repository.RoomTypeRepository;
import com.cena.traveloka.catalog.inventory.repository.RoomUnitRepository;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Validated
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoomUnitService {
    RoomUnitRepository repo;
    RoomTypeRepository roomTypeRepo;

    @Transactional
    @CacheEvict(value = "roomUnits", allEntries = true)
    public RoomUnitRes create(@NotNull RoomUnitCreateReq req) {
        log.info("Creating new room unit: {} for room type: {}", req.getCode(), req.getRoomTypeId());

        RoomType roomType = roomTypeRepo.findById(req.getRoomTypeId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Room type not found: " + req.getRoomTypeId()));

        validateRoomUnitCreation(roomType, req);

        RoomUnit roomUnit = RoomUnit.builder()
                .roomType(roomType)
                .code(req.getCode())
                .roomNumber(req.getRoomNumber() != null ? req.getRoomNumber() : req.getCode())
                .floorNumber(req.getFloorNumber())
                .status(RoomUnit.RoomStatus.AVAILABLE)
                .lastMaintenanceDate(req.getLastMaintenanceDate())
                .notes(req.getNotes())
                .build();

        roomUnit = repo.save(roomUnit);
        log.info("Room unit created successfully with ID: {}", roomUnit.getId());

        return toRes(roomUnit);
    }

    @Cacheable(value = "roomUnits", key = "'roomType-' + #roomTypeId")
    public List<RoomUnitRes> listByRoomType(@NotNull UUID roomTypeId) {
        RoomType roomType = roomTypeRepo.findById(roomTypeId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Room type not found: " + roomTypeId));
        return repo.findByRoomType(roomType).stream().map(this::toRes).toList();
    }

    public List<RoomUnitRes> findByRoomTypeAndStatus(UUID roomTypeId, RoomUnit.RoomStatus status) {
        RoomType roomType = roomTypeRepo.findById(roomTypeId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Room type not found: " + roomTypeId));
        return repo.findByRoomTypeAndStatus(roomType, status).stream().map(this::toRes).toList();
    }

    public List<RoomUnitRes> findAvailableUnitsByProperty(UUID propertyId) {
        return repo.findAvailableUnitsByProperty(propertyId).stream().map(this::toRes).toList();
    }

    public List<RoomUnitRes> findByRoomTypeAndFloor(UUID roomTypeId, Integer floorNumber) {
        RoomType roomType = roomTypeRepo.findById(roomTypeId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Room type not found: " + roomTypeId));
        return repo.findByRoomTypeAndFloorNumber(roomType, floorNumber).stream().map(this::toRes).toList();
    }

    public List<RoomUnitRes> findUnitsNeedingMaintenance(UUID roomTypeId, LocalDate beforeDate) {
        RoomType roomType = roomTypeRepo.findById(roomTypeId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Room type not found: " + roomTypeId));
        return repo.findUnitsNeedingMaintenance(roomType, beforeDate).stream().map(this::toRes).toList();
    }

    public Optional<RoomUnitRes> findByRoomTypeAndCode(UUID roomTypeId, String code) {
        RoomType roomType = roomTypeRepo.findById(roomTypeId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Room type not found: " + roomTypeId));
        return repo.findByRoomTypeAndCode(roomType, code).map(this::toRes);
    }

    @Transactional
    @CacheEvict(value = "roomUnits", allEntries = true)
    public RoomUnitRes update(@NotNull UUID id, @NotNull RoomUnitUpdateReq req) {
        log.info("Updating room unit: {}", id);

        RoomUnit roomUnit = repo.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Room unit not found: " + id));

        validateRoomUnitUpdate(roomUnit, req);

        if (req.getRoomNumber() != null) roomUnit.setRoomNumber(req.getRoomNumber());
        if (req.getFloorNumber() != null) roomUnit.setFloorNumber(req.getFloorNumber());
        if (req.getStatus() != null) roomUnit.setStatus(req.getStatus());
        if (req.getLastMaintenanceDate() != null) roomUnit.setLastMaintenanceDate(req.getLastMaintenanceDate());
        if (req.getNotes() != null) roomUnit.setNotes(req.getNotes());

        roomUnit = repo.save(roomUnit);
        log.info("Room unit updated successfully: {}", id);

        return toRes(roomUnit);
    }

    @Transactional
    @CacheEvict(value = "roomUnits", allEntries = true)
    public RoomUnitRes setStatus(@NotNull UUID id, @NotNull RoomUnit.RoomStatus status) {
        RoomUnit roomUnit = repo.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Room unit not found: " + id));

        roomUnit.setStatus(status);
        roomUnit = repo.save(roomUnit);

        log.info("Room unit status changed to {}: {}", status, id);
        return toRes(roomUnit);
    }

    @Transactional
    @CacheEvict(value = "roomUnits", allEntries = true)
    public RoomUnitRes markForMaintenance(@NotNull UUID id, String notes) {
        RoomUnit roomUnit = repo.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Room unit not found: " + id));

        if (roomUnit.getStatus() == RoomUnit.RoomStatus.OCCUPIED) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                "Cannot mark occupied room for maintenance");
        }

        roomUnit.setStatus(RoomUnit.RoomStatus.MAINTENANCE);
        if (notes != null) {
            roomUnit.setNotes(notes);
        }
        roomUnit = repo.save(roomUnit);

        log.info("Room unit marked for maintenance: {}", id);
        return toRes(roomUnit);
    }

    @Transactional
    @CacheEvict(value = "roomUnits", allEntries = true)
    public RoomUnitRes completeMaintenance(@NotNull UUID id) {
        RoomUnit roomUnit = repo.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Room unit not found: " + id));

        if (roomUnit.getStatus() != RoomUnit.RoomStatus.MAINTENANCE) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                "Room unit is not in maintenance status");
        }

        roomUnit.setStatus(RoomUnit.RoomStatus.AVAILABLE);
        roomUnit.setLastMaintenanceDate(LocalDate.now());
        roomUnit = repo.save(roomUnit);

        log.info("Room unit maintenance completed: {}", id);
        return toRes(roomUnit);
    }

    @Transactional
    @CacheEvict(value = "roomUnits", allEntries = true)
    public void delete(@NotNull UUID id) {
        RoomUnit roomUnit = repo.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Room unit not found: " + id));

        if (roomUnit.getStatus() == RoomUnit.RoomStatus.OCCUPIED) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                "Cannot delete occupied room unit");
        }

        repo.delete(roomUnit);
        log.info("Room unit deleted: {}", id);
    }

    public long countByRoomTypeAndStatus(UUID roomTypeId, RoomUnit.RoomStatus status) {
        RoomType roomType = roomTypeRepo.findById(roomTypeId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Room type not found: " + roomTypeId));
        return repo.countByRoomTypeAndStatus(roomType, status);
    }

    public List<Integer> getDistinctFloorsByProperty(UUID propertyId) {
        return repo.findDistinctFloorNumbersByProperty(propertyId);
    }

    private void validateRoomUnitCreation(RoomType roomType, RoomUnitCreateReq req) {
        if (repo.existsByRoomTypeAndCode(roomType, req.getCode())) {
            throw new AppException(ErrorCode.ALREADY_EXISTS,
                "Room unit with code already exists for this room type: " + req.getCode());
        }

        if (req.getRoomNumber() != null && repo.existsByRoomTypeAndRoomNumber(roomType, req.getRoomNumber())) {
            throw new AppException(ErrorCode.ALREADY_EXISTS,
                "Room unit with room number already exists for this room type: " + req.getRoomNumber());
        }
    }

    private void validateRoomUnitUpdate(RoomUnit existing, RoomUnitUpdateReq req) {
        if (req.getRoomNumber() != null && !req.getRoomNumber().equals(existing.getRoomNumber())) {
            if (repo.existsByRoomTypeAndRoomNumber(existing.getRoomType(), req.getRoomNumber())) {
                throw new AppException(ErrorCode.ALREADY_EXISTS,
                    "Room unit with room number already exists for this room type: " + req.getRoomNumber());
            }
        }
    }

    private RoomUnitRes toRes(RoomUnit roomUnit) {
        return RoomUnitRes.builder()
                .id(roomUnit.getId())
                .roomTypeId(roomUnit.getRoomType().getId())
                .code(roomUnit.getCode())
                .roomNumber(roomUnit.getRoomNumber())
                .floorNumber(roomUnit.getFloorNumber())
                .status(roomUnit.getStatus())
                .lastMaintenanceDate(roomUnit.getLastMaintenanceDate())
                .notes(roomUnit.getNotes())
                .createdAt(roomUnit.getCreatedAt())
                .updatedAt(roomUnit.getUpdatedAt())
                .build();
    }
}