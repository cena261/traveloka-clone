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
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PartnerService {
    PartnerRepository repo;

    @Transactional
    public PartnerRes create(PartnerCreateReq req) {
        Partner e = new Partner();
        e.setOwnerUserId(req.getOwnerUserId());
        e.setName(req.getName());
        e.setLegalName(req.getLegalName());
        e.setTaxNumber(req.getTaxNumber());
        // e.setStatus("active");
        repo.save(e);
        return toRes(e);
    }

    public Page<PartnerRes> list(int page, int size) {
        Page<Partner> data = repo.findAll(PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return data.map(this::toRes);
    }

    public PartnerRes get(UUID id) {
        return toRes(find(id));
    }

    @Transactional
    public PartnerRes update(UUID id, PartnerUpdateReq req) {
        Partner e = find(id);
        if (req.getName()!=null) e.setName(req.getName());
        if (req.getLegalName()!=null) e.setLegalName(req.getLegalName());
        if (req.getTaxNumber()!=null) e.setTaxNumber(req.getTaxNumber());
        if (req.getStatus()!=null) e.setStatus(req.getStatus());
        return toRes(e);
    }

    @Transactional
    public void delete(UUID id) {
        repo.delete(find(id));
    }

    private Partner find(UUID id){
        return repo.findById(id).orElseThrow(() ->
                new AppException(ErrorCode.NOT_FOUND, "Partner not found: " + id));
    }

    private PartnerRes toRes(Partner e) {
        return PartnerRes.builder()
                .id(e.getId())
                .ownerUserId(e.getOwnerUserId())
                .name(e.getName())
                .legalName(e.getLegalName())
                .taxNumber(e.getTaxNumber())
                .status(e.getStatus())
                .build();
    }
}
