package com.cena.traveloka.catalog.inventory.service;

import com.cena.traveloka.common.exception.AppException;
import com.cena.traveloka.common.exception.ErrorCode;
import com.cena.traveloka.catalog.inventory.dto.request.PropertyAmenityBindReq;
import com.cena.traveloka.catalog.inventory.dto.request.PropertyCreateReq;
import com.cena.traveloka.catalog.inventory.dto.request.PropertyUpdateReq;
import com.cena.traveloka.catalog.inventory.dto.response.AmenityRes;
import com.cena.traveloka.catalog.inventory.dto.response.PropertyRes;
import com.cena.traveloka.catalog.inventory.entity.Amenity;
import com.cena.traveloka.catalog.inventory.entity.Partner;
import com.cena.traveloka.catalog.inventory.entity.Property;
import com.cena.traveloka.catalog.inventory.entity.PropertyAmenity;
import com.cena.traveloka.catalog.inventory.repository.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Validated
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PropertyService {
    PropertyRepository repo;
    PartnerRepository partnerRepo;
    AmenityRepository amenityRepo;
    PropertyAmenityRepository propertyAmenityRepo;
    GeometryFactory geometryFactory = new GeometryFactory();

    @Transactional
    @CacheEvict(value = "properties", allEntries = true)
    public PropertyRes create(@NotNull PropertyCreateReq req) {
        log.info("Creating new property: {}", req.getName());

        Partner partner = partnerRepo.findById(req.getPartnerId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Partner not found: " + req.getPartnerId()));

        validatePartnerStatus(partner);
        validatePropertyCreation(req);

        Point geography = createGeographyPoint(req.getLat(), req.getLng());

        Property property = Property.builder()
                .partner(partner)
                .propertyCode(req.getPropertyCode())
                .kind(req.getKind())
                .name(req.getName())
                .description(req.getDescription())
                .countryCode(req.getCountryCode())
                .city(req.getCity())
                .addressLine(req.getAddressLine())
                .postalCode(req.getPostalCode())
                .latitude(req.getLat())
                .longitude(req.getLng())
                .geography(geography)
                .timezone(req.getTimezone() != null ? req.getTimezone() : "Asia/Ho_Chi_Minh")
                .status(Property.PropertyStatus.DRAFT)
                .starRating(req.getStarRating())
                .build();

        property = repo.save(property);
        log.info("Property created successfully with ID: {}", property.getId());

        return toRes(property);
    }

    public Page<PropertyRes> listByPartner(UUID partnerId, int page, int size) {
        Partner partner = partnerRepo.findById(partnerId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Partner not found: " + partnerId));
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return repo.findByPartner(partner, pageable).map(this::toRes);
    }

    public Page<PropertyRes> findByStatus(Property.PropertyStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return repo.findByStatus(status, pageable).map(this::toRes);
    }

    public Page<PropertyRes> searchProperties(Property.PropertyStatus status, String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return repo.findByStatusAndSearch(status, search, pageable).map(this::toRes);
    }

    public List<PropertyRes> findPropertiesNearLocation(double latitude, double longitude, double radiusKm, int limit) {
        double radiusMeters = radiusKm * 1000;
        List<Property> properties = repo.findNearestProperties(latitude, longitude, radiusMeters);
        return properties.stream()
                .limit(limit)
                .map(this::toRes)
                .toList();
    }

    public List<PropertyRes> findPropertiesWithinRadius(double latitude, double longitude, double radiusKm) {
        double radiusMeters = radiusKm * 1000;
        List<Property> properties = repo.findPropertiesWithinRadius(latitude, longitude, radiusMeters);
        return properties.stream().map(this::toRes).toList();
    }

    @Cacheable(value = "properties", key = "#id")
    public PropertyRes get(@NotNull UUID id) {
        return toRes(find(id));
    }

    public Optional<PropertyRes> findByPropertyCode(String propertyCode) {
        return repo.findByPropertyCode(propertyCode).map(this::toRes);
    }

    @Transactional
    @CacheEvict(value = "properties", allEntries = true)
    public PropertyRes update(@NotNull UUID id, @NotNull PropertyUpdateReq req) {
        log.info("Updating property: {}", id);

        Property property = find(id);
        validatePropertyUpdate(property, req);

        if (req.getKind() != null) property.setKind(req.getKind());
        if (req.getName() != null) property.setName(req.getName());
        if (req.getDescription() != null) property.setDescription(req.getDescription());
        if (req.getCountryCode() != null) property.setCountryCode(req.getCountryCode());
        if (req.getCity() != null) property.setCity(req.getCity());
        if (req.getAddressLine() != null) property.setAddressLine(req.getAddressLine());
        if (req.getPostalCode() != null) property.setPostalCode(req.getPostalCode());
        if (req.getStarRating() != null) property.setStarRating(req.getStarRating());
        if (req.getTimezone() != null) property.setTimezone(req.getTimezone());
        if (req.getStatus() != null) property.setStatus(req.getStatus());

        if (req.getLat() != null && req.getLng() != null) {
            property.setLatitude(req.getLat());
            property.setLongitude(req.getLng());
            property.setGeography(createGeographyPoint(req.getLat(), req.getLng()));
        }

        property = repo.save(property);
        log.info("Property updated successfully: {}", id);

        return toRes(property);
    }

    @Transactional
    @CacheEvict(value = "properties", allEntries = true)
    public PropertyRes activateProperty(@NotNull UUID id) {
        Property property = find(id);
        if (property.getStatus() != Property.PropertyStatus.PENDING_VERIFICATION) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                "Property must be in PENDING_VERIFICATION status to activate");
        }

        property.setStatus(Property.PropertyStatus.ACTIVE);
        property = repo.save(property);

        log.info("Property activated: {}", id);
        return toRes(property);
    }

    @Transactional
    @CacheEvict(value = "properties", allEntries = true)
    public PropertyRes suspendProperty(@NotNull UUID id, String reason) {
        Property property = find(id);
        if (property.getStatus() != Property.PropertyStatus.ACTIVE) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                "Only active properties can be suspended");
        }

        property.setStatus(Property.PropertyStatus.SUSPENDED);
        property = repo.save(property);

        log.info("Property suspended: {} - Reason: {}", id, reason);
        return toRes(property);
    }

    @Transactional
    @CacheEvict(value = "properties", allEntries = true)
    public void delete(@NotNull UUID id) {
        Property property = find(id);
        if (property.getStatus() == Property.PropertyStatus.ACTIVE) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                "Cannot delete active property. Suspend first.");
        }

        repo.delete(property);
        log.info("Property deleted: {}", id);
    }

    public List<PropertyRes> getTopPerformingProperties(Integer minBookings, Double minRating, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Property> properties = repo.findTopPerformingProperties(minBookings, minRating, pageable);
        return properties.getContent().stream().map(this::toRes).toList();
    }

    @Transactional
    @CacheEvict(value = {"properties", "propertyAmenities"}, allEntries = true)
    public PropertyRes bindAmenities(@NotNull UUID propertyId, @NotNull PropertyAmenityBindReq req) {
        log.info("Binding amenities to property: {}", propertyId);

        Property property = find(propertyId);
        List<Amenity> amenities = amenityRepo.findAllById(req.getAmenityIds());

        if (amenities.size() != req.getAmenityIds().size()) {
            throw new AppException(ErrorCode.NOT_FOUND, "Some amenities not found");
        }

        propertyAmenityRepo.findByProperty(property).forEach(propertyAmenityRepo::delete);

        for (int i = 0; i < amenities.size(); i++) {
            Amenity amenity = amenities.get(i);
            PropertyAmenityBindReq.AmenityConfig config = req.getAmenityConfigs().get(i);

            PropertyAmenity propertyAmenity = PropertyAmenity.builder()
                    .property(property)
                    .amenity(amenity)
                    .isFree(config.getIsFree())
                    .additionalCost(config.getAdditionalCost())
                    .availableFrom(config.getAvailableFrom())
                    .availableTo(config.getAvailableTo())
                    .seasonalAvailability(config.getSeasonalAvailability())
                    .notes(config.getNotes())
                    .build();

            propertyAmenityRepo.save(propertyAmenity);
        }

        log.info("Amenities bound successfully to property: {}", propertyId);
        return toRes(property);
    }

    @Cacheable(value = "propertyAmenities", key = "#propertyId")
    public List<AmenityRes> listAmenities(@NotNull UUID propertyId) {
        Property property = find(propertyId);
        List<PropertyAmenity> propertyAmenities = propertyAmenityRepo.findByProperty(property);
        return propertyAmenities.stream()
                .map(pa -> toAmenityRes(pa.getAmenity()))
                .collect(Collectors.toList());
    }

    public List<AmenityRes> listAmenitiesByCategory(@NotNull UUID propertyId, Amenity.AmenityCategory category) {
        Property property = find(propertyId);
        List<PropertyAmenity> propertyAmenities = propertyAmenityRepo.findByPropertyAndAmenityCategory(property, category);
        return propertyAmenities.stream()
                .map(pa -> toAmenityRes(pa.getAmenity()))
                .collect(Collectors.toList());
    }

    public List<AmenityRes> listFreeAmenities(@NotNull UUID propertyId) {
        Property property = find(propertyId);
        List<PropertyAmenity> propertyAmenities = propertyAmenityRepo.findByPropertyAndIsFree(property, true);
        return propertyAmenities.stream()
                .map(pa -> toAmenityRes(pa.getAmenity()))
                .collect(Collectors.toList());
    }

    private Property find(UUID id) {
        return repo.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Property not found: " + id));
    }

    private Point createGeographyPoint(Double latitude, Double longitude) {
        return geometryFactory.createPoint(new Coordinate(longitude, latitude));
    }

    private void validatePartnerStatus(Partner partner) {
        if (partner.getStatus() != Partner.PartnerStatus.ACTIVE) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                "Partner must be active to create properties");
        }
    }

    private void validatePropertyCreation(PropertyCreateReq req) {
        if (repo.existsByPropertyCode(req.getPropertyCode())) {
            throw new AppException(ErrorCode.ALREADY_EXISTS,
                "Property with code already exists: " + req.getPropertyCode());
        }
    }

    private void validatePropertyUpdate(Property existing, PropertyUpdateReq req) {
        if (existing.getStatus() == Property.PropertyStatus.ACTIVE &&
            req.getStatus() != null && req.getStatus() != Property.PropertyStatus.ACTIVE) {
            log.warn("Attempting to change status of active property: {}", existing.getId());
        }
    }

    private PropertyRes toRes(Property property) {
        return PropertyRes.builder()
                .id(property.getId())
                .partnerId(property.getPartner().getId())
                .propertyCode(property.getPropertyCode())
                .kind(property.getKind())
                .name(property.getName())
                .description(property.getDescription())
                .countryCode(property.getCountryCode())
                .city(property.getCity())
                .addressLine(property.getAddressLine())
                .postalCode(property.getPostalCode())
                .lat(property.getLatitude())
                .lng(property.getLongitude())
                .starRating(property.getStarRating())
                .averageRating(property.getAverageRating())
                .totalReviews(property.getTotalReviews())
                .totalBookings(property.getTotalBookings())
                .status(property.getStatus())
                .timezone(property.getTimezone())
                .createdAt(property.getCreatedAt())
                .updatedAt(property.getUpdatedAt())
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
