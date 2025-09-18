package com.cena.traveloka.search.controller;

import com.cena.traveloka.search.dto.PropertySearchResponse;
import com.cena.traveloka.search.dto.SuggestionResponse;
import com.cena.traveloka.search.service.AutoCompleteService;
import com.cena.traveloka.search.service.LocationSearchService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/search/suggestions")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SuggestionController {

    AutoCompleteService autoCompleteService;
    LocationSearchService locationSearchService;

    @GetMapping("/autocomplete")
    public ResponseEntity<SuggestionResponse> getAutocompleteSuggestions(
            @RequestParam String query,
            @RequestParam(defaultValue = "vi") String language,
            @RequestParam(required = false) BigDecimal latitude,
            @RequestParam(required = false) BigDecimal longitude,
            @RequestParam(defaultValue = "10") Integer limit) {

        log.info("Autocomplete request: query='{}', language='{}', lat={}, lng={}",
                query, language, latitude, longitude);

        try {
            // Validate query
            if (query == null || query.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            // Limit query length for security
            if (query.length() > 100) {
                query = query.substring(0, 100);
            }

            SuggestionResponse response = autoCompleteService.getSuggestions(
                query.trim(), language, latitude, longitude, limit);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Autocomplete failed for query: '{}'", query, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/location")
    public ResponseEntity<PropertySearchResponse> searchNearLocation(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "10.0") Double radiusKm,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("Location search: lat={}, lng={}, radius={}km, query='{}'",
                latitude, longitude, radiusKm, query);

        try {
            // Validate coordinates
            if (latitude < -90 || latitude > 90) {
                return ResponseEntity.badRequest().build();
            }
            if (longitude < -180 || longitude > 180) {
                return ResponseEntity.badRequest().build();
            }
            if (radiusKm <= 0 || radiusKm > 100) {
                return ResponseEntity.badRequest().build();
            }

            Pageable pageable = PageRequest.of(page, Math.min(size, 100));

            PropertySearchResponse response = locationSearchService.searchNearLocation(
                latitude, longitude, radiusKm, query, pageable);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Location search failed for lat={}, lng={}", latitude, longitude, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/location/analytics")
    public ResponseEntity<Map<String, Object>> getLocationAnalytics(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "10.0") Double radiusKm) {

        try {
            // Validate coordinates
            if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
                return ResponseEntity.badRequest().build();
            }

            Map<String, Object> analytics = locationSearchService.getLocationAnalytics(
                latitude, longitude, radiusKm);

            return ResponseEntity.ok(analytics);

        } catch (Exception e) {
            log.error("Location analytics failed for lat={}, lng={}", latitude, longitude, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/city/{cityName}")
    public ResponseEntity<PropertySearchResponse> searchByCity(
            @PathVariable String cityName,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("City search: city='{}', query='{}'", cityName, query);

        try {
            // Validate city name
            if (cityName == null || cityName.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            Pageable pageable = PageRequest.of(page, Math.min(size, 100));

            PropertySearchResponse response = locationSearchService.searchByCity(
                cityName.trim(), query, pageable);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("City search failed for city: '{}'", cityName, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/destinations")
    public ResponseEntity<Map<String, Object>> getDestinationAnalytics(
            @RequestParam(defaultValue = "30") int days) {

        try {
            Map<String, Object> analytics = locationSearchService.getDestinationAnalytics(days);
            return ResponseEntity.ok(analytics);

        } catch (Exception e) {
            log.error("Failed to get destination analytics", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/popular-destinations")
    public ResponseEntity<?> getPopularDestinations(
            @RequestParam(defaultValue = "70.0") Double minTrendingScore,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        try {
            Pageable pageable = PageRequest.of(page, Math.min(size, 50));

            var destinations = locationSearchService.getPopularDestinations(
                BigDecimal.valueOf(minTrendingScore), pageable);

            return ResponseEntity.ok(destinations);

        } catch (Exception e) {
            log.error("Failed to get popular destinations", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/destinations/update-trends")
    public ResponseEntity<Void> updateDestinationTrends() {
        try {
            locationSearchService.updateDestinationTrendingScores();
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Failed to update destination trends", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/cleanup")
    public ResponseEntity<Void> cleanupOldData(
            @RequestParam(defaultValue = "365") int retentionDays) {

        try {
            if (retentionDays < 30) {
                return ResponseEntity.badRequest().build();
            }

            locationSearchService.cleanupOldAnalytics(retentionDays);
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Failed to cleanup old data", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
