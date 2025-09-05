package com.cena.traveloka.inventory.controller;

import com.cena.traveloka.inventory.dto.request.AmenityCreateReq;
import com.cena.traveloka.inventory.dto.request.AmenityUpdateReq;
import com.cena.traveloka.inventory.dto.response.AmenityRes;
import com.cena.traveloka.inventory.service.AmenityService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/inventory/amenities")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AmenityController {
    AmenityService svc;

    @PostMapping
    public AmenityRes create(@RequestBody @Valid AmenityCreateReq req) {
        return svc.create(req);
    }

    @GetMapping
    public List<AmenityRes> list() {
        return svc.list();
    }

    @PutMapping("/{id}")
    public AmenityRes update(@PathVariable UUID id, @RequestBody @Valid AmenityUpdateReq req) {
        return svc.update(id, req);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        svc.delete(id);
    }
}
