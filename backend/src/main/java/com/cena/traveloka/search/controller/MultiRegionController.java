package com.cena.traveloka.search.controller;

import com.cena.traveloka.search.dto.*;
import com.cena.traveloka.search.service.MultiRegionSearchService;
import com.cena.traveloka.search.service.RegionConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/search/multi-region")
@RequiredArgsConstructor
@Slf4j
public class MultiRegionController {

    private final MultiRegionSearchService multiRegionSearchService;
    private final RegionConfigService regionConfigService;

    @PostMapping("/search")
    public ResponseEntity<MultiRegionSearchResponse> multiRegionSearch(
            @RequestBody @Valid MultiRegionSearchRequest request,
            HttpServletRequest httpRequest) {

        log.info("Multi-region search request: query={}, destination={}, regions={}",
                request.getQuery(),
                request.getDestination(),
                request.getRegionalPreferences() != null ? request.getRegionalPreferences().getPreferredRegions() : "auto");

        try {
            // Enrich request with user context if not provided
            enrichRequestWithUserContext(request, httpRequest);

            // Execute multi-region search
            MultiRegionSearchResponse response = multiRegionSearchService.executeMultiRegionSearch(request);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Invalid multi-region search request", e);
            return ResponseEntity.badRequest().build();

        } catch (Exception e) {
            log.error("Multi-region search failed", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/regions/optimal")
    public ResponseEntity<OptimalRegionsResponse> getOptimalRegions(
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) String userCountry,
            @RequestParam(required = false) BigDecimal latitude,
            @RequestParam(required = false) BigDecimal longitude,
            @RequestParam(required = false) List<String> preferredRegions,
            @RequestParam(required = false) List<String> excludedRegions) {

        log.info("Getting optimal regions for destination: {}, country: {}, coordinates: {},{}",
                destination, userCountry, latitude, longitude);

        try {
            // Build request for region determination
            MultiRegionSearchRequest request = MultiRegionSearchRequest.builder()
                    .destination(destination)
                    .regionalPreferences(MultiRegionSearchRequest.RegionalPreferences.builder()
                            .preferredRegions(preferredRegions)
                            .excludedRegions(excludedRegions)
                            .build())
                    .geographicConstraints(MultiRegionSearchRequest.GeographicConstraints.builder()
                            .centerLatitude(latitude)
                            .centerLongitude(longitude)
                            .build())
                    .userContext(MultiRegionSearchRequest.UserContext.builder()
                            .detectedCountry(userCountry)
                            .latitude(latitude)
                            .longitude(longitude)
                            .build())
                    .build();

            List<String> optimalRegions = multiRegionSearchService.getOptimalRegionsForRequest(request);

            OptimalRegionsResponse response = OptimalRegionsResponse.builder()
                    .recommendedRegions(optimalRegions)
                    .regionDetails(getRegionDetailsForRegions(optimalRegions))
                    .selectionCriteria(buildSelectionCriteria(request))
                    .timestamp(OffsetDateTime.now())
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to get optimal regions", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/regions/{regionId}/config")
    public ResponseEntity<RegionConfig> getRegionConfiguration(@PathVariable String regionId) {
        log.info("Getting region configuration for: {}", regionId);

        try {
            Optional<RegionConfig> regionConfig = regionConfigService.getRegionConfig(regionId);

            return regionConfig.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());

        } catch (Exception e) {
            log.error("Failed to get region configuration for: {}", regionId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/regions")
    public ResponseEntity<List<RegionSummary>> getAllRegions(
            @RequestParam(defaultValue = "false") boolean includeInactive) {

        log.info("Getting all regions, includeInactive: {}", includeInactive);

        try {
            List<RegionConfig> regions = includeInactive
                    ? regionConfigService.getAllActiveRegions() // In real implementation, get all regions
                    : regionConfigService.getAllActiveRegions();

            List<RegionSummary> regionSummaries = regions.stream()
                    .map(this::convertToRegionSummary)
                    .toList();

            return ResponseEntity.ok(regionSummaries);

        } catch (Exception e) {
            log.error("Failed to get regions list", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/regions/health")
    public ResponseEntity<RegionHealthResponse> getRegionsHealthStatus() {
        log.info("Getting region health status");

        try {
            Map<String, Integer> healthScores = regionConfigService.getRegionHealthScores();

            List<RegionHealthInfo> regionHealthInfos = healthScores.entrySet().stream()
                    .map(entry -> RegionHealthInfo.builder()
                            .regionId(entry.getKey())
                            .healthScore(entry.getValue())
                            .status(entry.getValue() >= 70 ? "HEALTHY" : entry.getValue() >= 50 ? "DEGRADED" : "UNHEALTHY")
                            .isHealthy(regionConfigService.isRegionHealthy(entry.getKey()))
                            .lastChecked(OffsetDateTime.now()) // In real implementation, get actual last check time
                            .build())
                    .toList();

            RegionHealthResponse response = RegionHealthResponse.builder()
                    .regionHealthInfos(regionHealthInfos)
                    .overallStatus(calculateOverallHealthStatus(regionHealthInfos))
                    .healthyRegionCount((int) regionHealthInfos.stream().filter(RegionHealthInfo::getIsHealthy).count())
                    .totalRegionCount(regionHealthInfos.size())
                    .lastUpdated(OffsetDateTime.now())
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to get region health status", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/regions/{regionId}/disable")
    public ResponseEntity<Void> disableRegion(
            @PathVariable String regionId,
            @RequestParam String reason) {

        log.info("Disabling region: {} - Reason: {}", regionId, reason);

        try {
            regionConfigService.disableRegion(regionId, reason);
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Failed to disable region: {}", regionId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/regions/{regionId}/enable")
    public ResponseEntity<Void> enableRegion(@PathVariable String regionId) {
        log.info("Enabling region: {}", regionId);

        try {
            regionConfigService.enableRegion(regionId);
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Failed to enable region: {}", regionId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/search/quality-assessment")
    public ResponseEntity<MultiRegionSearchResponse.SearchQualityMetrics> assessSearchQuality(
            @RequestBody MultiRegionSearchResponse response) {

        log.info("Assessing search quality for multi-region response");

        try {
            MultiRegionSearchResponse.SearchQualityMetrics qualityMetrics =
                    multiRegionSearchService.assessSearchQuality(response);

            return ResponseEntity.ok(qualityMetrics);

        } catch (Exception e) {
            log.error("Failed to assess search quality", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/regions/by-country/{countryCode}")
    public ResponseEntity<List<String>> getRegionsByCountry(@PathVariable String countryCode) {
        log.info("Getting regions for country: {}", countryCode);

        try {
            List<String> regions = regionConfigService.getRegionsForCountry(countryCode);
            return ResponseEntity.ok(regions);

        } catch (Exception e) {
            log.error("Failed to get regions for country: {}", countryCode, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/regions/by-bounds")
    public ResponseEntity<List<String>> getRegionsByBounds(
            @RequestParam BigDecimal north,
            @RequestParam BigDecimal south,
            @RequestParam BigDecimal east,
            @RequestParam BigDecimal west) {

        log.info("Getting regions within bounds: N{}, S{}, E{}, W{}", north, south, east, west);

        try {
            List<String> regions = regionConfigService.getRegionsByGeographicBounds(north, south, east, west);
            return ResponseEntity.ok(regions);

        } catch (Exception e) {
            log.error("Failed to get regions by bounds", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // Private helper methods

    private void enrichRequestWithUserContext(MultiRegionSearchRequest request, HttpServletRequest httpRequest) {
        if (request.getUserContext() == null) {
            request.setUserContext(MultiRegionSearchRequest.UserContext.builder().build());
        }

        MultiRegionSearchRequest.UserContext userContext = request.getUserContext();

        // Extract user context from HTTP request
        if (userContext.getIpAddress() == null) {
            userContext.setIpAddress(getClientIpAddress(httpRequest));
        }

        if (userContext.getUserAgent() == null) {
            userContext.setUserAgent(httpRequest.getHeader("User-Agent"));
        }

        // Set default values
        if (userContext.getLocationSource() == null) {
            userContext.setLocationSource("ip");
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    private List<RegionDetailInfo> getRegionDetailsForRegions(List<String> regionIds) {
        return regionIds.stream()
                .map(regionId -> regionConfigService.getRegionConfig(regionId))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(this::convertToRegionDetailInfo)
                .toList();
    }

    private RegionDetailInfo convertToRegionDetailInfo(RegionConfig config) {
        return RegionDetailInfo.builder()
                .regionId(config.getRegionId())
                .regionName(config.getRegionName())
                .regionCode(config.getRegionCode())
                .countryCodes(config.getCountryCodes())
                .primaryLanguage(config.getPrimaryLanguage())
                .primaryCurrency(config.getPrimaryCurrency())
                .isActive(config.getIsActive())
                .build();
    }

    private Map<String, Object> buildSelectionCriteria(MultiRegionSearchRequest request) {
        // Build selection criteria explanation
        return Map.of(
                "geographic_proximity", request.getGeographicConstraints() != null,
                "user_preferences", request.getRegionalPreferences() != null,
                "destination_based", request.getDestination() != null,
                "health_based", true
        );
    }

    private RegionSummary convertToRegionSummary(RegionConfig config) {
        return RegionSummary.builder()
                .regionId(config.getRegionId())
                .regionName(config.getRegionName())
                .regionCode(config.getRegionCode())
                .countryCodes(config.getCountryCodes())
                .primaryLanguage(config.getPrimaryLanguage())
                .primaryCurrency(config.getPrimaryCurrency())
                .isActive(config.getIsActive())
                .lastUpdated(config.getLastUpdated())
                .build();
    }

    private String calculateOverallHealthStatus(List<RegionHealthInfo> regionHealthInfos) {
        long healthyCount = regionHealthInfos.stream().filter(RegionHealthInfo::getIsHealthy).count();
        double healthyPercentage = (double) healthyCount / regionHealthInfos.size();

        if (healthyPercentage >= 0.9) return "EXCELLENT";
        if (healthyPercentage >= 0.7) return "GOOD";
        if (healthyPercentage >= 0.5) return "FAIR";
        return "POOR";
    }

    // Response DTOs

    @lombok.Data
    @lombok.Builder
    public static class OptimalRegionsResponse {
        private List<String> recommendedRegions;
        private List<RegionDetailInfo> regionDetails;
        private Map<String, Object> selectionCriteria;
        private OffsetDateTime timestamp;
    }

    @lombok.Data
    @lombok.Builder
    public static class RegionDetailInfo {
        private String regionId;
        private String regionName;
        private String regionCode;
        private List<String> countryCodes;
        private String primaryLanguage;
        private String primaryCurrency;
        private Boolean isActive;
    }

    @lombok.Data
    @lombok.Builder
    public static class RegionSummary {
        private String regionId;
        private String regionName;
        private String regionCode;
        private List<String> countryCodes;
        private String primaryLanguage;
        private String primaryCurrency;
        private Boolean isActive;
        private OffsetDateTime lastUpdated;
    }

    @lombok.Data
    @lombok.Builder
    public static class RegionHealthResponse {
        private List<RegionHealthInfo> regionHealthInfos;
        private String overallStatus;
        private Integer healthyRegionCount;
        private Integer totalRegionCount;
        private OffsetDateTime lastUpdated;
    }

    @lombok.Data
    @lombok.Builder
    public static class RegionHealthInfo {
        private String regionId;
        private Integer healthScore;
        private String status;
        private Boolean isHealthy;
        private OffsetDateTime lastChecked;
    }
}