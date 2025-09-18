package com.cena.traveloka.search.service;

import com.cena.traveloka.search.dto.SuggestionResponse;
import com.cena.traveloka.search.entity.PopularDestination;
import com.cena.traveloka.search.entity.SearchIndex;
import com.cena.traveloka.search.repository.PopularDestinationRepository;
import com.cena.traveloka.search.repository.PropertyElasticsearchRepository;
import com.cena.traveloka.search.repository.SearchHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AutoCompleteService {

    private final PropertyElasticsearchRepository elasticsearchRepository;
    private final PopularDestinationRepository popularDestinationRepository;
    private final SearchHistoryRepository searchHistoryRepository;

    @Cacheable(value = "searchSuggestions", key = "#query + ':' + #language")
    @Transactional(readOnly = true)
    public SuggestionResponse getSuggestions(String query, String language, BigDecimal latitude, BigDecimal longitude, Integer limit) {
        log.info("Getting suggestions for query: '{}' in language: '{}'", query, language);

        long startTime = System.currentTimeMillis();

        try {
            List<SuggestionResponse.Suggestion> suggestions = new ArrayList<>();

            // Normalize and validate inputs
            String normalizedQuery = normalizeVietnameseQuery(query);
            int suggestionLimit = limit != null ? Math.min(limit, 50) : 10;

            // Get different types of suggestions
            suggestions.addAll(getLocationSuggestions(normalizedQuery, language, suggestionLimit / 4));
            suggestions.addAll(getPropertySuggestions(normalizedQuery, language, suggestionLimit / 4));
            suggestions.addAll(getPopularSearchSuggestions(normalizedQuery, language, suggestionLimit / 4));
            suggestions.addAll(getDestinationSuggestions(normalizedQuery, language, latitude, longitude, suggestionLimit / 4));

            // Sort by score and limit results
            List<SuggestionResponse.Suggestion> sortedSuggestions = suggestions.stream()
                .sorted((s1, s2) -> Float.compare(s2.getScore(), s1.getScore()))
                .limit(suggestionLimit)
                .collect(Collectors.toList());

            long responseTime = System.currentTimeMillis() - startTime;

            SuggestionResponse.ResponseMetadata metadata = SuggestionResponse.ResponseMetadata.builder()
                .responseTimeMs(responseTime)
                .cacheHit(false) // Will be set by cache interceptor
                .language(language)
                .suggestionCount(sortedSuggestions.size())
                .queryNormalized(normalizedQuery)
                .autocompleteSessionId(UUID.randomUUID().toString())
                .build();

            log.info("Generated {} suggestions in {}ms for query: '{}'", sortedSuggestions.size(), responseTime, query);

            return SuggestionResponse.builder()
                .suggestions(sortedSuggestions)
                .responseMetadata(metadata)
                .build();

        } catch (Exception e) {
            log.error("Failed to generate suggestions for query: '{}'", query, e);
            return createEmptyResponse(query, language);
        }
    }

    private List<SuggestionResponse.Suggestion> getLocationSuggestions(String query, String language, int limit) {
        try {
            List<SearchIndex> locationResults = elasticsearchRepository.findSuggestionsByCity(query);

            return locationResults.stream()
                .limit(limit)
                .map(this::convertToLocationSuggestion)
                .collect(Collectors.toList());

        } catch (Exception e) {
            log.warn("Failed to get location suggestions", e);
            return List.of();
        }
    }

    private List<SuggestionResponse.Suggestion> getPropertySuggestions(String query, String language, int limit) {
        try {
            List<SearchIndex> propertyResults = elasticsearchRepository.findSuggestionsByName(query);

            return propertyResults.stream()
                .limit(limit)
                .map(this::convertToPropertySuggestion)
                .collect(Collectors.toList());

        } catch (Exception e) {
            log.warn("Failed to get property suggestions", e);
            return List.of();
        }
    }

    private List<SuggestionResponse.Suggestion> getPopularSearchSuggestions(String query, String language, int limit) {
        try {
            OffsetDateTime since = OffsetDateTime.now().minusDays(30);
            List<Object[]> popularQueries = searchHistoryRepository.findPopularSearchQueries(since, 5, limit);

            return popularQueries.stream()
                .filter(row -> {
                    String searchQuery = (String) row[0];
                    return searchQuery != null && searchQuery.toLowerCase().contains(query.toLowerCase());
                })
                .map(row -> {
                    String searchQuery = (String) row[0];
                    Long searchCount = ((Number) row[1]).longValue();
                    return convertToPopularSearchSuggestion(searchQuery, searchCount);
                })
                .collect(Collectors.toList());

        } catch (Exception e) {
            log.warn("Failed to get popular search suggestions", e);
            return List.of();
        }
    }

    private List<SuggestionResponse.Suggestion> getDestinationSuggestions(String query, String language, BigDecimal latitude, BigDecimal longitude, int limit) {
        try {
            List<PopularDestination> destinations;

            if (latitude != null && longitude != null) {
                // Get destinations near user's location
                destinations = popularDestinationRepository.findDestinationsNearLocation(
                    latitude.doubleValue(), longitude.doubleValue(), 100000.0 // 100km radius
                );
            } else {
                // Get popular destinations by name search
                destinations = popularDestinationRepository.findByDestinationNameContaining(query);
            }

            return destinations.stream()
                .limit(limit)
                .map(this::convertToDestinationSuggestion)
                .collect(Collectors.toList());

        } catch (Exception e) {
            log.warn("Failed to get destination suggestions", e);
            return List.of();
        }
    }

    private SuggestionResponse.Suggestion convertToLocationSuggestion(SearchIndex searchIndex) {
        SuggestionResponse.LocationDetails locationDetails = SuggestionResponse.LocationDetails.builder()
            .city(searchIndex.getCity())
            .countryCode(searchIndex.getCountryCode())
            .locationType("CITY")
            .coordinates(searchIndex.getLocation() != null && searchIndex.getLocation().getCoordinates() != null ?
                SuggestionResponse.GeoLocation.builder()
                    .latitude(BigDecimal.valueOf(searchIndex.getLocation().getCoordinates().getLat()))
                    .longitude(BigDecimal.valueOf(searchIndex.getLocation().getCoordinates().getLon()))
                    .build() : null)
            .build();

        SuggestionResponse.SuggestionDetails details = SuggestionResponse.SuggestionDetails.builder()
            .location(locationDetails)
            .build();

        return SuggestionResponse.Suggestion.builder()
            .text(searchIndex.getCity())
            .displayText(formatLocationDisplayText(searchIndex.getCity(), searchIndex.getCountryCode()))
            .type(SuggestionResponse.SuggestionType.LOCATION)
            .score(85.0f)
            .details(details)
            .isPopular(true)
            .build();
    }

    private SuggestionResponse.Suggestion convertToPropertySuggestion(SearchIndex searchIndex) {
        SuggestionResponse.PropertyDetails propertyDetails = SuggestionResponse.PropertyDetails.builder()
            .id(searchIndex.getPropertyId())
            .propertyType(searchIndex.getKind())
            .starRating(searchIndex.getStarRating())
            .location(SuggestionResponse.LocationInfo.builder()
                .city(searchIndex.getCity())
                .countryCode(searchIndex.getCountryCode())
                .build())
            .ratingAverage(searchIndex.getRatingAvg())
            .reviewCount(searchIndex.getRatingCount())
            .startingPrice(getLowestPrice(searchIndex))
            .currency("VND")
            .isFeatured(searchIndex.getSearchBoost() != null ?
                       searchIndex.getSearchBoost().getIsPromoted() : false)
            .build();

        SuggestionResponse.SuggestionDetails details = SuggestionResponse.SuggestionDetails.builder()
            .property(propertyDetails)
            .build();

        return SuggestionResponse.Suggestion.builder()
            .text(searchIndex.getName())
            .displayText(formatPropertyDisplayText(searchIndex.getName(), searchIndex.getCity()))
            .type(SuggestionResponse.SuggestionType.PROPERTY)
            .score(90.0f)
            .details(details)
            .isPopular(false)
            .build();
    }

    private SuggestionResponse.Suggestion convertToPopularSearchSuggestion(String searchQuery, Long searchCount) {
        float score = Math.min(95.0f, 70.0f + (searchCount.floatValue() / 100.0f));

        return SuggestionResponse.Suggestion.builder()
            .text(searchQuery)
            .displayText(searchQuery)
            .type(SuggestionResponse.SuggestionType.POPULAR_SEARCH)
            .score(score)
            .isPopular(true)
            .searchCount(searchCount.intValue())
            .build();
    }

    private SuggestionResponse.Suggestion convertToDestinationSuggestion(PopularDestination destination) {
        SuggestionResponse.LocationDetails locationDetails = SuggestionResponse.LocationDetails.builder()
            .city(destination.getDestinationName())
            .countryCode(destination.getCountryCode())
            .locationType(destination.getDestinationType().name())
            .coordinates(destination.getCoordinates() != null ?
                SuggestionResponse.GeoLocation.builder()
                    .latitude(BigDecimal.valueOf(destination.getCoordinates().getY()))
                    .longitude(BigDecimal.valueOf(destination.getCoordinates().getX()))
                    .build() : null)
            .propertyCount(destination.getSearchMetrics().getSearchVolume())
            .averagePrice(destination.getSearchMetrics().getAverageBookingValue())
            .build();

        SuggestionResponse.SuggestionDetails details = SuggestionResponse.SuggestionDetails.builder()
            .location(locationDetails)
            .build();

        float score = calculateDestinationScore(destination);

        return SuggestionResponse.Suggestion.builder()
            .text(destination.getDestinationName())
            .displayText(formatDestinationDisplayText(destination))
            .type(SuggestionResponse.SuggestionType.LOCATION)
            .score(score)
            .details(details)
            .isPopular(destination.getPopularityRank() != null && destination.getPopularityRank() <= 10)
            .build();
    }

    private BigDecimal getLowestPrice(SearchIndex searchIndex) {
        if (searchIndex.getRoomTypes() == null || searchIndex.getRoomTypes().isEmpty()) {
            return BigDecimal.ZERO;
        }

        return searchIndex.getRoomTypes().stream()
            .map(SearchIndex.RoomTypeData::getBasePrice)
            .filter(price -> price != null)
            .min(BigDecimal::compareTo)
            .orElse(BigDecimal.ZERO);
    }

    private String formatLocationDisplayText(String city, String countryCode) {
        return String.format("%s, %s", city, countryCode);
    }

    private String formatPropertyDisplayText(String propertyName, String city) {
        return String.format("%s - %s", propertyName, city);
    }

    private String formatDestinationDisplayText(PopularDestination destination) {
        String typeName = switch (destination.getDestinationType()) {
            case CITY -> "Thành phố";
            case LANDMARK -> "Địa danh";
            case REGION -> "Vùng";
            case AIRPORT -> "Sân bay";
            case DISTRICT -> "Quận/Huyện";
        };

        return String.format("%s (%s)", destination.getDestinationName(), typeName);
    }

    private float calculateDestinationScore(PopularDestination destination) {
        float baseScore = 80.0f;

        // Add score based on popularity rank
        if (destination.getPopularityRank() != null) {
            baseScore += Math.max(0, 20 - destination.getPopularityRank());
        }

        // Add score based on trending score
        if (destination.getTrendingScore() != null) {
            baseScore += Math.min(10, destination.getTrendingScore().floatValue() / 10);
        }

        // Add score based on search volume
        if (destination.getSearchMetrics().getSearchVolume() > 0) {
            baseScore += Math.min(5, destination.getSearchMetrics().getSearchVolume() / 200);
        }

        return Math.min(100.0f, baseScore);
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

    private SuggestionResponse createEmptyResponse(String query, String language) {
        SuggestionResponse.ResponseMetadata metadata = SuggestionResponse.ResponseMetadata.builder()
            .responseTimeMs(0L)
            .cacheHit(false)
            .language(language)
            .suggestionCount(0)
            .queryNormalized(normalizeVietnameseQuery(query))
            .autocompleteSessionId(UUID.randomUUID().toString())
            .build();

        return SuggestionResponse.builder()
            .suggestions(List.of())
            .responseMetadata(metadata)
            .build();
    }
}
