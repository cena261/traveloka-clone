package com.cena.traveloka.inventory.service;

import com.cena.traveloka.exception.AppException;
import com.cena.traveloka.exception.ErrorCode;
import com.cena.traveloka.inventory.dto.request.PropertyAmenityBindReq;
import com.cena.traveloka.inventory.dto.request.PropertyCreateReq;
import com.cena.traveloka.inventory.dto.request.PropertyUpdateReq;
import com.cena.traveloka.inventory.dto.response.AmenityRes;
import com.cena.traveloka.inventory.dto.response.PropertyRes;
import com.cena.traveloka.inventory.entity.Amenity;
import com.cena.traveloka.inventory.entity.Partner;
import com.cena.traveloka.inventory.entity.Property;
import com.cena.traveloka.inventory.repository.AmenityRepository;
import com.cena.traveloka.inventory.repository.PartnerRepository;
import com.cena.traveloka.inventory.repository.PropertyRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PropertyService {
    PropertyRepository repo;
    PartnerRepository partnerRepo;
    AmenityRepository amenityRepo;

    @Transactional
    public PropertyRes create(PropertyCreateReq req) {
        Partner partner = partnerRepo.findById(req.getPartnerId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Partner not found: " + req.getPartnerId()));

        Property e = new Property();
        e.setPartner(partner);
        e.setKind(req.getKind());
        e.setName(req.getName());
        e.setDescription(req.getDescription());
        e.setCountryCode(req.getCountryCode());
        e.setCity(req.getCity());
        e.setAddressLine(req.getAddressLine());
        e.setPostalCode(req.getPostalCode());
        e.setLatitude(req.getLat());
        e.setLongitude(req.getLng());
        e.setTimezone(req.getTimezone() != null ? req.getTimezone() : "Asia/Ho_Chi_Minh");
        e.setStatus("draft");

        repo.save(e);
        return toRes(e);
    }

    public Page<PropertyRes> listByPartner(UUID partnerId, int page, int size) {
        Partner partner = partnerRepo.findById(partnerId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Partner not found: " + partnerId));
        return repo.findByPartner(partner, PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .map(this::toRes);
    }

    public PropertyRes get(UUID id) {
        return toRes(find(id));
    }

    @Transactional
    public PropertyRes update(UUID id, PropertyUpdateReq req) {
        Property e = find(id);
        if (req.getKind() != null) e.setKind(req.getKind());
        if (req.getName() != null) e.setName(req.getName());
        if (req.getDescription() != null) e.setDescription(req.getDescription());
        if (req.getCountryCode() != null) e.setCountryCode(req.getCountryCode());
        if (req.getCity() != null) e.setCity(req.getCity());
        if (req.getAddressLine() != null) e.setAddressLine(req.getAddressLine());
        if (req.getPostalCode() != null) e.setPostalCode(req.getPostalCode());
        if (req.getLat() != null) e.setLatitude(req.getLat());
        if (req.getLng() != null) e.setLongitude(req.getLng());
        if (req.getTimezone() != null) e.setTimezone(req.getTimezone());
        if (req.getStatus() != null) e.setStatus(req.getStatus()); // draft|active|inactive
        return toRes(e);
    }

    @Transactional
    public void delete(UUID id) {
        repo.delete(find(id));
    }

    @Transactional
    public PropertyRes bindAmenities(UUID propertyId, PropertyAmenityBindReq req) {
        Property property = find(propertyId);

        List<Amenity> amenities = amenityRepo.findAllById(req.getAmenityIds());
        if (amenities.size() != req.getAmenityIds().size()) {
            throw new AppException(ErrorCode.NOT_FOUND, "Some amenities not found");
        }

        property.setAmenities(new HashSet<>(amenities)); // ghi vào bảng inventory.amenity_map
        return toRes(property);
    }

    public List<AmenityRes> listAmenities(UUID propertyId) {
        Property property = find(propertyId);
        return property.getAmenities().stream()
                .map(this::toAmenityRes)
                .collect(Collectors.toList());
    }

    private Property find(UUID id) {
        return repo.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Property not found: " + id));
    }

    private PropertyRes toRes(Property e) {
        BigDecimal ratingAvg = e.getRatingAvg(); // có thể null
        return PropertyRes.builder()
                .id(e.getId())
                .partnerId(e.getPartner().getId())
                .kind(e.getKind())
                .name(e.getName())
                .description(e.getDescription())
                .countryCode(e.getCountryCode())
                .city(e.getCity())
                .addressLine(e.getAddressLine())
                .postalCode(e.getPostalCode())
                .lat(e.getLatitude())
                .lng(e.getLongitude())
                .ratingAvg(ratingAvg)
                .ratingCount(e.getRatingCount())
                .status(e.getStatus())
                .timezone(e.getTimezone())
                .createdAt(e.getCreatedAt() instanceof OffsetDateTime odt ? odt : e.getCreatedAt())
                .updatedAt(e.getUpdatedAt() instanceof OffsetDateTime odt2 ? odt2 : e.getUpdatedAt())
                .build();
    }

    private AmenityRes toAmenityRes(Amenity a) {
        return AmenityRes.builder()
                .id(a.getId())
                .code(a.getCode())
                .name(a.getName())
                .build();
    }
}
