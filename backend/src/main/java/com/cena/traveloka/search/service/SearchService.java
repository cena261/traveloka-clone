package com.cena.traveloka.search.service;

import com.cena.traveloka.search.dto.PropertySearchRequest;
import com.cena.traveloka.search.dto.PropertySearchResponse;
import com.cena.traveloka.search.entity.SearchHistory;
import com.cena.traveloka.search.entity.SearchIndex;
import com.cena.traveloka.search.repository.PropertyElasticsearchRepository;
import com.cena.traveloka.search.repository.SearchHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.SearchHits;
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
public class SearchService {

    private final PropertyElasticsearchRepository elasticsearchRepository;
    private final SearchHistoryRepository searchHistoryRepository;
    private final SearchAnalyticsService analyticsService;

    @Cacheable(value = "searchResults", key = "#request.toString().hashCode()")
    @Transactional(readOnly = true)
    public PropertySearchResponse searchProperties(PropertySearchRequest request) {
        log.info("Executing property search for query: '{}' with language: '{}'",
                request.getQuery(), request.getLanguage());

        long startTime = System.currentTimeMillis();

        try {
            // Create pageable with sorting
            Pageable pageable = createPageable(request);

            // Execute search based on filters
            SearchHits<SearchIndex> searchHits = executeSearch(request, pageable);

            // Convert to response
            PropertySearchResponse response = convertToResponse(searchHits, request, pageable);

            // Record search analytics
            long responseTime = System.currentTimeMillis() - startTime;
            recordSearchAnalytics(request, response, responseTime);

            log.info("Search completed in {}ms, found {} results", responseTime, response.getTotalCount());
            return response;

        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            log.error("Search failed after {}ms for query: '{}'", responseTime, request.getQuery(), e);

            // Record failed search
            recordFailedSearch(request, responseTime);

            // Return empty response instead of throwing
            return createEmptyResponse(request);
        }
    }

    private Pageable createPageable(PropertySearchRequest request) {
        Sort sort = switch (request.getSortBy()) {
            case "PRICE_LOW_TO_HIGH" -> Sort.by(Sort.Direction.ASC, "room_types.base_price");
            case "PRICE_HIGH_TO_LOW" -> Sort.by(Sort.Direction.DESC, "room_types.base_price");
            case "RATING" -> Sort.by(Sort.Direction.DESC, "rating_avg", "rating_count");
            case "DISTANCE" -> Sort.by(Sort.Direction.ASC, "_score");
            case "POPULARITY" -> Sort.by(Sort.Direction.DESC, "search_boost.popularity_score");
            default -> Sort.by(Sort.Direction.DESC, "_score");
        };

        return PageRequest.of(
            request.getPagination().getPage(),
            request.getPagination().getSize(),
            sort
        );
    }

    private SearchHits<SearchIndex> executeSearch(PropertySearchRequest request, Pageable pageable) {
        // Normalize Vietnamese query
        String normalizedQuery = normalizeVietnameseQuery(request.getQuery());

        // Check if we have location, price, and other complex filters
        boolean hasLocationFilter = hasLocationFilter(request);
        boolean hasPriceFilter = hasPriceFilter(request);
        boolean hasPropertyFilter = hasPropertyFilter(request);

        if (hasLocationFilter && hasPriceFilter && hasPropertyFilter) {
            // Complex multi-filter search
            return executeComplexSearch(normalizedQuery, request, pageable);
        } else if (hasLocationFilter) {
            // Location-based search
            return executeLocationSearch(normalizedQuery, request, pageable);
        } else if (hasPriceFilter) {
            // Price-filtered search
            return executePriceSearch(normalizedQuery, request, pageable);
        } else if (hasPropertyFilter && request.getProperty().getStarRating() != null) {
            // Star rating filtered search
            return elasticsearchRepository.searchByQueryAndStarRating(
                normalizedQuery, request.getProperty().getStarRating(), pageable
            );
        } else if (hasPropertyFilter && request.getProperty().getAmenities() != null) {
            // Amenity filtered search
            return elasticsearchRepository.searchByQueryAndAmenities(
                normalizedQuery, request.getProperty().getAmenities(), pageable
            );
        } else {
            // Simple text search
            return elasticsearchRepository.searchByQuery(normalizedQuery, pageable);
        }
    }

    private SearchHits<SearchIndex> executeComplexSearch(String query, PropertySearchRequest request, Pageable pageable) {
        PropertySearchRequest.LocationFilter location = request.getLocation();
        PropertySearchRequest.PriceFilter price = request.getPrice();
        List<Integer> starRatings = request.getProperty() != null ? request.getProperty().getStarRating() : null;

        return elasticsearchRepository.searchWithComplexFilters(
            query,
            location.getLatitude().doubleValue(),
            location.getLongitude().doubleValue(),
            location.getRadiusKm().doubleValue(),
            price.getMinPrice(),
            price.getMaxPrice(),
            starRatings != null ? starRatings : List.of(),
            pageable
        );
    }

