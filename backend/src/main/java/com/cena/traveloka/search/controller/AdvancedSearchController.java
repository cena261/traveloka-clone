package com.cena.traveloka.search.controller;

import com.cena.traveloka.search.dto.FacetedSearchRequest;
import com.cena.traveloka.search.dto.FacetedSearchResponse;
import com.cena.traveloka.search.service.AdvancedFilterService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search/advanced")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdvancedSearchController {

    AdvancedFilterService advancedFilterService;

    @PostMapping("/faceted")
    public ResponseEntity<FacetedSearchResponse> facetedSearch(
            @RequestBody @Valid FacetedSearchRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "score,desc") String sort) {

        log.info("Advanced faceted search request: query='{}', filters={}",
                request.getQuery(), countActiveFilters(request));

        try {
            // Validate advanced filters
            if (!advancedFilterService.validateAdvancedFilters(request)) {
                return ResponseEntity.badRequest().build();
            }

            // Normalize filters
            FacetedSearchRequest normalizedRequest = advancedFilterService.normalizeAdvancedFilters(request);

            // Add pagination
            Pageable pageable = createPageable(page, size, sort);
            normalizedRequest = normalizedRequest.toBuilder()
                .pageable(pageable)
                .build();

            // Execute faceted search
            FacetedSearchResponse response = advancedFilterService.executeFacetedSearch(normalizedRequest);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Advanced faceted search failed for query: '{}'", request.getQuery(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/facets/{category}")
    public ResponseEntity<Map<String, Object>> getFacetOptions(
            @PathVariable String category,
            @RequestParam(defaultValue = "vi") String language) {

        log.info("Getting facet options for category: '{}' in language: '{}'", category, language);

        try {
            // Validate category
            if (!isValidFacetCategory(category)) {
                return ResponseEntity.badRequest().build();
            }

            Map<String, Object> options = advancedFilterService.getFacetOptions(category, language);
            return ResponseEntity.ok(options);

        } catch (Exception e) {
            log.error("Failed to get facet options for category: '{}'", category, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/facets/dynamic")
    public ResponseEntity<Map<String, Object>> getDynamicFacets(
            @RequestBody @Valid FacetedSearchRequest baseRequest,
            @RequestParam String facetCategory) {

        log.info("Getting dynamic facets for category: '{}' with base filters", facetCategory);

        try {
            // This would execute a search to get dynamic facet counts
            // based on the current filter selection
            Map<String, Object> dynamicFacets = buildDynamicFacets(baseRequest, facetCategory);
            return ResponseEntity.ok(dynamicFacets);

        } catch (Exception e) {
            log.error("Failed to get dynamic facets for category: '{}'", facetCategory, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/filters/suggestions")
    public ResponseEntity<Map<String, Object>> getFilterSuggestions(
            @RequestParam String query,
            @RequestParam(defaultValue = "vi") String language,
            @RequestParam(defaultValue = "10") int limit) {

        log.info("Getting filter suggestions for query: '{}' in language: '{}'", query, language);

        try {
            // This would provide intelligent filter suggestions
            // based on the search query
            Map<String, Object> suggestions = buildFilterSuggestions(query, language, limit);
            return ResponseEntity.ok(suggestions);

        } catch (Exception e) {
            log.error("Failed to get filter suggestions for query: '{}'", query, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/filters/validate")
    public ResponseEntity<Map<String, Object>> validateFilterCombination(
            @RequestBody @Valid FacetedSearchRequest request) {

        log.info("Validating filter combination for advanced search");

        try {
            boolean isValid = advancedFilterService.validateAdvancedFilters(request);

            Map<String, Object> validation = Map.of(
                "valid", isValid,
                "warnings", getFilterWarnings(request),
                "suggestions", getFilterSuggestions(request),
                "estimatedResults", estimateResultCount(request)
            );

            return ResponseEntity.ok(validation);

        } catch (Exception e) {
            log.error("Failed to validate filter combination", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/filters/popular")
    public ResponseEntity<Map<String, Object>> getPopularFilterCombinations(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "vi") String language) {

        log.info("Getting popular filter combinations for last {} days", days);

        try {
            Map<String, Object> popularCombinations = getPopularCombinations(days, limit, language);
            return ResponseEntity.ok(popularCombinations);

        } catch (Exception e) {
            log.error("Failed to get popular filter combinations", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/search/explain")
    public ResponseEntity<Map<String, Object>> explainSearchResults(
            @RequestBody @Valid FacetedSearchRequest request,
            @RequestParam(required = false) String propertyId) {

        log.info("Explaining search results for query: '{}'", request.getQuery());

        try {
            Map<String, Object> explanation = buildSearchExplanation(request, propertyId);
            return ResponseEntity.ok(explanation);

        } catch (Exception e) {
            log.error("Failed to explain search results", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/filters/hierarchy/{category}")
    public ResponseEntity<Map<String, Object>> getHierarchicalFilters(
            @PathVariable String category,
            @RequestParam(defaultValue = "vi") String language,
            @RequestParam(required = false) String parentFilter) {

        log.info("Getting hierarchical filters for category: '{}', parent: '{}'", category, parentFilter);

        try {
            Map<String, Object> hierarchy = buildHierarchicalFilters(category, language, parentFilter);
            return ResponseEntity.ok(hierarchy);

        } catch (Exception e) {
            log.error("Failed to get hierarchical filters for category: '{}'", category, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // Helper methods

    private Pageable createPageable(int page, int size, String sort) {
        try {
            String[] sortParts = sort.split(",");
            String property = sortParts[0];
            Sort.Direction direction = sortParts.length > 1 && "desc".equals(sortParts[1]) ?
                Sort.Direction.DESC : Sort.Direction.ASC;

            return PageRequest.of(page, Math.min(size, 100), Sort.by(direction, property));

        } catch (Exception e) {
            log.warn("Invalid sort parameter: {}, using default", sort);
            return PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "score"));
        }
    }

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

    private boolean isValidFacetCategory(String category) {
        return List.of("location", "property", "price", "amenities", "reviews", "availability", "business")
            .contains(category.toLowerCase());
    }

    private Map<String, Object> buildDynamicFacets(FacetedSearchRequest baseRequest, String facetCategory) {
        // This would execute a search with the base filters to get current facet counts
        return Map.of(
            "category", facetCategory,
            "facets", List.of(),
            "totalResults", 0,
            "basedOnFilters", countActiveFilters(baseRequest)
        );
    }

    private Map<String, Object> buildFilterSuggestions(String query, String language, int limit) {
        // This would analyze the query and suggest relevant filters
        return Map.of(
            "query", query,
            "suggestions", List.of(
                Map.of("type", "location", "value", "Hà Nội", "confidence", 0.9),
                Map.of("type", "property", "value", "hotel", "confidence", 0.8)
            ),
            "totalSuggestions", 2
        );
    }

    private List<String> getFilterWarnings(FacetedSearchRequest request) {
        List<String> warnings = new ArrayList<>();

        // Check for potentially conflicting filters
        if (request.getPrice() != null && request.getPrice().getMinPrice() != null
            && request.getPrice().getMaxPrice() != null
            && request.getPrice().getMinPrice().compareTo(request.getPrice().getMaxPrice()) > 0) {
            warnings.add("Minimum price is greater than maximum price");
        }

        // Check for unrealistic filter combinations
        if (request.getAvailability() != null && request.getAvailability().getGuests() != null) {
            var guests = request.getAvailability().getGuests();
            if (guests.getAdults() != null && guests.getRooms() != null
                && guests.getAdults() > guests.getRooms() * 4) {
                warnings.add("Guest count may be too high for requested room count");
            }
        }

        return warnings;
    }

    private List<String> getFilterSuggestions(FacetedSearchRequest request) {
        List<String> suggestions = new ArrayList<>();

        // Suggest commonly added filters
        if (request.getLocation() == null) {
            suggestions.add("Consider adding a location filter to narrow results");
        }

        if (request.getPrice() == null) {
            suggestions.add("Set a price range to find properties in your budget");
        }

        if (request.getAvailability() == null) {
            suggestions.add("Add check-in dates to see real-time availability");
        }

        return suggestions;
    }

    private int estimateResultCount(FacetedSearchRequest request) {
        // This would estimate result count based on filters
        // In a real implementation, this might use cached aggregation data
        return 150; // Placeholder
    }

    private Map<String, Object> getPopularCombinations(int days, int limit, String language) {
        // This would query analytics data for popular filter combinations
        return Map.of(
            "period", days + " days",
            "combinations", List.of(
                Map.of("filters", Map.of("location", "Hà Nội", "starRating", "4+"), "count", 1250),
                Map.of("filters", Map.of("location", "Hồ Chí Minh", "price", "1M-2M"), "count", 980)
            ),
            "totalCombinations", 2
        );
    }

    private Map<String, Object> buildSearchExplanation(FacetedSearchRequest request, String propertyId) {
        // This would explain why certain results appear and their ranking
        return Map.of(
            "query", request.getQuery(),
            "matchingFactors", List.of("query_match", "location_proximity", "star_rating"),
            "appliedFilters", countActiveFilters(request),
            "scoringFactors", Map.of(
                "relevance", 0.4,
                "popularity", 0.3,
                "rating", 0.2,
                "distance", 0.1
            )
        );
    }

    private Map<String, Object> buildHierarchicalFilters(String category, String language, String parentFilter) {
        // This would build hierarchical filter structures
        return Map.of(
            "category", category,
            "parent", parentFilter,
            "children", List.of(
                Map.of("value", "child1", "label", "Child 1", "count", 50),
                Map.of("value", "child2", "label", "Child 2", "count", 30)
            ),
            "hasMore", false
        );
    }
}