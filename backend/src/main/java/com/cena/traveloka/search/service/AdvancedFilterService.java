package com.cena.traveloka.search.service;

import com.cena.traveloka.search.dto.FacetedSearchRequest;
import com.cena.traveloka.search.dto.FacetedSearchResponse;
import com.cena.traveloka.search.repository.PropertyElasticsearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdvancedFilterService {

    private final PropertyElasticsearchRepository elasticsearchRepository;

    @Transactional(readOnly = true)
    @Cacheable(value = "facetedSearch", key = "#request.hashCode()")
    public FacetedSearchResponse executeFacetedSearch(FacetedSearchRequest request) {
        log.info("Executing faceted search for query: '{}' with {} filters",
                request.getQuery(),
                countActiveFilters(request));

        long startTime = System.currentTimeMillis();

        try {
            // Build Elasticsearch aggregation query
            var searchQuery = buildAdvancedSearchQuery(request);

            // Execute search with aggregations
            var searchResults = elasticsearchRepository.searchWithAggregations(searchQuery);

            // Process results
            var results = processSearchResults(searchResults, request);

            // Build facets from aggregations
            var facets = buildSearchFacets(searchResults, request);

            // Build applied filters
            var appliedFilters = buildAppliedFilters(request);

            // Build metadata
            var metadata = buildSearchMetadata(startTime, request, results.size());

            // Build pagination
            var pagination = buildPagination(searchResults, request);

            return FacetedSearchResponse.builder()
                .results(results)
                .facets(facets)
                .appliedFilters(appliedFilters)
                .metadata(metadata)
                .pagination(pagination)
                .build();

        } catch (Exception e) {
            log.error("Faceted search failed for query: '{}'", request.getQuery(), e);
            return createEmptyResponse(request);
        }
    }

    @Transactional(readOnly = true)
    public boolean validateAdvancedFilters(FacetedSearchRequest request) {
        log.debug("Validating advanced filters for search request");

        try {
            // Validate location filters
            if (request.getLocation() != null) {
                if (!validateLocationFilter(request.getLocation())) {
                    return false;
                }
            }

            // Validate price filters
            if (request.getPrice() != null) {
                if (!validatePriceFilter(request.getPrice())) {
                    return false;
                }
            }

            // Validate property filters
            if (request.getProperty() != null) {
                if (!validatePropertyFilter(request.getProperty())) {
                    return false;
                }
            }

            // Validate availability filters
            if (request.getAvailability() != null) {
                if (!validateAvailabilityFilter(request.getAvailability())) {
                    return false;
                }
            }

            // Validate review filters
            if (request.getReviews() != null) {
                if (!validateReviewFilter(request.getReviews())) {
                    return false;
                }
            }

            return true;

        } catch (Exception e) {
            log.error("Error validating advanced filters", e);
            return false;
        }
    }

    @Transactional(readOnly = true)
    public FacetedSearchRequest normalizeAdvancedFilters(FacetedSearchRequest request) {
        log.debug("Normalizing advanced filters for search request");

        try {
            var builder = request.toBuilder();

            // Normalize location filters
            if (request.getLocation() != null) {
                builder.location(normalizeLocationFilter(request.getLocation()));
            }

            // Normalize price filters
            if (request.getPrice() != null) {
                builder.price(normalizePriceFilter(request.getPrice()));
            }

            // Normalize property filters
            if (request.getProperty() != null) {
                builder.property(normalizePropertyFilter(request.getProperty()));
            }

            // Normalize availability filters
            if (request.getAvailability() != null) {
                builder.availability(normalizeAvailabilityFilter(request.getAvailability()));
            }

            return builder.build();

        } catch (Exception e) {
            log.error("Error normalizing advanced filters", e);
            return request;
        }
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "facetOptions", key = "#category + ':' + #language")
    public Map<String, Object> getFacetOptions(String category, String language) {
        log.info("Getting facet options for category: '{}' in language: '{}'", category, language);

        try {
            return switch (category.toLowerCase()) {
                case "location" -> getLocationFacetOptions();
                case "property" -> getPropertyFacetOptions();
                case "price" -> getPriceFacetOptions();
                case "amenities" -> getAmenityFacetOptions();
                case "reviews" -> getReviewFacetOptions();
                case "availability" -> getAvailabilityFacetOptions();
                case "business" -> getBusinessFacetOptions();
                default -> Map.of();
            };

        } catch (Exception e) {
            log.error("Failed to get facet options for category: '{}'", category, e);
            return Map.of();
        }
    }

    private Object buildAdvancedSearchQuery(FacetedSearchRequest request) {
        // This would build a complex Elasticsearch query with aggregations
        // Implementation would depend on the specific Elasticsearch client being used
        Map<String, Object> query = new HashMap<>();

        // Base query
        if (request.getQuery() != null && !request.getQuery().trim().isEmpty()) {
            query.put("query", Map.of(
                "multi_match", Map.of(
                    "query", request.getQuery(),
                    "fields", List.of("name^3", "description^2", "city^2", "amenities")
                )
            ));
        }

        // Add filters
        List<Map<String, Object>> filters = new ArrayList<>();

        if (request.getLocation() != null) {
            filters.addAll(buildLocationFilters(request.getLocation()));
        }

        if (request.getProperty() != null) {
            filters.addAll(buildPropertyFilters(request.getProperty()));
        }

        if (request.getPrice() != null) {
            filters.addAll(buildPriceFilters(request.getPrice()));
        }

        if (!filters.isEmpty()) {
            query.put("post_filter", Map.of("bool", Map.of("must", filters)));
        }

        // Add aggregations for facets
        query.put("aggs", buildFacetAggregations(request));

        return query;
    }

    private List<Map<String, Object>> buildLocationFilters(FacetedSearchRequest.AdvancedLocationFilter location) {
        List<Map<String, Object>> filters = new ArrayList<>();

        // Geographic radius filter
        if (location.getLatitude() != null && location.getLongitude() != null && location.getRadiusKm() != null) {
            filters.add(Map.of(
                "geo_distance", Map.of(
                    "distance", location.getRadiusKm() + "km",
                    "location.coordinates", Map.of(
                        "lat", location.getLatitude(),
                        "lon", location.getLongitude()
                    )
                )
            ));
        }

        // City filter
        if (location.getCity() != null) {
            filters.add(Map.of("term", Map.of("city.keyword", location.getCity())));
        }

        // Country filter
        if (location.getCountryCode() != null) {
            filters.add(Map.of("term", Map.of("countryCode", location.getCountryCode())));
        }

        // POI proximity filters
        if (location.getNearbyPois() != null && !location.getNearbyPois().isEmpty()) {
            for (var poi : location.getNearbyPois()) {
                filters.add(buildPoiFilter(poi));
            }
        }

        return filters;
    }

    private List<Map<String, Object>> buildPropertyFilters(FacetedSearchRequest.AdvancedPropertyFilter property) {
        List<Map<String, Object>> filters = new ArrayList<>();

        // Property types
        if (property.getPropertyTypes() != null && !property.getPropertyTypes().isEmpty()) {
            filters.add(Map.of("terms", Map.of("kind", property.getPropertyTypes())));
        }

        // Star rating
        if (property.getStarRating() != null && !property.getStarRating().isEmpty()) {
            filters.add(Map.of("terms", Map.of("starRating", property.getStarRating())));
        }

        // Room count range
        if (property.getMinRooms() != null || property.getMaxRooms() != null) {
            Map<String, Object> rangeFilter = new HashMap<>();
            if (property.getMinRooms() != null) {
                rangeFilter.put("gte", property.getMinRooms());
            }
            if (property.getMaxRooms() != null) {
                rangeFilter.put("lte", property.getMaxRooms());
            }
            filters.add(Map.of("range", Map.of("totalRooms", rangeFilter)));
        }

        // Required amenities
        if (property.getRequiredAmenities() != null && !property.getRequiredAmenities().isEmpty()) {
            for (String amenity : property.getRequiredAmenities()) {
                filters.add(Map.of("term", Map.of("amenities.name.keyword", amenity)));
            }
        }

        // Accessibility
        if (property.getWheelchairAccessible() != null && property.getWheelchairAccessible()) {
            filters.add(Map.of("term", Map.of("amenities.category", "accessibility")));
        }

        return filters;
    }

    private List<Map<String, Object>> buildPriceFilters(FacetedSearchRequest.AdvancedPriceFilter price) {
        List<Map<String, Object>> filters = new ArrayList<>();

        // Price range
        if (price.getMinPrice() != null || price.getMaxPrice() != null) {
            Map<String, Object> rangeFilter = new HashMap<>();
            if (price.getMinPrice() != null) {
                rangeFilter.put("gte", price.getMinPrice());
            }
            if (price.getMaxPrice() != null) {
                rangeFilter.put("lte", price.getMaxPrice());
            }
            filters.add(Map.of("range", Map.of("roomTypes.basePrice", rangeFilter)));
        }

        // Currency
        if (price.getCurrency() != null) {
            filters.add(Map.of("term", Map.of("roomTypes.currency", price.getCurrency())));
        }

        // On sale filter
        if (price.getOnSale() != null && price.getOnSale()) {
            filters.add(Map.of("exists", Map.of("field", "promotions")));
        }

        // Free cancellation
        if (price.getFreeCancellation() != null && price.getFreeCancellation()) {
            filters.add(Map.of("term", Map.of("policies.freeCancellation", true)));
        }

        return filters;
    }

    private Map<String, Object> buildPoiFilter(FacetedSearchRequest.PoiFilter poi) {
        // This would build a filter for properties near a specific POI
        return Map.of("bool", Map.of(
            "must", List.of(
                Map.of("term", Map.of("nearbyPois.type", poi.getType())),
                Map.of("range", Map.of("nearbyPois.distanceKm", Map.of("lte", poi.getMaxDistanceKm())))
            )
        ));
    }

    private Map<String, Object> buildFacetAggregations(FacetedSearchRequest request) {
        Map<String, Object> aggregations = new HashMap<>();

        // Location facets
        aggregations.put("cities", Map.of("terms", Map.of("field", "city.keyword", "size", 20)));
        aggregations.put("countries", Map.of("terms", Map.of("field", "countryCode", "size", 10)));

        // Property facets
        aggregations.put("propertyTypes", Map.of("terms", Map.of("field", "kind", "size", 10)));
        aggregations.put("starRatings", Map.of("terms", Map.of("field", "starRating", "size", 5)));

        // Price facets
        aggregations.put("priceRanges", Map.of(
            "range", Map.of(
                "field", "roomTypes.basePrice",
                "ranges", List.of(
                    Map.of("to", 500000),
                    Map.of("from", 500000, "to", 1000000),
                    Map.of("from", 1000000, "to", 2000000),
                    Map.of("from", 2000000, "to", 5000000),
                    Map.of("from", 5000000)
                )
            )
        ));

        // Amenity facets
        aggregations.put("amenities", Map.of("terms", Map.of("field", "amenities.name.keyword", "size", 50)));

        // Review facets
        aggregations.put("guestRatings", Map.of(
            "range", Map.of(
                "field", "ratingAvg",
                "ranges", List.of(
                    Map.of("from", 4.5),
                    Map.of("from", 4.0, "to", 4.5),
                    Map.of("from", 3.5, "to", 4.0),
                    Map.of("from", 3.0, "to", 3.5)
                )
            )
        ));

        return aggregations;
    }

    // Additional helper methods for validation, normalization, and facet building would go here
    // ... (implementation continues with various validation and normalization methods)

    private int countActiveFilters(FacetedSearchRequest request) {
        int count = 0;

        if (request.getLocation() != null) count++;
        if (request.getProperty() != null) count++;
        if (request.getPrice() != null) count++;
        if (request.getAvailability() != null) count++;
        if (request.getReviews() != null) count++;
        if (request.getBusiness() != null) count++;

        return count;
    }

    // Placeholder implementations for validation methods
    private boolean validateLocationFilter(FacetedSearchRequest.AdvancedLocationFilter location) {
        // Validate coordinates, radius, etc.
        return true;
    }

    private boolean validatePriceFilter(FacetedSearchRequest.AdvancedPriceFilter price) {
        // Validate price ranges, currency, etc.
        return true;
    }

    private boolean validatePropertyFilter(FacetedSearchRequest.AdvancedPropertyFilter property) {
        // Validate property filters
        return true;
    }

    private boolean validateAvailabilityFilter(FacetedSearchRequest.AvailabilityFilter availability) {
        // Validate dates, guest requirements, etc.
        return true;
    }

    private boolean validateReviewFilter(FacetedSearchRequest.ReviewFilter reviews) {
        // Validate rating ranges, review counts, etc.
        return true;
    }

    // Placeholder implementations for normalization methods
    private FacetedSearchRequest.AdvancedLocationFilter normalizeLocationFilter(
            FacetedSearchRequest.AdvancedLocationFilter location) {
        return location;
    }

    private FacetedSearchRequest.AdvancedPriceFilter normalizePriceFilter(
            FacetedSearchRequest.AdvancedPriceFilter price) {
        return price;
    }

    private FacetedSearchRequest.AdvancedPropertyFilter normalizePropertyFilter(
            FacetedSearchRequest.AdvancedPropertyFilter property) {
        return property;
    }

    private FacetedSearchRequest.AvailabilityFilter normalizeAvailabilityFilter(
            FacetedSearchRequest.AvailabilityFilter availability) {
        return availability;
    }

    // Placeholder implementations for facet option methods
    private Map<String, Object> getLocationFacetOptions() {
        return Map.of("cities", List.of("Hà Nội", "Hồ Chí Minh", "Đà Nẵng"));
    }

    private Map<String, Object> getPropertyFacetOptions() {
        return Map.of("types", List.of("hotel", "villa", "homestay"));
    }

    private Map<String, Object> getPriceFacetOptions() {
        return Map.of("ranges", List.of("0-500K", "500K-1M", "1M-2M"));
    }

    private Map<String, Object> getAmenityFacetOptions() {
        return Map.of("categories", Map.of("leisure", List.of("pool", "spa", "gym")));
    }

    private Map<String, Object> getReviewFacetOptions() {
        return Map.of("ratings", List.of("4.5+", "4.0+", "3.5+"));
    }

    private Map<String, Object> getAvailabilityFacetOptions() {
        return Map.of("booking", List.of("instant", "request"));
    }

    private Map<String, Object> getBusinessFacetOptions() {
        return Map.of("types", List.of("direct", "ota", "partner"));
    }

    // Placeholder implementations for response building methods
    private List<PropertySearchResult> processSearchResults(Object searchResults, FacetedSearchRequest request) {
        return List.of(); // Would process actual search results
    }

    private FacetedSearchResponse.SearchFacets buildSearchFacets(Object searchResults, FacetedSearchRequest request) {
        return FacetedSearchResponse.SearchFacets.builder().build(); // Would build from aggregations
    }

    private FacetedSearchResponse.AppliedFilters buildAppliedFilters(FacetedSearchRequest request) {
        return FacetedSearchResponse.AppliedFilters.builder()
            .totalFilterCount(countActiveFilters(request))
            .activeFilters(Map.of())
            .build();
    }

    private FacetedSearchResponse.SearchMetadata buildSearchMetadata(long startTime, FacetedSearchRequest request, int resultCount) {
        return FacetedSearchResponse.SearchMetadata.builder()
            .responseTimeMs(System.currentTimeMillis() - startTime)
            .searchId(UUID.randomUUID().toString())
            .totalResults((long) resultCount)
            .language(request.getLanguage())
            .cacheHit(false)
            .build();
    }

    private PaginationInfo buildPagination(Object searchResults, FacetedSearchRequest request) {
        return PaginationInfo.builder().build(); // Would build pagination info
    }

    private FacetedSearchResponse createEmptyResponse(FacetedSearchRequest request) {
        return FacetedSearchResponse.builder()
            .results(List.of())
            .facets(FacetedSearchResponse.SearchFacets.builder().build())
            .appliedFilters(FacetedSearchResponse.AppliedFilters.builder().build())
            .metadata(FacetedSearchResponse.SearchMetadata.builder().build())
            .pagination(PaginationInfo.builder().build())
            .build();
    }
}