    private SearchHits<SearchIndex> executeLocationSearch(String query, PropertySearchRequest request, Pageable pageable) {
        PropertySearchRequest.LocationFilter location = request.getLocation();
        return elasticsearchRepository.searchByQueryAndLocation(
            query,
            location.getLatitude().doubleValue(),
            location.getLongitude().doubleValue(),
            location.getRadiusKm().doubleValue(),
            pageable
        );
    }

    private SearchHits<SearchIndex> executePriceSearch(String query, PropertySearchRequest request, Pageable pageable) {
        PropertySearchRequest.PriceFilter price = request.getPrice();
        return elasticsearchRepository.searchByQueryAndPriceRange(
            query,
            price.getMinPrice(),
            price.getMaxPrice(),
            pageable
        );
    }

    private PropertySearchResponse convertToResponse(SearchHits<SearchIndex> searchHits,
                                                   PropertySearchRequest request,
                                                   Pageable pageable) {

        List<PropertySearchResponse.PropertyResult> properties = searchHits.getSearchHits().stream()
            .map(hit -> convertToPropertyResult(hit.getContent(), hit.getScore()))
            .collect(Collectors.toList());

        PropertySearchResponse.PageInfo pageInfo = PropertySearchResponse.PageInfo.builder()
            .currentPage(pageable.getPageNumber())
            .pageSize(pageable.getPageSize())
            .totalPages((int) Math.ceil((double) searchHits.getTotalHits() / pageable.getPageSize()))
            .hasNext(pageable.getPageNumber() < (searchHits.getTotalHits() / pageable.getPageSize()) - 1)
            .hasPrevious(pageable.getPageNumber() > 0)
            .build();

        PropertySearchResponse.SearchMetadata metadata = PropertySearchResponse.SearchMetadata.builder()
            .searchId(UUID.randomUUID().toString())
            .responseTimeMs(System.currentTimeMillis())
            .cacheHit(false) // Will be set by cache interceptor
            .language(request.getLanguage())
            .currency(request.getPrice() != null ? request.getPrice().getCurrency() : "VND")
            .searchTimestamp(OffsetDateTime.now())
            .build();

        PropertySearchResponse.SearchAggregations aggregations = buildAggregations();

        return PropertySearchResponse.builder()
            .totalCount(searchHits.getTotalHits())
            .pageInfo(pageInfo)
            .properties(properties)
            .metadata(metadata)
            .aggregations(aggregations)
            .build();
    }

    private PropertySearchResponse.PropertyResult convertToPropertyResult(SearchIndex searchIndex, float score) {
        return PropertySearchResponse.PropertyResult.builder()
            .id(searchIndex.getPropertyId())
            .name(searchIndex.getName())
            .description(searchIndex.getDescription())
            .propertyType(searchIndex.getKind())
            .starRating(searchIndex.getStarRating())
            .location(convertLocationInfo(searchIndex.getLocation()))
            .ratingInfo(convertRatingInfo(searchIndex))
            .priceInfo(convertPriceInfo(searchIndex))
            .amenities(convertAmenities(searchIndex.getAmenities()))
            .images(convertImages(searchIndex.getImages()))
            .roomTypes(convertRoomTypes(searchIndex.getRoomTypes()))
            .searchScore(score)
            .isPromoted(searchIndex.getSearchBoost() != null ?
                       searchIndex.getSearchBoost().getIsPromoted() : false)
            .build();
    }

    private PropertySearchResponse.LocationInfo convertLocationInfo(SearchIndex.LocationData location) {
        if (location == null) return null;

        return PropertySearchResponse.LocationInfo.builder()
            .city(location.getCity())
            .countryCode(location.getCountryCode())
            .address(location.getAddress())
            .postalCode(location.getPostalCode())
            .coordinates(location.getCoordinates() != null ?
                PropertySearchResponse.GeoLocation.builder()
                    .latitude(BigDecimal.valueOf(location.getCoordinates().getLat()))
                    .longitude(BigDecimal.valueOf(location.getCoordinates().getLon()))
                    .build() : null)
            .build();
    }

    private PropertySearchResponse.RatingInfo convertRatingInfo(SearchIndex searchIndex) {
        return PropertySearchResponse.RatingInfo.builder()
            .averageRating(searchIndex.getRatingAvg())
            .totalReviews(searchIndex.getRatingCount())
            .build();
    }

    private PropertySearchResponse.PriceInfo convertPriceInfo(SearchIndex searchIndex) {
        // Get the lowest price from room types
        BigDecimal lowestPrice = searchIndex.getRoomTypes() != null ?
            searchIndex.getRoomTypes().stream()
                .map(SearchIndex.RoomTypeData::getBasePrice)
                .filter(price -> price != null)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO) : BigDecimal.ZERO;

