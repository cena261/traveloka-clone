package com.cena.traveloka.catalog.inventory.controller;

import com.cena.traveloka.catalog.inventory.dto.request.RoomUnitCreateReq;
import com.cena.traveloka.catalog.inventory.dto.request.RoomUnitUpdateReq;
import com.cena.traveloka.catalog.inventory.dto.response.RoomUnitRes;
import com.cena.traveloka.catalog.inventory.entity.RoomUnit;
import com.cena.traveloka.catalog.inventory.service.RoomUnitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/inventory/room-units")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Room Units", description = "Room unit management operations")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoomUnitController {
    RoomUnitService svc;

    @PostMapping
    @Operation(summary = "Create room unit", description = "Creates a new room unit")
    public ResponseEntity<RoomUnitRes> create(@RequestBody @Valid RoomUnitCreateReq req) {
        RoomUnitRes roomUnit = svc.create(req);
        return ResponseEntity.ok(roomUnit);
    }

    @GetMapping("/by-room-type/{roomTypeId}")
    @Operation(summary = "List room units by room type", description = "Retrieves all room units for a specific room type")
    public ResponseEntity<List<RoomUnitRes>> listByRoomType(
            @Parameter(description = "Room type ID", required = true) @PathVariable UUID roomTypeId) {
        List<RoomUnitRes> units = svc.listByRoomType(roomTypeId);
        return ResponseEntity.ok(units);
    }

    @GetMapping("/by-room-type/{roomTypeId}/status/{status}")
    @Operation(summary = "List room units by room type and status", description = "Retrieves room units filtered by room type and status")
    public ResponseEntity<List<RoomUnitRes>> findByRoomTypeAndStatus(
            @Parameter(description = "Room type ID", required = true) @PathVariable UUID roomTypeId,
            @Parameter(description = "Room status", required = true) @PathVariable RoomUnit.RoomStatus status) {
        List<RoomUnitRes> units = svc.findByRoomTypeAndStatus(roomTypeId, status);
        return ResponseEntity.ok(units);
    }

    @GetMapping("/available/property/{propertyId}")
    @Operation(summary = "List available units by property", description = "Retrieves all available room units for a property")
    public ResponseEntity<List<RoomUnitRes>> findAvailableUnitsByProperty(
            @Parameter(description = "Property ID", required = true) @PathVariable UUID propertyId) {
        List<RoomUnitRes> units = svc.findAvailableUnitsByProperty(propertyId);
        return ResponseEntity.ok(units);
    }

    @GetMapping("/by-room-type/{roomTypeId}/floor/{floorNumber}")
    @Operation(summary = "List units by room type and floor", description = "Retrieves room units by room type and floor number")
    public ResponseEntity<List<RoomUnitRes>> findByRoomTypeAndFloor(
            @Parameter(description = "Room type ID", required = true) @PathVariable UUID roomTypeId,
            @Parameter(description = "Floor number", required = true) @PathVariable Integer floorNumber) {
        List<RoomUnitRes> units = svc.findByRoomTypeAndFloor(roomTypeId, floorNumber);
        return ResponseEntity.ok(units);
    }

    @GetMapping("/maintenance-needed/{roomTypeId}")
    @Operation(summary = "List units needing maintenance", description = "Retrieves room units that need maintenance")
    public ResponseEntity<List<RoomUnitRes>> findUnitsNeedingMaintenance(
            @Parameter(description = "Room type ID", required = true) @PathVariable UUID roomTypeId,
            @Parameter(description = "Date before which maintenance is needed") @RequestParam(required = false) LocalDate beforeDate) {
        LocalDate checkDate = beforeDate != null ? beforeDate : LocalDate.now().minusMonths(6);
        List<RoomUnitRes> units = svc.findUnitsNeedingMaintenance(roomTypeId, checkDate);
        return ResponseEntity.ok(units);
    }

    @GetMapping("/by-room-type/{roomTypeId}/code/{code}")
    @Operation(summary = "Find unit by room type and code", description = "Retrieves a room unit by room type and code")
    public ResponseEntity<RoomUnitRes> findByRoomTypeAndCode(
            @Parameter(description = "Room type ID", required = true) @PathVariable UUID roomTypeId,
            @Parameter(description = "Room unit code", required = true) @PathVariable String code) {
        Optional<RoomUnitRes> unit = svc.findByRoomTypeAndCode(roomTypeId, code);
        return unit.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update room unit", description = "Updates an existing room unit")
    public ResponseEntity<RoomUnitRes> update(
            @Parameter(description = "Room unit ID", required = true) @PathVariable UUID id,
            @RequestBody @Valid RoomUnitUpdateReq req) {
        RoomUnitRes unit = svc.update(id, req);
        return ResponseEntity.ok(unit);
    }

    @PostMapping("/{id}/status")
    @Operation(summary = "Update room unit status", description = "Updates the status of a room unit")
    public ResponseEntity<RoomUnitRes> setStatus(
            @Parameter(description = "Room unit ID", required = true) @PathVariable UUID id,
            @Parameter(description = "New status", required = true) @RequestParam RoomUnit.RoomStatus status) {
        RoomUnitRes unit = svc.setStatus(id, status);
        return ResponseEntity.ok(unit);
    }

    @PostMapping("/{id}/maintenance")
    @Operation(summary = "Mark room unit for maintenance", description = "Marks a room unit for maintenance")
    public ResponseEntity<RoomUnitRes> markForMaintenance(
            @Parameter(description = "Room unit ID", required = true) @PathVariable UUID id,
            @Parameter(description = "Maintenance notes") @RequestParam(required = false) String notes) {
        RoomUnitRes unit = svc.markForMaintenance(id, notes);
        return ResponseEntity.ok(unit);
    }

    @PostMapping("/{id}/maintenance/complete")
    @Operation(summary = "Complete room unit maintenance", description = "Marks maintenance as completed for a room unit")
    public ResponseEntity<RoomUnitRes> completeMaintenance(
            @Parameter(description = "Room unit ID", required = true) @PathVariable UUID id) {
        RoomUnitRes unit = svc.completeMaintenance(id);
        return ResponseEntity.ok(unit);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete room unit", description = "Deletes a room unit (only if not occupied)")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Room unit ID", required = true) @PathVariable UUID id) {
        svc.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/count/by-room-type/{roomTypeId}/status/{status}")
    @Operation(summary = "Count units by status", description = "Counts room units by room type and status")
    public ResponseEntity<Long> countByRoomTypeAndStatus(
            @Parameter(description = "Room type ID", required = true) @PathVariable UUID roomTypeId,
            @Parameter(description = "Room status", required = true) @PathVariable RoomUnit.RoomStatus status) {
        long count = svc.countByRoomTypeAndStatus(roomTypeId, status);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/floors/property/{propertyId}")
    @Operation(summary = "Get distinct floors by property", description = "Retrieves all distinct floor numbers for a property")
    public ResponseEntity<List<Integer>> getDistinctFloorsByProperty(
            @Parameter(description = "Property ID", required = true) @PathVariable UUID propertyId) {
        List<Integer> floors = svc.getDistinctFloorsByProperty(propertyId);
        return ResponseEntity.ok(floors);
    }
}