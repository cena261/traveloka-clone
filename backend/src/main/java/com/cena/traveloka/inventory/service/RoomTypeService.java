package com.cena.traveloka.inventory.service;

import com.cena.traveloka.exception.AppException;
import com.cena.traveloka.exception.ErrorCode;
import com.cena.traveloka.inventory.dto.request.RoomTypeCreateReq;
import com.cena.traveloka.inventory.dto.request.RoomTypeUpdateReq;
import com.cena.traveloka.inventory.dto.response.RoomTypeRes;
import com.cena.traveloka.inventory.entity.Property;
import com.cena.traveloka.inventory.entity.RoomType;
import com.cena.traveloka.inventory.repository.PropertyRepository;
import com.cena.traveloka.inventory.repository.RoomTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoomTypeService {
    private final RoomTypeRepository repo;
    private final PropertyRepository propertyRepo;

    @Transactional
    public RoomTypeRes create(RoomTypeCreateReq req) {
        Property property = propertyRepo.findById(req.getPropertyId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Property not found: " + req.getPropertyId()));

        RoomType e = new RoomType();
        e.setProperty(property);
        e.setName(req.getName());
        e.setDescription(req.getDescription());
        e.setCapacityAdult(req.getCapacityAdult());
        e.setCapacityChild(req.getCapacityChild() != null ? req.getCapacityChild() : 0);
        e.setBasePriceCents(req.getBasePriceCents());
        e.setCurrency(req.getCurrency() != null ? req.getCurrency() : "VND");
        e.setRefundable(req.getRefundable() != null ? req.getRefundable() : Boolean.TRUE);
        e.setTotalUnits(req.getTotalUnits() != null ? req.getTotalUnits() : 0);

        repo.save(e);
        return toRes(e);
    }

    public List<RoomTypeRes> listByProperty(UUID propertyId) {
        Property property = propertyRepo.findById(propertyId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Property not found: " + propertyId));
        return repo.findByProperty(property).stream().map(this::toRes).collect(Collectors.toList());
    }

    @Transactional
    public RoomTypeRes update(UUID id, RoomTypeUpdateReq req) {
        RoomType e = repo.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "RoomType not found: " + id));

        if (req.getName()!=null) e.setName(req.getName());
        if (req.getDescription()!=null) e.setDescription(req.getDescription());
        if (req.getCapacityAdult()!=null) e.setCapacityAdult(req.getCapacityAdult());
        if (req.getCapacityChild()!=null) e.setCapacityChild(req.getCapacityChild());
        if (req.getBasePriceCents()!=null) e.setBasePriceCents(req.getBasePriceCents());
        if (req.getCurrency()!=null) e.setCurrency(req.getCurrency());
        if (req.getRefundable()!=null) e.setRefundable(req.getRefundable());
        if (req.getTotalUnits()!=null) e.setTotalUnits(req.getTotalUnits());

        return toRes(e);
    }

    @Transactional
    public void delete(UUID id) {
        RoomType e = repo.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "RoomType not found: " + id));
        repo.delete(e);
    }

    private RoomTypeRes toRes(RoomType e) {
        return RoomTypeRes.builder()
                .id(e.getId())
                .propertyId(e.getProperty().getId())
                .name(e.getName())
                .description(e.getDescription())
                .capacityAdult(e.getCapacityAdult())
                .capacityChild(e.getCapacityChild())
                .basePriceCents(e.getBasePriceCents())
                .currency(e.getCurrency())
                .refundable(e.getRefundable())
                .totalUnits(e.getTotalUnits())
                .build();
    }
}