        return PropertySearchResponse.PriceInfo.builder()
            .basePrice(lowestPrice)
            .currency("VND")
            .pricePerNight(true)
            .taxesIncluded(false)
            .build();
    }

    private List<PropertySearchResponse.AmenityInfo> convertAmenities(List<SearchIndex.AmenityData> amenities) {
        if (amenities == null) return List.of();

        return amenities.stream()
            .map(amenity -> PropertySearchResponse.AmenityInfo.builder()
                .id(amenity.getId())
                .name(amenity.getName())
                .category(amenity.getCategory())
                .isFeatured(amenity.getIsFeatured())
                .build())
            .collect(Collectors.toList());
    }

    private List<PropertySearchResponse.ImageInfo> convertImages(List<SearchIndex.ImageData> images) {
        if (images == null) return List.of();

        return images.stream()
            .map(image -> PropertySearchResponse.ImageInfo.builder()
                .id(image.getId())
                .url(image.getUrl())
                .alt(image.getAlt())
                .type(image.getType())
                .isPrimary(image.getIsPrimary())
                .sortOrder(image.getSortOrder())
                .build())
            .collect(Collectors.toList());
    }

    private List<PropertySearchResponse.RoomTypeInfo> convertRoomTypes(List<SearchIndex.RoomTypeData> roomTypes) {
        if (roomTypes == null) return List.of();

        return roomTypes.stream()
            .map(room -> PropertySearchResponse.RoomTypeInfo.builder()
                .id(room.getId())
                .name(room.getName())
                .description(room.getDescription())
                .maxOccupancy(room.getMaxOccupancy())
                .availableRooms(room.getAvailableRooms())
                .basePrice(room.getBasePrice())
                .currency(room.getCurrency())
                .build())
            .collect(Collectors.toList());
    }

    private PropertySearchResponse.SearchAggregations buildAggregations() {
        // This would typically be built from Elasticsearch aggregations
        // For now, return empty aggregations
        return PropertySearchResponse.SearchAggregations.builder()
            .priceRanges(List.of())
            .starRatings(List.of())
            .propertyTypes(List.of())
            .amenities(List.of())
            .locations(List.of())
            .guestRatings(List.of())
            .build();
    }

    private void recordSearchAnalytics(PropertySearchRequest request, PropertySearchResponse response, long responseTime) {
        try {
            SearchHistory searchHistory = SearchHistory.builder()
                .sessionId(UUID.randomUUID()) // Would come from session
                .searchQuery(request.getQuery())
                .searchType(SearchHistory.SearchType.FULL_TEXT)
                .totalResults(response.getTotalCount().intValue())
                .responseTimeMs((int) responseTime)
                .language(request.getLanguage())
                .searchTimestamp(OffsetDateTime.now())
                .build();

            searchHistoryRepository.save(searchHistory);

        } catch (Exception e) {
            log.warn("Failed to record search analytics", e);
        }
    }

    private void recordFailedSearch(PropertySearchRequest request, long responseTime) {
        try {
            SearchHistory searchHistory = SearchHistory.builder()
                .sessionId(UUID.randomUUID())
                .searchQuery(request.getQuery())
                .searchType(SearchHistory.SearchType.FULL_TEXT)
                .totalResults(0)
                .responseTimeMs((int) responseTime)
                .language(request.getLanguage())
                .searchTimestamp(OffsetDateTime.now())
                .build();

            searchHistoryRepository.save(searchHistory);

        } catch (Exception e) {
            log.warn("Failed to record failed search analytics", e);
        }
    }

    private PropertySearchResponse createEmptyResponse(PropertySearchRequest request) {
        return PropertySearchResponse.builder()
            .totalCount(0L)
            .pageInfo(PropertySearchResponse.PageInfo.builder()
                .currentPage(0)
                .pageSize(request.getPagination().getSize())
                .totalPages(0)
                .hasNext(false)
                .hasPrevious(false)
                .build())
            .properties(List.of())
            .metadata(PropertySearchResponse.SearchMetadata.builder()
                .searchId(UUID.randomUUID().toString())
                .responseTimeMs(System.currentTimeMillis())
                .cacheHit(false)
                .language(request.getLanguage())
                .currency("VND")
                .searchTimestamp(OffsetDateTime.now())
                .build())
            .aggregations(PropertySearchResponse.SearchAggregations.builder()
                .priceRanges(List.of())
                .starRatings(List.of())
                .propertyTypes(List.of())
                .amenities(List.of())
                .locations(List.of())
                .guestRatings(List.of())
                .build())
            .build();
    }

    private String normalizeVietnameseQuery(String query) {
        if (query == null) return "";

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

    private boolean hasLocationFilter(PropertySearchRequest request) {
        return request.getLocation() != null &&
               request.getLocation().getLatitude() != null &&
               request.getLocation().getLongitude() != null;
    }

    private boolean hasPriceFilter(PropertySearchRequest request) {
        return request.getPrice() != null &&
               (request.getPrice().getMinPrice() != null || request.getPrice().getMaxPrice() != null);
    }

    private boolean hasPropertyFilter(PropertySearchRequest request) {
        return request.getProperty() != null &&
               (request.getProperty().getStarRating() != null ||
                request.getProperty().getAmenities() != null ||
                request.getProperty().getPropertyTypes() != null);
    }
}
