package com.cena.traveloka.catalog.inventory.controller;

import com.cena.traveloka.common.PageResponse;
import com.cena.traveloka.catalog.inventory.dto.request.PartnerCreateReq;
import com.cena.traveloka.catalog.inventory.dto.request.PartnerUpdateReq;
import com.cena.traveloka.catalog.inventory.dto.response.PartnerRes;
import com.cena.traveloka.catalog.inventory.entity.Partner;
import com.cena.traveloka.catalog.inventory.service.PartnerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/inventory/partners")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Partners", description = "Partner management operations")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PartnerController {
    PartnerService svc;

    @PostMapping
    @Operation(summary = "Create new partner", description = "Creates a new partner with the provided information")
    public ResponseEntity<PartnerRes> create(@RequestBody @Valid PartnerCreateReq req) {
        PartnerRes partner = svc.create(req);
        return ResponseEntity.ok(partner);
    }

    @GetMapping
    @Operation(summary = "List partners", description = "Retrieves a paginated list of all partners")
    public ResponseEntity<PageResponse<PartnerRes>> list(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") @Min(1) int size) {
        Page<PartnerRes> partners = svc.list(page, size);
        return ResponseEntity.ok(PageResponse.from(partners));
    }

    @GetMapping("/search")
    @Operation(summary = "Search partners", description = "Search partners by status and/or name")
    public ResponseEntity<PageResponse<PartnerRes>> search(
            @Parameter(description = "Partner status") @RequestParam(required = false) Partner.PartnerStatus status,
            @Parameter(description = "Search term for partner name") @RequestParam(required = false) String search,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") @Min(1) int size) {
        Page<PartnerRes> partners = svc.searchPartners(status, search, page, size);
        return ResponseEntity.ok(PageResponse.from(partners));
    }

    @GetMapping("/by-status")
    @Operation(summary = "List partners by status", description = "Retrieves partners filtered by status")
    public ResponseEntity<PageResponse<PartnerRes>> findByStatus(
            @Parameter(description = "Partner status", required = true) @RequestParam Partner.PartnerStatus status,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") @Min(1) int size) {
        Page<PartnerRes> partners = svc.findByStatus(status, page, size);
        return ResponseEntity.ok(PageResponse.from(partners));
    }

    @GetMapping("/by-email")
    @Operation(summary = "Find partner by email", description = "Retrieves a partner by email address")
    public ResponseEntity<PartnerRes> findByEmail(
            @Parameter(description = "Email address", required = true) @RequestParam String email) {
        Optional<PartnerRes> partner = svc.findByEmail(email);
        return partner.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/top-performing")
    @Operation(summary = "Get top performing partners", description = "Retrieves partners with high performance ratings")
    public ResponseEntity<List<PartnerRes>> getTopPerforming(
            @Parameter(description = "Minimum bookings") @RequestParam(defaultValue = "100") Integer minBookings,
            @Parameter(description = "Minimum rating") @RequestParam(defaultValue = "4.0") Double minRating,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") @Min(1) int size) {
        List<PartnerRes> partners = svc.getTopPerformingPartners(minBookings, minRating, page, size);
        return ResponseEntity.ok(partners);
    }

    @GetMapping("/expiring-contracts")
    @Operation(summary = "Get partners with expiring contracts", description = "Retrieves partners whose contracts expire within 30 days")
    public ResponseEntity<List<PartnerRes>> getPartnersWithExpiringContracts() {
        List<PartnerRes> partners = svc.getPartnersWithExpiringContracts();
        return ResponseEntity.ok(partners);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get partner by ID", description = "Retrieves a specific partner by their ID")
    public ResponseEntity<PartnerRes> get(
            @Parameter(description = "Partner ID", required = true) @PathVariable UUID id) {
        PartnerRes partner = svc.get(id);
        return ResponseEntity.ok(partner);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update partner", description = "Updates an existing partner's information")
    public ResponseEntity<PartnerRes> update(
            @Parameter(description = "Partner ID", required = true) @PathVariable UUID id,
            @RequestBody @Valid PartnerUpdateReq req) {
        PartnerRes partner = svc.update(id, req);
        return ResponseEntity.ok(partner);
    }

    @PostMapping("/{id}/activate")
    @Operation(summary = "Activate partner", description = "Activates a pending partner")
    public ResponseEntity<PartnerRes> activate(
            @Parameter(description = "Partner ID", required = true) @PathVariable UUID id) {
        PartnerRes partner = svc.activatePartner(id);
        return ResponseEntity.ok(partner);
    }

    @PostMapping("/{id}/suspend")
    @Operation(summary = "Suspend partner", description = "Suspends an active partner")
    public ResponseEntity<PartnerRes> suspend(
            @Parameter(description = "Partner ID", required = true) @PathVariable UUID id,
            @Parameter(description = "Suspension reason") @RequestParam(required = false) String reason) {
        PartnerRes partner = svc.suspendPartner(id, reason);
        return ResponseEntity.ok(partner);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete partner", description = "Deletes a partner (only if not active)")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Partner ID", required = true) @PathVariable UUID id) {
        svc.delete(id);
        return ResponseEntity.noContent().build();
    }
}
