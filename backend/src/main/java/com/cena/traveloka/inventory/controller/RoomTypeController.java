package com.cena.traveloka.inventory.controller;

import com.cena.traveloka.inventory.dto.request.RoomTypeCreateReq;
import com.cena.traveloka.inventory.dto.request.RoomTypeUpdateReq;
import com.cena.traveloka.inventory.dto.response.RoomTypeRes;
import com.cena.traveloka.inventory.service.RoomTypeService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/inventory/room-types")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoomTypeController {
    RoomTypeService svc;

    @PostMapping
    public RoomTypeRes create(@RequestBody @Valid RoomTypeCreateReq req) {
        return svc.create(req);
    }

    @GetMapping
    public List<RoomTypeRes> listByProperty(@RequestParam UUID propertyId) {
        return svc.listByProperty(propertyId);
    }

    @PutMapping("/{id}")
    public RoomTypeRes update(@PathVariable UUID id, @RequestBody @Valid RoomTypeUpdateReq req) {
        return svc.update(id, req);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        svc.delete(id);
    }
}
