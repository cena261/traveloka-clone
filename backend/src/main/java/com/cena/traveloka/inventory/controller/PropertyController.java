package com.cena.traveloka.inventory.controller;

import com.cena.traveloka.dto.response.common.PageResponse;
import com.cena.traveloka.inventory.dto.request.PropertyAmenityBindReq;
import com.cena.traveloka.inventory.dto.request.PropertyCreateReq;
import com.cena.traveloka.inventory.dto.request.PropertyUpdateReq;
import com.cena.traveloka.inventory.dto.response.PropertyRes;
import com.cena.traveloka.inventory.service.PropertyService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/inventory/properties")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PropertyController {
    PropertyService svc;

    @PostMapping
    public PropertyRes create(@RequestBody @Valid PropertyCreateReq req) {
        return svc.create(req);
    }

    @GetMapping
    public PageResponse<PropertyRes> listByPartner(@RequestParam UUID partnerId,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        Page<PropertyRes> p = svc.listByPartner(partnerId, page, size);
        return PageResponse.from(p);
    }

    @GetMapping("/{id}")
    public PropertyRes get(@PathVariable UUID id) {
        return svc.get(id);
    }

    @PutMapping("/{id}")
    public PropertyRes update(@PathVariable UUID id, @RequestBody @Valid PropertyUpdateReq req) {
        return svc.update(id, req);
    }

    @PostMapping("/{id}/amenities")
    public PropertyRes bindAmenities(@PathVariable UUID id, @RequestBody @Valid PropertyAmenityBindReq req) {
        return svc.bindAmenities(id, req);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        svc.delete(id);
    }
}
