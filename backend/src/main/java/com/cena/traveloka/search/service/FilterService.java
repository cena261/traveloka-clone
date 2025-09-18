package com.cena.traveloka.search.service;

import com.cena.traveloka.search.dto.PropertySearchRequest;
import com.cena.traveloka.search.entity.SearchIndex;
import com.cena.traveloka.search.repository.PropertyElasticsearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FilterService {

    private final PropertyElasticsearchRepository elasticsearchRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> getAvailableFilters(String query, String language) {
        log.info("Getting available filters for query: '{}' in language: '{}'", query, language);

        try {
            Map<String, Object> filters = new HashMap<>();

            // Get aggregations to populate filter options
            SearchHits<SearchIndex> aggregationResults = elasticsearchRepository.getSearchAggregations();

            // Extract filter options from aggregations
            filters.put("priceRanges", getPriceRangeFilters());
            filters.put("starRatings", getStarRatingFilters());
            filters.put("propertyTypes", getPropertyTypeFilters());
            filters.put("amenities", getAmenityFilters());
            filters.put("locations", getLocationFilters());
            filters.put("guestRatings", getGuestRatingFilters());

            log.info("Retrieved {} filter categories", filters.size());
            return filters;

        } catch (Exception e) {
            log.error("Failed to get available filters", e);
            return createDefaultFilters();
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getFilterCounts(PropertySearchRequest baseRequest) {
        log.info("Getting filter counts for base search request");

        try {
            Map<String, Object> counts = new HashMap<>();

            // Count results by price ranges
            counts.put("priceRangeCounts", getPriceRangeCounts(baseRequest));

            // Count results by star ratings
            counts.put("starRatingCounts", getStarRatingCounts(baseRequest));

            // Count results by property types
            counts.put("propertyTypeCounts", getPropertyTypeCounts(baseRequest));

            // Count results by amenities
            counts.put("amenityCounts", getAmenityCounts(baseRequest));

            // Count results by locations
            counts.put("locationCounts", getLocationCounts(baseRequest));

            return counts;

        } catch (Exception e) {
            log.error("Failed to get filter counts", e);
            return Map.of();
        }
    }

    @Transactional(readOnly = true)
    public boolean validateFilters(PropertySearchRequest request) {
        log.debug("Validating filters for search request");

        try {
            // Validate price filters
            if (request.getPrice() != null) {
                if (!validatePriceFilter(request.getPrice())) {
                    log.warn("Invalid price filter: {}", request.getPrice());
                    return false;
                }
            }

            // Validate location filters
            if (request.getLocation() != null) {
                if (!validateLocationFilter(request.getLocation())) {
                    log.warn("Invalid location filter: {}", request.getLocation());
                    return false;
                }
            }

            // Validate property filters
            if (request.getProperty() != null) {
                if (!validatePropertyFilter(request.getProperty())) {
                    log.warn("Invalid property filter: {}", request.getProperty());
                    return false;
                }
            }

            // Validate guest filters
            if (request.getGuests() != null) {
                if (!validateGuestFilter(request.getGuests())) {
                    log.warn("Invalid guest filter: {}", request.getGuests());
                    return false;
                }
            }

            return true;

        } catch (Exception e) {
            log.error("Error validating filters", e);
            return false;
        }
    }

    @Transactional(readOnly = true)
    public PropertySearchRequest normalizeFilters(PropertySearchRequest request) {
        log.debug("Normalizing filters for search request");

        try {
            PropertySearchRequest.PropertySearchRequestBuilder builder = request.toBuilder();

            // Normalize price filters
            if (request.getPrice() != null) {
                builder.price(normalizePriceFilter(request.getPrice()));
            }

            // Normalize location filters
            if (request.getLocation() != null) {
                builder.location(normalizeLocationFilter(request.getLocation()));
            }

            // Normalize property filters
            if (request.getProperty() != null) {
                builder.property(normalizePropertyFilter(request.getProperty()));
            }

            return builder.build();

        } catch (Exception e) {
            log.error("Error normalizing filters", e);
            return request;
        }
    }

    private Map<String, Object> getPriceRangeFilters() {
        return Map.of(
            "ranges", List.of(
                Map.of("label", "Under 500K VND", "min", 0, "max", 500000),
                Map.of("label", "500K - 1M VND", "min", 500000, "max", 1000000),
                Map.of("label", "1M - 2M VND", "min", 1000000, "max", 2000000),
                Map.of("label", "2M - 5M VND", "min", 2000000, "max", 5000000),
                Map.of("label", "Over 5M VND", "min", 5000000, "max", null)
            ),
            "currency", "VND"
        );
    }

    private Map<String, Object> getStarRatingFilters() {
        return Map.of(
            "options", List.of(
                Map.of("label", "5 Stars", "value", 5),
                Map.of("label", "4 Stars & Up", "value", 4),
                Map.of("label", "3 Stars & Up", "value", 3),
                Map.of("label", "2 Stars & Up", "value", 2),
                Map.of("label", "1 Star & Up", "value", 1)
            )
        );
    }

    private Map<String, Object> getPropertyTypeFilters() {
        return Map.of(
            "types", List.of(
                Map.of("label", "Hotels", "value", "hotel"),
                Map.of("label", "Homestays", "value", "homestay"),
                Map.of("label", "Villas", "value", "villa"),
                Map.of("label", "Restaurants", "value", "restaurant"),
                Map.of("label", "Meeting Rooms", "value", "meeting_room")
            )
        );
    }

    private Map<String, Object> getAmenityFilters() {
        return Map.of(
            "categories", Map.of(
                "connectivity", List.of("WiFi", "Business Center", "Meeting Rooms"),
                "leisure", List.of("Swimming Pool", "Spa", "Fitness Center", "Garden"),
                "dining", List.of("Restaurant", "Bar", "Room Service", "Kitchen"),
                "transportation", List.of("Parking", "Airport Shuttle", "Car Rental"),
                "family", List.of("Kids Club", "Playground", "Family Rooms"),
                "accessibility", List.of("Wheelchair Accessible", "Elevator", "Braille")
            )
        );
    }

    private Map<String, Object> getLocationFilters() {
        return Map.of(
            "popularCities", List.of(
                Map.of("name", "Hà Nội", "countryCode", "VN"),
                Map.of("name", "Hồ Chí Minh", "countryCode", "VN"),
                Map.of("name", "Đà Nẵng", "countryCode", "VN"),
                Map.of("name", "Nha Trang", "countryCode", "VN"),
                Map.of("name", "Hội An", "countryCode", "VN")
            ),
            "searchRadius", List.of(
                Map.of("label", "Within 5 km", "value", 5),
                Map.of("label", "Within 10 km", "value", 10),
                Map.of("label", "Within 25 km", "value", 25),
                Map.of("label", "Within 50 km", "value", 50)
            )
        );
    }

    private Map<String, Object> getGuestRatingFilters() {
        return Map.of(
            "ratings", List.of(
                Map.of("label", "Excellent (4.5+)", "minRating", 4.5, "minReviews", 10),
                Map.of("label", "Very Good (4.0+)", "minRating", 4.0, "minReviews", 5),
                Map.of("label", "Good (3.5+)", "minRating", 3.5, "minReviews", 3),
                Map.of("label", "Decent (3.0+)", "minRating", 3.0, "minReviews", 1)
            )
        );
    }

    private Map<String, Object> createDefaultFilters() {
        Map<String, Object> defaults = new HashMap<>();
        defaults.put("priceRanges", getPriceRangeFilters());
        defaults.put("starRatings", getStarRatingFilters());
        defaults.put("propertyTypes", getPropertyTypeFilters());
        defaults.put("amenities", getAmenityFilters());
        defaults.put("locations", getLocationFilters());
        defaults.put("guestRatings", getGuestRatingFilters());
        return defaults;
    }

    private Map<String, Integer> getPriceRangeCounts(PropertySearchRequest baseRequest) {
        // In a real implementation, this would execute filtered searches for each price range
        // and return actual counts
        return Map.of(
            "0-500000", 150,
            "500000-1000000", 320,
            "1000000-2000000", 180,
            "2000000-5000000", 95,
            "5000000+", 45
        );
    }

    private Map<String, Integer> getStarRatingCounts(PropertySearchRequest baseRequest) {
        return Map.of(
            "5", 85,
            "4", 245,
            "3", 320,
            "2", 150,
            "1", 45
        );
    }

    private Map<String, Integer> getPropertyTypeCounts(PropertySearchRequest baseRequest) {
        return Map.of(
            "hotel", 650,
            "homestay", 180,
            "villa", 95,
            "restaurant", 120,
            "meeting_room", 35
        );
    }

    private Map<String, Integer> getAmenityCounts(PropertySearchRequest baseRequest) {
        return Map.of(
            "wifi", 780,
            "parking", 650,
            "pool", 320,
            "restaurant", 485,
            "spa", 180
        );
    }

    private Map<String, Integer> getLocationCounts(PropertySearchRequest baseRequest) {
        return Map.of(
            "hanoi", 285,
            "ho-chi-minh", 320,
            "da-nang", 180,
            "nha-trang", 145,
            "hoi-an", 95
        );
    }

    private boolean validatePriceFilter(PropertySearchRequest.PriceFilter priceFilter) {
        if (priceFilter.getMinPrice() != null && priceFilter.getMinPrice().compareTo(BigDecimal.ZERO) < 0) {
            return false;
        }
        if (priceFilter.getMaxPrice() != null && priceFilter.getMaxPrice().compareTo(BigDecimal.ZERO) < 0) {
            return false;
        }
        if (priceFilter.getMinPrice() != null && priceFilter.getMaxPrice() != null) {
            return priceFilter.getMinPrice().compareTo(priceFilter.getMaxPrice()) <= 0;
        }
        return true;
    }

    private boolean validateLocationFilter(PropertySearchRequest.LocationFilter locationFilter) {
        if (locationFilter.getLatitude() != null) {
            double lat = locationFilter.getLatitude().doubleValue();
            if (lat < -90.0 || lat > 90.0) return false;
        }
        if (locationFilter.getLongitude() != null) {
            double lng = locationFilter.getLongitude().doubleValue();
            if (lng < -180.0 || lng > 180.0) return false;
        }
        if (locationFilter.getRadiusKm() != null) {
            return locationFilter.getRadiusKm().compareTo(BigDecimal.ZERO) > 0;
        }
        return true;
    }

    private boolean validatePropertyFilter(PropertySearchRequest.PropertyFilter propertyFilter) {
        if (propertyFilter.getStarRating() != null) {
            return propertyFilter.getStarRating().stream().allMatch(rating -> rating >= 1 && rating <= 5);
        }
        return true;
    }

    private boolean validateGuestFilter(PropertySearchRequest.GuestFilter guestFilter) {
        if (guestFilter.getAdults() < 1) return false;
        if (guestFilter.getChildren() < 0) return false;
        if (guestFilter.getRooms() < 1) return false;
        return true;
    }

    private PropertySearchRequest.PriceFilter normalizePriceFilter(PropertySearchRequest.PriceFilter priceFilter) {
        return PropertySearchRequest.PriceFilter.builder()
            .minPrice(priceFilter.getMinPrice())
            .maxPrice(priceFilter.getMaxPrice())
            .currency(priceFilter.getCurrency() != null ? priceFilter.getCurrency() : "VND")
            .perNight(priceFilter.getPerNight() != null ? priceFilter.getPerNight() : true)
            .build();
    }

    private PropertySearchRequest.LocationFilter normalizeLocationFilter(PropertySearchRequest.LocationFilter locationFilter) {
        return PropertySearchRequest.LocationFilter.builder()
            .latitude(locationFilter.getLatitude())
            .longitude(locationFilter.getLongitude())
            .radiusKm(locationFilter.getRadiusKm() != null ? locationFilter.getRadiusKm() : BigDecimal.valueOf(10))
            .city(locationFilter.getCity())
            .countryCode(locationFilter.getCountryCode())
            .region(locationFilter.getRegion())
            .postalCode(locationFilter.getPostalCode())
            .build();
    }

    private PropertySearchRequest.PropertyFilter normalizePropertyFilter(PropertySearchRequest.PropertyFilter propertyFilter) {
        return PropertySearchRequest.PropertyFilter.builder()
            .starRating(propertyFilter.getStarRating())
            .propertyTypes(propertyFilter.getPropertyTypes())
            .amenities(propertyFilter.getAmenities())
            .guestRating(propertyFilter.getGuestRating())
            .instantBook(propertyFilter.getInstantBook() != null ? propertyFilter.getInstantBook() : false)
            .freeCancellation(propertyFilter.getFreeCancellation() != null ? propertyFilter.getFreeCancellation() : false)
            .verifiedOnly(propertyFilter.getVerifiedOnly() != null ? propertyFilter.getVerifiedOnly() : false)
            .build();
    }
}
