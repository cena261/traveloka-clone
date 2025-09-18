package com.cena.traveloka.catalog.inventory.service;

import com.cena.traveloka.common.exception.AppException;
import com.cena.traveloka.common.exception.ErrorCode;
import com.cena.traveloka.catalog.inventory.dto.request.PartnerCreateReq;
import com.cena.traveloka.catalog.inventory.dto.request.PartnerUpdateReq;
import com.cena.traveloka.catalog.inventory.dto.response.PartnerRes;
import com.cena.traveloka.catalog.inventory.entity.Partner;
import com.cena.traveloka.catalog.inventory.repository.PartnerRepository;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Validated
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PartnerService {
    PartnerRepository repo;

    @Transactional
    @CacheEvict(value = "partners", allEntries = true)
    public PartnerRes create(@NotNull PartnerCreateReq req) {
        log.info("Creating new partner: {}", req.getName());

        validatePartnerCreation(req);

        Partner partner = Partner.builder()
                .ownerUserId(req.getOwnerUserId())
                .name(req.getName())
                .legalName(req.getLegalName())
                .taxNumber(req.getTaxNumber())
                .businessRegistrationNumber(req.getBusinessRegistrationNumber())
                .email(req.getEmail())
                .phoneNumber(req.getPhoneNumber())
                .address(req.getAddress())
                .city(req.getCity())
                .country(req.getCountry())
                .postalCode(req.getPostalCode())
                .website(req.getWebsite())
                .commissionRate(req.getCommissionRate())
                .status(Partner.PartnerStatus.PENDING)
                .build();

        partner = repo.save(partner);
        log.info("Partner created successfully with ID: {}", partner.getId());

        return toRes(partner);
    }

    @Cacheable(value = "partners", key = "'list-' + #page + '-' + #size")
    public Page<PartnerRes> list(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Partner> partners = repo.findAll(pageable);
        return partners.map(this::toRes);
    }

    public Page<PartnerRes> findByStatus(Partner.PartnerStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Partner> partners = repo.findByStatus(status, pageable);
        return partners.map(this::toRes);
    }

    public Page<PartnerRes> searchPartners(Partner.PartnerStatus status, String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Partner> partners = repo.findByStatusAndSearch(status, search, pageable);
        return partners.map(this::toRes);
    }

    @Cacheable(value = "partners", key = "#id")
    public PartnerRes get(@NotNull UUID id) {
        return toRes(find(id));
    }

    public Optional<PartnerRes> findByEmail(String email) {
        return repo.findByEmail(email).map(this::toRes);
    }

    public Optional<PartnerRes> findByBusinessRegistrationNumber(String businessRegistrationNumber) {
        return repo.findByBusinessRegistrationNumber(businessRegistrationNumber).map(this::toRes);
    }

    @Transactional
    @CacheEvict(value = "partners", allEntries = true)
    public PartnerRes update(@NotNull UUID id, @NotNull PartnerUpdateReq req) {
        log.info("Updating partner: {}", id);

        Partner partner = find(id);
        validatePartnerUpdate(partner, req);

        if (req.getName() != null) partner.setName(req.getName());
        if (req.getLegalName() != null) partner.setLegalName(req.getLegalName());
        if (req.getTaxNumber() != null) partner.setTaxNumber(req.getTaxNumber());
        if (req.getEmail() != null) partner.setEmail(req.getEmail());
        if (req.getPhoneNumber() != null) partner.setPhoneNumber(req.getPhoneNumber());
        if (req.getAddress() != null) partner.setAddress(req.getAddress());
        if (req.getCity() != null) partner.setCity(req.getCity());
        if (req.getCountry() != null) partner.setCountry(req.getCountry());
        if (req.getPostalCode() != null) partner.setPostalCode(req.getPostalCode());
        if (req.getWebsite() != null) partner.setWebsite(req.getWebsite());
        if (req.getCommissionRate() != null) partner.setCommissionRate(req.getCommissionRate());
        if (req.getStatus() != null) partner.setStatus(req.getStatus());

        partner = repo.save(partner);
        log.info("Partner updated successfully: {}", id);

        return toRes(partner);
    }

    @Transactional
    @CacheEvict(value = "partners", allEntries = true)
    public PartnerRes activatePartner(@NotNull UUID id) {
        Partner partner = find(id);
        if (partner.getStatus() != Partner.PartnerStatus.PENDING) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                "Partner must be in PENDING status to activate");
        }

        partner.setStatus(Partner.PartnerStatus.ACTIVE);
        partner.setContractStartDate(LocalDate.now());
        partner = repo.save(partner);

        log.info("Partner activated: {}", id);
        return toRes(partner);
    }

    @Transactional
    @CacheEvict(value = "partners", allEntries = true)
    public PartnerRes suspendPartner(@NotNull UUID id, String reason) {
        Partner partner = find(id);
        if (partner.getStatus() != Partner.PartnerStatus.ACTIVE) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                "Only active partners can be suspended");
        }

        partner.setStatus(Partner.PartnerStatus.SUSPENDED);
        partner = repo.save(partner);

        log.info("Partner suspended: {} - Reason: {}", id, reason);
        return toRes(partner);
    }

    @Transactional
    @CacheEvict(value = "partners", allEntries = true)
    public void delete(@NotNull UUID id) {
        Partner partner = find(id);
        if (partner.getStatus() == Partner.PartnerStatus.ACTIVE) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                "Cannot delete active partner. Suspend first.");
        }

        repo.delete(partner);
        log.info("Partner deleted: {}", id);
    }

    public List<PartnerRes> getTopPerformingPartners(Integer minBookings, Double minRating, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Partner> partners = repo.findTopPerformingPartners(minBookings, minRating, pageable);
        return partners.getContent().stream().map(this::toRes).toList();
    }

    public List<PartnerRes> getPartnersWithExpiringContracts() {
        List<Partner> partners = repo.findPartnersWithExpiringContracts();
        return partners.stream().map(this::toRes).toList();
    }

    private Partner find(UUID id) {
        return repo.findById(id).orElseThrow(() ->
                new AppException(ErrorCode.NOT_FOUND, "Partner not found: " + id));
    }

    private void validatePartnerCreation(PartnerCreateReq req) {
        if (repo.existsByEmail(req.getEmail())) {
            throw new AppException(ErrorCode.ALREADY_EXISTS,
                "Partner with email already exists: " + req.getEmail());
        }

        if (repo.existsByBusinessRegistrationNumber(req.getBusinessRegistrationNumber())) {
            throw new AppException(ErrorCode.ALREADY_EXISTS,
                "Partner with business registration number already exists: " + req.getBusinessRegistrationNumber());
        }
    }

    private void validatePartnerUpdate(Partner existing, PartnerUpdateReq req) {
        if (req.getEmail() != null && !req.getEmail().equals(existing.getEmail())) {
            if (repo.existsByEmail(req.getEmail())) {
                throw new AppException(ErrorCode.ALREADY_EXISTS,
                    "Partner with email already exists: " + req.getEmail());
            }
        }
    }

    private PartnerRes toRes(Partner partner) {
        return PartnerRes.builder()
                .id(partner.getId())
                .ownerUserId(partner.getOwnerUserId())
                .name(partner.getName())
                .legalName(partner.getLegalName())
                .taxNumber(partner.getTaxNumber())
                .businessRegistrationNumber(partner.getBusinessRegistrationNumber())
                .email(partner.getEmail())
                .phoneNumber(partner.getPhoneNumber())
                .address(partner.getAddress())
                .city(partner.getCity())
                .country(partner.getCountry())
                .postalCode(partner.getPostalCode())
                .website(partner.getWebsite())
                .commissionRate(partner.getCommissionRate())
                .status(partner.getStatus())
                .contractStartDate(partner.getContractStartDate())
                .contractEndDate(partner.getContractEndDate())
                .performanceRating(partner.getPerformanceRating())
                .totalBookings(partner.getTotalBookings())
                .createdAt(partner.getCreatedAt())
                .updatedAt(partner.getUpdatedAt())
                .build();
    }
}
