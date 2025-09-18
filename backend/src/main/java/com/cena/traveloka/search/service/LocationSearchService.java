package com.cena.traveloka.search.service;

import com.cena.traveloka.search.dto.PropertySearchRequest;
import com.cena.traveloka.search.dto.PropertySearchResponse;
import com.cena.traveloka.search.entity.PopularDestination;
import com.cena.traveloka.search.entity.SearchIndex;
import com.cena.traveloka.search.repository.PopularDestinationRepository;
import com.cena.traveloka.search.repository.PropertyElasticsearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocationSearchService {

    private final PropertyElasticsearchRepository elasticsearchRepository;
    private final PopularDestinationRepository popularDestinationRepository;

    @Transactional(readOnly = true)
    public PropertySearchResponse searchNearLocation(Double latitude, Double longitude, Double radiusKm,
                                                   String query, Pageable pageable) {
        log.info("Searching properties near location: lat={}, lng={}, radius={}km, query='{}'",
                latitude, longitude, radiusKm, query);

        try {
            SearchHits<SearchIndex> searchHits;

            if (query != null && !query.trim().isEmpty()) {
                // Search with both location and text query
                searchHits = elasticsearchRepository.searchByQueryAndLocation(
                    normalizeVietnameseQuery(query), latitude, longitude, radiusKm, pageable
                );
            } else {
                // Search by location only
                searchHits = elasticsearchRepository.findByLocationWithinRadius(
                    latitude, longitude, radiusKm, pageable
                );
            }

            return convertToLocationBasedResponse(searchHits, latitude, longitude, pageable);

        } catch (Exception e) {
            log.error("Failed to search near location: lat={}, lng={}, radius={}km",
                    latitude, longitude, radiusKm, e);
            return createEmptyLocationResponse(pageable);
        }
    }

    @Transactional(readOnly = true)
    public List<PopularDestination> findNearbyDestinations(Double latitude, Double longitude, Double radiusKm) {
        log.info("Finding destinations near location: lat={}, lng={}, radius={}km",
                latitude, longitude, radiusKm);

        try {
            return popularDestinationRepository.findDestinationsNearLocation(
                latitude, longitude, radiusKm * 1000 // Convert km to meters
            );

        } catch (Exception e) {
            log.error("Failed to find nearby destinations", e);
            return List.of();
        }
    }

    @Transactional(readOnly = true)
    public PropertySearchResponse searchInCity(String cityName, String countryCode, String query, Pageable pageable) {
        log.info("Searching properties in city: {}, {}, query: '{}'", cityName, countryCode, query);

        try {
            // Get city coordinates for enhanced results
            List<PopularDestination> cityDestinations = popularDestinationRepository
                .findByDestinationNameContaining(cityName);

            SearchHits<SearchIndex> searchHits;

            if (query != null && !query.trim().isEmpty()) {
                // Search with both city and text query
                String normalizedQuery = normalizeVietnameseQuery(query) + " " + cityName;
                searchHits = elasticsearchRepository.searchByQuery(normalizedQuery, pageable);
            } else {
                // Search by city only
                searchHits = elasticsearchRepository.findByCity(List.of(cityName), pageable);
            }

            return convertToCityBasedResponse(searchHits, cityName, countryCode, pageable);

        } catch (Exception e) {
            log.error("Failed to search in city: {}, {}", cityName, countryCode, e);
            return createEmptyLocationResponse(pageable);
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getLocationAnalytics(Double latitude, Double longitude, Double radiusKm) {
        log.info("Getting location analytics for lat={}, lng={}, radius={}km",
                latitude, longitude, radiusKm);

        try {
            Map<String, Object> analytics = new HashMap<>();

            // Get property count in area
            SearchHits<SearchIndex> propertiesInArea = elasticsearchRepository.findByLocationWithinRadius(
                latitude, longitude, radiusKm, PageRequest.of(0, 1)
            );
            analytics.put("propertyCount", propertiesInArea.getTotalHits());

            // Get popular destinations in area
            List<PopularDestination> nearbyDestinations = findNearbyDestinations(latitude, longitude, radiusKm);
            analytics.put("destinationCount", nearbyDestinations.size());

            // Calculate average property rating in area
            SearchHits<SearchIndex> allPropertiesInArea = elasticsearchRepository.findByLocationWithinRadius(
                latitude, longitude, radiusKm, PageRequest.of(0, 100)
            );

            double avgRating = allPropertiesInArea.getSearchHits().stream()
                .mapToDouble(hit -> {
                    SearchIndex property = hit.getContent();
                    return property.getRatingAvg() != null ? property.getRatingAvg().doubleValue() : 0.0;
                })
                .filter(rating -> rating > 0)
                .average()
                .orElse(0.0);

            analytics.put("averageRating", avgRating);

            // Get price range in area
            Map<String, BigDecimal> priceRange = calculatePriceRange(allPropertiesInArea);
            analytics.put("priceRange", priceRange);

            // Get amenity distribution
            Map<String, Integer> amenityDistribution = calculateAmenityDistribution(allPropertiesInArea);
            analytics.put("amenityDistribution", amenityDistribution);

            return analytics;

        } catch (Exception e) {
            log.error("Failed to get location analytics", e);
            return Map.of("error", "Failed to retrieve location analytics");
        }
    }

    @Transactional(readOnly = true)
    public List<String> getSuggestedLocations(String query, String language, int limit) {
        log.info("Getting location suggestions for query: '{}' in language: '{}'", query, language);

        try {
            // Get city suggestions from search index
            List<SearchIndex> citySuggestions = elasticsearchRepository.findSuggestionsByCity(query);

            List<String> suggestions = citySuggestions.stream()
                .map(SearchIndex::getCity)
                .distinct()
                .limit(limit / 2)
                .collect(Collectors.toList());

            // Add destination suggestions
            List<PopularDestination> destinationSuggestions = popularDestinationRepository
                .findByDestinationNameContaining(query);

            List<String> destinationNames = destinationSuggestions.stream()
                .map(PopularDestination::getDestinationName)
                .limit(limit - suggestions.size())
                .toList();

            suggestions.addAll(destinationNames);

            return suggestions.stream().distinct().limit(limit).collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to get location suggestions", e);
            return List.of();
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getPopularLocations(String countryCode, int limit) {
        log.info("Getting popular locations for country: {}", countryCode);

        try {
            Map<String, Object> locations = new HashMap<>();

            // Get popular cities
            List<PopularDestination> cities = popularDestinationRepository
                .findByDestinationTypeAndCountryCodeOrderByPopularityRankAsc(
                    PopularDestination.DestinationType.CITY, countryCode
                );

            locations.put("cities", cities.stream()
                .limit(limit / 2)
                .map(this::convertDestinationToLocationInfo)
                .collect(Collectors.toList()));

            // Get popular landmarks
            List<PopularDestination> landmarks = popularDestinationRepository
                .findByDestinationTypeAndCountryCodeOrderByPopularityRankAsc(
                    PopularDestination.DestinationType.LANDMARK, countryCode
                );

            locations.put("landmarks", landmarks.stream()
                .limit(limit / 2)
                .map(this::convertDestinationToLocationInfo)
                .collect(Collectors.toList()));

            return locations;

        } catch (Exception e) {
            log.error("Failed to get popular locations for country: {}", countryCode, e);
            return Map.of();
        }
    }

    @Transactional(readOnly = true)
    public boolean isLocationSupported(Double latitude, Double longitude) {
        try {
            // Check if we have any properties within 50km of the location
            SearchHits<SearchIndex> nearbyProperties = elasticsearchRepository.findByLocationWithinRadius(
                latitude, longitude, 50.0, PageRequest.of(0, 1)
            );

            return nearbyProperties.getTotalHits() > 0;

        } catch (Exception e) {
            log.error("Failed to check location support for lat={}, lng={}", latitude, longitude, e);
            return false;
        }
    }

    private PropertySearchResponse convertToLocationBasedResponse(SearchHits<SearchIndex> searchHits,
                                                                Double latitude, Double longitude,
                                                                Pageable pageable) {

        List<PropertySearchResponse.PropertyResult> properties = searchHits.getSearchHits().stream()
            .map(hit -> {
                PropertySearchResponse.PropertyResult result = convertToPropertyResult(hit.getContent(), hit.getScore());

                // Add distance calculation for location-based searches
                if (result.getLocation() != null && result.getLocation().getCoordinates() != null) {
                    double distance = calculateDistance(
                        latitude, longitude,
                        result.getLocation().getCoordinates().getLatitude().doubleValue(),
                        result.getLocation().getCoordinates().getLongitude().doubleValue()
                    );
                    result.setDistanceKm(BigDecimal.valueOf(distance));
                }

                return result;
            })
            .collect(Collectors.toList());

        PropertySearchResponse.PageInfo pageInfo = PropertySearchResponse.PageInfo.builder()
            .currentPage(pageable.getPageNumber())
            .pageSize(pageable.getPageSize())
            .totalPages((int) Math.ceil((double) searchHits.getTotalHits() / pageable.getPageSize()))
            .hasNext(pageable.getPageNumber() < (searchHits.getTotalHits() / pageable.getPageSize()) - 1)
            .hasPrevious(pageable.getPageNumber() > 0)
            .build();

        PropertySearchResponse.SearchMetadata metadata = PropertySearchResponse.SearchMetadata.builder()
            .searchId(java.util.UUID.randomUUID().toString())
            .responseTimeMs(System.currentTimeMillis())
            .cacheHit(false)
            .language("vi")
            .currency("VND")
            .searchTimestamp(java.time.OffsetDateTime.now())
            .build();

        return PropertySearchResponse.builder()
            .totalCount(searchHits.getTotalHits())
            .pageInfo(pageInfo)
            .properties(properties)
            .metadata(metadata)
            .aggregations(PropertySearchResponse.SearchAggregations.builder().build())
            .build();
    }

    private PropertySearchResponse convertToCityBasedResponse(SearchHits<SearchIndex> searchHits,
                                                            String cityName, String countryCode,
                                                            Pageable pageable) {
        return convertToLocationBasedResponse(searchHits, null, null, pageable);
    }

    private PropertySearchResponse createEmptyLocationResponse(Pageable pageable) {
        return PropertySearchResponse.builder()
            .totalCount(0L)
            .pageInfo(PropertySearchResponse.PageInfo.builder()
                .currentPage(0)
                .pageSize(pageable.getPageSize())
                .totalPages(0)
                .hasNext(false)
                .hasPrevious(false)
                .build())
            .properties(List.of())
            .metadata(PropertySearchResponse.SearchMetadata.builder()
                .searchId(java.util.UUID.randomUUID().toString())
                .responseTimeMs(System.currentTimeMillis())
                .cacheHit(false)
                .language("vi")
                .currency("VND")
                .searchTimestamp(java.time.OffsetDateTime.now())
                .build())
            .aggregations(PropertySearchResponse.SearchAggregations.builder().build())
            .build();
    }

    private PropertySearchResponse.PropertyResult convertToPropertyResult(SearchIndex searchIndex, float score) {
        // This would be similar to the conversion in SearchService
        // For brevity, returning a basic conversion
        return PropertySearchResponse.PropertyResult.builder()
            .id(searchIndex.getPropertyId())
            .name(searchIndex.getName())
            .description(searchIndex.getDescription())
            .propertyType(searchIndex.getKind())
            .starRating(searchIndex.getStarRating())
            .searchScore(score)
            .build();
    }

    private Map<String, Object> convertDestinationToLocationInfo(PopularDestination destination) {
        Map<String, Object> locationInfo = new HashMap<>();
        locationInfo.put("name", destination.getDestinationName());
        locationInfo.put("type", destination.getDestinationType().name());
        locationInfo.put("countryCode", destination.getCountryCode());
        locationInfo.put("popularityRank", destination.getPopularityRank());
        locationInfo.put("searchVolume", destination.getSearchMetrics().getSearchVolume());

        if (destination.getCoordinates() != null) {
            locationInfo.put("coordinates", Map.of(
                "latitude", destination.getCoordinates().getY(),
                "longitude", destination.getCoordinates().getX()
            ));
        }

        return locationInfo;
    }

    private Map<String, BigDecimal> calculatePriceRange(SearchHits<SearchIndex> searchHits) {
        List<BigDecimal> prices = searchHits.getSearchHits().stream()
            .flatMap(hit -> {
                SearchIndex property = hit.getContent();
                if (property.getRoomTypes() != null) {
                    return property.getRoomTypes().stream()
                        .map(SearchIndex.RoomTypeData::getBasePrice)
                        .filter(price -> price != null);
                }
                return java.util.stream.Stream.empty();
            })
            .collect(Collectors.toList());

        if (prices.isEmpty()) {
            return Map.of("min", BigDecimal.ZERO, "max", BigDecimal.ZERO);
        }

        BigDecimal min = prices.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal max = prices.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);

        return Map.of("min", min, "max", max);
    }

    private Map<String, Integer> calculateAmenityDistribution(SearchHits<SearchIndex> searchHits) {
        Map<String, Integer> distribution = new HashMap<>();

        searchHits.getSearchHits().forEach(hit -> {
            SearchIndex property = hit.getContent();
            if (property.getAmenities() != null) {
                property.getAmenities().forEach(amenity -> {
                    String category = amenity.getCategory();
                    distribution.merge(category, 1, Integer::sum);
                });
            }
        });

        return distribution;
    }

    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        // Haversine formula for calculating distance between two points
        final int R = 6371; // Radius of the earth in km

        double latDistance = Math.toRadians(lat2 - lat1);
        double lngDistance = Math.toRadians(lng2 - lng1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c; // Distance in km
    }

    private String normalizeVietnameseQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            return "";
        }

        // Basic Vietnamese text normalization
        return query.toLowerCase()
            .replace("đ", "d")
            .replace("á", "a").replace("à", "a").replace("ả", "a").replace("ã", "a").replace("ạ", "a")
            .replace("ă", "a").replace("ắ", "a").replace("ằ", "a").replace("ẳ", "a").replace("ẵ", "a").replace("ặ", "a")
            .replace("â", "a").replace("ấ", "a").replace("ầ", "a").replace("ẩ", "a").replace("ẫ", "a").replace("ậ", "a")
            .replace("é", "e").replace("è", "e").replace("ẻ", "e").replace("ẽ", "e").replace("ẹ", "e")
            .replace("ê", "e").replace("ế", "e").replace("ề", "e").replace("ể", "e").replace("ễ", "e").replace("ệ", "e")
            .replace("í", "i").replace("ì", "i").replace("ỉ", "i").replace("ĩ", "i").replace("ị", "i")
            .replace("ó", "o").replace("ò", "o").replace("ỏ", "o").replace("õ", "o").replace("ọ", "o")
            .replace("ô", "o").replace("ố", "o").replace("ồ", "o").replace("ổ", "o").replace("ỗ", "o").replace("ộ", "o")
            .replace("ơ", "o").replace("ớ", "o").replace("ờ", "o").replace("ở", "o").replace("ỡ", "o").replace("ợ", "o")
            .replace("ú", "u").replace("ù", "u").replace("ủ", "u").replace("ũ", "u").replace("ụ", "u")
            .replace("ư", "u").replace("ứ", "u").replace("ừ", "u").replace("ử", "u").replace("ữ", "u").replace("ự", "u")
            .replace("ý", "y").replace("ỳ", "y").replace("ỷ", "y").replace("ỹ", "y").replace("ỵ", "y")
            .trim();
    }
}
