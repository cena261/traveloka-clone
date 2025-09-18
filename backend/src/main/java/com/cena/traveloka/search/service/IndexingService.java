package com.cena.traveloka.search.service;

import com.cena.traveloka.catalog.inventory.entity.Property;
import com.cena.traveloka.search.entity.SearchIndex;
import com.cena.traveloka.search.repository.PropertyElasticsearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndexingService {

    private final PropertyElasticsearchRepository elasticsearchRepository;

    @Transactional
    public void indexProperty(Property property) {
        log.info("Indexing property: {} (ID: {})", property.getName(), property.getId());

        try {
            SearchIndex searchIndex = convertPropertyToSearchIndex(property);
            elasticsearchRepository.save(searchIndex);

            log.info("Successfully indexed property: {} (ID: {})", property.getName(), property.getId());

        } catch (Exception e) {
            log.error("Failed to index property: {} (ID: {})", property.getName(), property.getId(), e);
            throw new RuntimeException("Failed to index property: " + property.getId(), e);
        }
    }

    @Transactional
    public void bulkIndexProperties(List<Property> properties) {
        log.info("Bulk indexing {} properties", properties.size());

        try {
            List<SearchIndex> searchIndices = properties.stream()
                .map(this::convertPropertyToSearchIndex)
                .collect(Collectors.toList());

            elasticsearchRepository.saveAll(searchIndices);

            log.info("Successfully bulk indexed {} properties", properties.size());

        } catch (Exception e) {
            log.error("Failed to bulk index {} properties", properties.size(), e);
            throw new RuntimeException("Failed to bulk index properties", e);
        }
    }

    @Transactional
    public void removeFromIndex(UUID propertyId) {
        log.info("Removing property from index: {}", propertyId);

        try {
            elasticsearchRepository.deleteById(propertyId);
            log.info("Successfully removed property from index: {}", propertyId);

        } catch (Exception e) {
            log.error("Failed to remove property from index: {}", propertyId, e);
            throw new RuntimeException("Failed to remove property from index: " + propertyId, e);
        }
    }

    @Async
    @Transactional
    public void reindexProperty(Property property) {
        log.info("Reindexing property: {} (ID: {})", property.getName(), property.getId());

        try {
            // Remove existing index entry
            elasticsearchRepository.deleteById(property.getId());

            // Add updated index entry
            SearchIndex searchIndex = convertPropertyToSearchIndex(property);
            elasticsearchRepository.save(searchIndex);

            log.info("Successfully reindexed property: {} (ID: {})", property.getName(), property.getId());

        } catch (Exception e) {
            log.error("Failed to reindex property: {} (ID: {})", property.getName(), property.getId(), e);
            throw new RuntimeException("Failed to reindex property: " + property.getId(), e);
        }
    }

    @Async
    @Transactional
    public void refreshAllIndices() {
        log.info("Starting full reindexing of all properties");

        try {
            // This would typically query all active properties from the database
            // and reindex them in batches
            log.warn("Full reindexing not implemented - would require PropertyRepository");

        } catch (Exception e) {
            log.error("Failed to refresh all indices", e);
            throw new RuntimeException("Failed to refresh all indices", e);
        }
    }

    private SearchIndex convertPropertyToSearchIndex(Property property) {
        try {
            return SearchIndex.builder()
                .propertyId(property.getId())
                .name(property.getName())
                .description(property.getDescription())
                .kind(property.getKind().name())
                .starRating(property.getStarRating())
                .city(property.getCity())
                .countryCode(property.getCountryCode())
                .addressLine(property.getAddressLine())
                .postalCode(property.getPostalCode())
                .location(convertLocationData(property))
                .phoneNumber(property.getPhoneNumber())
                .email(property.getEmail())
                .website(property.getWebsite())
                .checkInTime(property.getCheckInTime())
                .checkOutTime(property.getCheckOutTime())
                .totalRooms(property.getTotalRooms())
                .ratingAvg(property.getRatingAvg())
                .ratingCount(property.getRatingCount())
                .status(property.getStatus().name())
                .timezone(property.getTimezone())
                .amenities(convertAmenities(property))
                .images(convertImages(property))
                .roomTypes(convertRoomTypes(property))
                .createdAt(property.getCreatedAt())
                .updatedAt(property.getUpdatedAt())
                .searchBoost(generateSearchBoost(property))
                .build();

        } catch (Exception e) {
            log.error("Failed to convert property to search index: {} (ID: {})", property.getName(), property.getId(), e);
            throw new RuntimeException("Failed to convert property to search index", e);
        }
    }

    private SearchIndex.LocationData convertLocationData(Property property) {
        SearchIndex.GeoPointData coordinates = null;

        if (property.getLatitude() != null && property.getLongitude() != null) {
            coordinates = SearchIndex.GeoPointData.builder()
                .lat(property.getLatitude())
                .lon(property.getLongitude())
                .build();
        }

        return SearchIndex.LocationData.builder()
            .coordinates(coordinates)
            .city(property.getCity())
            .countryCode(property.getCountryCode())
            .address(property.getAddressLine())
            .postalCode(property.getPostalCode())
            .build();
    }

    private List<SearchIndex.AmenityData> convertAmenities(Property property) {
        if (property.getAmenities() == null || property.getAmenities().isEmpty()) {
            return List.of();
        }

        return property.getAmenities().stream()
            .map(amenity -> SearchIndex.AmenityData.builder()
                .id(amenity.getId())
                .name(amenity.getName())
                .category(amenity.getCategory().name())
                .description(amenity.getDescription())
                .isFeatured(false) // Would come from amenity properties
                .build())
            .collect(Collectors.toList());
    }

    private List<SearchIndex.ImageData> convertImages(Property property) {
        if (property.getImages() == null || property.getImages().isEmpty()) {
            return List.of();
        }

        return property.getImages().stream()
            .map(image -> SearchIndex.ImageData.builder()
                .id(image.getId())
                .url(image.getImageUrl())
                .alt(image.getAltText())
                .type(image.getImageType().name())
                .sortOrder(image.getSortOrder())
                .isPrimary(image.getIsPrimary())
                .build())
            .collect(Collectors.toList());
    }

    private List<SearchIndex.RoomTypeData> convertRoomTypes(Property property) {
        if (property.getRoomTypes() == null || property.getRoomTypes().isEmpty()) {
            return List.of();
        }

        return property.getRoomTypes().stream()
            .map(roomType -> SearchIndex.RoomTypeData.builder()
                .id(roomType.getId())
                .name(roomType.getName())
                .description(roomType.getDescription())
                .maxOccupancy(roomType.getMaxOccupancy())
                .availableRooms(roomType.getQuantity()) // Assuming quantity represents available rooms
                .basePrice(roomType.getBasePrice())
                .currency("VND") // Default currency
                .build())
            .collect(Collectors.toList());
    }

    private SearchIndex.SearchBoostData generateSearchBoost(Property property) {
        // Calculate popularity score based on rating and reviews
        float popularityScore = calculatePopularityScore(property);

        // Calculate conversion rate (would come from booking data)
        float conversionRate = 0.05f; // Default 5%

        // Calculate review score
        float reviewScore = property.getRatingAvg() != null ?
            property.getRatingAvg().floatValue() / 5.0f * 100 : 0;

        // Calculate search frequency (would come from search analytics)
        int searchFrequency = 0;

        // Determine if promoted (would come from business rules)
        boolean isPromoted = property.getStarRating() != null && property.getStarRating() >= 4;

        return SearchIndex.SearchBoostData.builder()
            .popularityScore(popularityScore)
            .conversionRate(conversionRate)
            .reviewScore(reviewScore)
            .searchFrequency(searchFrequency)
            .isPromoted(isPromoted)
            .lastUpdated(OffsetDateTime.now())
            .build();
    }

    private float calculatePopularityScore(Property property) {
        float score = 50.0f; // Base score

        // Add score based on rating
        if (property.getRatingAvg() != null) {
            score += property.getRatingAvg().floatValue() * 10; // Max 50 points
        }

        // Add score based on review count
        if (property.getRatingCount() != null && property.getRatingCount() > 0) {
            score += Math.min(20, property.getRatingCount() / 5); // Max 20 points
        }

        // Add score based on star rating
        if (property.getStarRating() != null) {
            score += property.getStarRating() * 4; // Max 20 points
        }

        // Add score based on amenity count
        if (property.getAmenities() != null) {
            score += Math.min(10, property.getAmenities().size()); // Max 10 points
        }

        return Math.min(100.0f, score);
    }

    public boolean isIndexed(UUID propertyId) {
        try {
            return elasticsearchRepository.existsById(propertyId);
        } catch (Exception e) {
            log.error("Failed to check if property is indexed: {}", propertyId, e);
            return false;
        }
    }

    public long getIndexedPropertyCount() {
        try {
            return elasticsearchRepository.count();
        } catch (Exception e) {
            log.error("Failed to get indexed property count", e);
            return 0;
        }
    }
}
