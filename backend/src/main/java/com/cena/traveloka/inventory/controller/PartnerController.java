package com.cena.traveloka.inventory.controller;

import com.cena.traveloka.dto.response.common.PageResponse;
import com.cena.traveloka.inventory.dto.request.PartnerCreateReq;
import com.cena.traveloka.inventory.dto.request.PartnerUpdateReq;
import com.cena.traveloka.inventory.dto.response.PartnerRes;
import com.cena.traveloka.inventory.service.PartnerService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/inventory/partners")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PartnerController {
    PartnerService svc;

    @PostMapping
    public PartnerRes create(@RequestBody @Valid PartnerCreateReq req) {
        return svc.create(req);
    }

    @GetMapping
    public PageResponse<PartnerRes> list(@RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "20") int size) {
        Page<PartnerRes> p = svc.list(page, size);
        return PageResponse.from(p);
    }

    @GetMapping("/{id}")
    public PartnerRes get(@PathVariable UUID id) {
        return svc.get(id);
    }

    @PutMapping("/{id}")
    public PartnerRes update(@PathVariable UUID id, @RequestBody @Valid PartnerUpdateReq req) {
        return svc.update(id, req);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        svc.delete(id);
    }
}
