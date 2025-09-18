package com.cena.traveloka.search.service;

import com.cena.traveloka.search.dto.RegionConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegionConfigService {

    @Value("${search.multiregion.default-region:us-east}")
    private String defaultRegion;

    @Value("${search.multiregion.enable-health-checks:true}")
    private boolean enableHealthChecks;

    @Value("${search.multiregion.health-check-interval:30000}")
    private long healthCheckInterval;

    private final Map<String, RegionConfig> regionConfigs = new ConcurrentHashMap<>();
    private final Map<String, OffsetDateTime> regionHealthStatus = new ConcurrentHashMap<>();
    private final Map<String, Integer> regionHealthScores = new ConcurrentHashMap<>();

    public void initializeRegionConfigs() {
        log.info("Initializing region configurations");

        try {
            // Load region configurations (in production, this would come from database/config service)
            loadRegionConfigurations();
            validateRegionConfigs();

            log.info("Successfully initialized {} region configurations", regionConfigs.size());

        } catch (Exception e) {
            log.error("Failed to initialize region configurations", e);
            throw new RuntimeException("Region configuration initialization failed", e);
        }
    }

    @Cacheable(value = "regionConfig", key = "#regionId")
    public Optional<RegionConfig> getRegionConfig(String regionId) {
        log.debug("Getting region configuration for: {}", regionId);
        return Optional.ofNullable(regionConfigs.get(regionId));
    }

    public List<RegionConfig> getAllActiveRegions() {
        log.debug("Getting all active region configurations");

        return regionConfigs.values().stream()
                .filter(RegionConfig::getIsActive)
                .collect(Collectors.toList());
    }

    public List<String> getRegionsForCountry(String countryCode) {
        log.debug("Getting regions for country: {}", countryCode);

        return regionConfigs.values().stream()
                .filter(config -> config.getCountryCodes().contains(countryCode))
                .filter(RegionConfig::getIsActive)
                .map(RegionConfig::getRegionId)
                .collect(Collectors.toList());
    }

    public String determineOptimalRegion(String userCountry, BigDecimal latitude, BigDecimal longitude) {
        log.debug("Determining optimal region for country: {}, coordinates: {},{}",
                userCountry, latitude, longitude);

        try {
            // Strategy 1: Direct country match
            List<String> regionsByCountry = getRegionsForCountry(userCountry);
            if (!regionsByCountry.isEmpty()) {
                String region = selectBestRegionByHealth(regionsByCountry);
                if (region != null) {
                    log.debug("Selected region by country match: {}", region);
                    return region;
                }
            }

            // Strategy 2: Geographic proximity
            if (latitude != null && longitude != null) {
                String nearestRegion = findNearestRegion(latitude, longitude);
                if (nearestRegion != null) {
                    log.debug("Selected region by proximity: {}", nearestRegion);
                    return nearestRegion;
                }
            }

            // Strategy 3: Default region with health check
            if (isRegionHealthy(defaultRegion)) {
                log.debug("Using healthy default region: {}", defaultRegion);
                return defaultRegion;
            }

            // Strategy 4: Any healthy region
            String anyHealthyRegion = getAnyHealthyRegion();
            if (anyHealthyRegion != null) {
                log.debug("Using any healthy region: {}", anyHealthyRegion);
                return anyHealthyRegion;
            }

            log.warn("No healthy regions available, using default: {}", defaultRegion);
            return defaultRegion;

        } catch (Exception e) {
            log.error("Failed to determine optimal region, using default", e);
            return defaultRegion;
        }
    }

    public List<String> getRegionsByGeographicBounds(BigDecimal north, BigDecimal south,
                                                    BigDecimal east, BigDecimal west) {
        log.debug("Getting regions within bounds: N{}, S{}, E{}, W{}", north, south, east, west);

        return regionConfigs.values().stream()
                .filter(RegionConfig::getIsActive)
                .filter(config -> {
                    RegionConfig.GeographicBounds bounds = config.getBounds();
                    if (bounds == null) return false;

                    return bounds.getNorthLat().compareTo(south) >= 0 &&
                           bounds.getSouthLat().compareTo(north) <= 0 &&
                           bounds.getEastLng().compareTo(west) >= 0 &&
                           bounds.getWestLng().compareTo(east) <= 0;
                })
                .map(RegionConfig::getRegionId)
                .collect(Collectors.toList());
    }

    @Scheduled(fixedRateString = "${search.multiregion.health-check-interval:30000}")
    public void performHealthChecks() {
        if (!enableHealthChecks) return;

        log.debug("Performing region health checks");

        List<CompletableFuture<Void>> healthCheckFutures = regionConfigs.keySet().stream()
                .map(this::checkRegionHealthAsync)
                .collect(Collectors.toList());

        CompletableFuture.allOf(healthCheckFutures.toArray(new CompletableFuture[0]))
                .thenRun(() -> log.debug("Completed health checks for all regions"))
                .exceptionally(throwable -> {
                    log.error("Failed to complete some health checks", throwable);
                    return null;
                });
    }

    public boolean isRegionHealthy(String regionId) {
        Integer healthScore = regionHealthScores.get(regionId);
        return healthScore != null && healthScore >= 70; // 70% threshold
    }

    public Map<String, Integer> getRegionHealthScores() {
        return new HashMap<>(regionHealthScores);
    }

    public void updateRegionConfig(String regionId, RegionConfig config) {
        log.info("Updating region configuration for: {}", regionId);

        try {
            validateRegionConfig(config);
            config.setLastUpdated(OffsetDateTime.now());
            regionConfigs.put(regionId, config);

            log.info("Successfully updated region configuration for: {}", regionId);

        } catch (Exception e) {
            log.error("Failed to update region configuration for: {}", regionId, e);
            throw new RuntimeException("Region configuration update failed", e);
        }
    }

    public void disableRegion(String regionId, String reason) {
        log.warn("Disabling region: {} - Reason: {}", regionId, reason);

        RegionConfig config = regionConfigs.get(regionId);
        if (config != null) {
            RegionConfig updatedConfig = config.toBuilder()
                    .isActive(false)
                    .lastUpdated(OffsetDateTime.now())
                    .build();
            regionConfigs.put(regionId, updatedConfig);
        }
    }

    public void enableRegion(String regionId) {
        log.info("Enabling region: {}", regionId);

        RegionConfig config = regionConfigs.get(regionId);
        if (config != null) {
            RegionConfig updatedConfig = config.toBuilder()
                    .isActive(true)
                    .lastUpdated(OffsetDateTime.now())
                    .build();
            regionConfigs.put(regionId, updatedConfig);
        }
    }

    // Private helper methods

    private void loadRegionConfigurations() {
        // Sample configurations - in production, load from database/config service

        regionConfigs.put("us-east", createUSEastConfig());
        regionConfigs.put("us-west", createUSWestConfig());
        regionConfigs.put("eu-central", createEUCentralConfig());
        regionConfigs.put("asia-pacific", createAsiaPacificConfig());
        regionConfigs.put("southeast-asia", createSoutheastAsiaConfig());

        log.debug("Loaded {} region configurations", regionConfigs.size());
    }

    private RegionConfig createUSEastConfig() {
        return RegionConfig.builder()
                .regionId("us-east")
                .regionName("US East")
                .regionCode("USE")
                .countryCodes(List.of("US", "CA"))
                .timeZones(List.of("America/New_York", "America/Toronto"))
                .primaryLanguage("en-US")
                .supportedLanguages(List.of("en-US", "es-US", "fr-CA"))
                .primaryCurrency("USD")
                .supportedCurrencies(List.of("USD", "CAD"))
                .bounds(RegionConfig.GeographicBounds.builder()
                        .northLat(BigDecimal.valueOf(60.0))
                        .southLat(BigDecimal.valueOf(25.0))
                        .eastLng(BigDecimal.valueOf(-60.0))
                        .westLng(BigDecimal.valueOf(-125.0))
                        .coordinateSystem("WGS84")
                        .zoomLevel(4)
                        .build())
                .searchConfig(RegionConfig.SearchEngineConfig.builder()
                        .primarySearchCluster("elasticsearch-us-east")
                        .fallbackClusters(List.of("elasticsearch-us-central"))
                        .indexPrefix("travel-us")
                        .maxResultsPerPage(50)
                        .defaultPageSize(20)
                        .queryTimeout(5000L)
                        .enableFacets(true)
                        .enablePersonalization(true)
                        .enabledFeatures(List.of("faceted_search", "personalization", "analytics"))
                        .build())
                .cacheConfig(RegionConfig.CacheConfig.builder()
                        .cacheCluster("redis-us-east")
                        .fallbackCaches(List.of("redis-us-central"))
                        .defaultTtlSeconds(3600)
                        .searchResultsTtl(1800)
                        .suggestionsTtl(7200)
                        .facetsTtl(3600)
                        .userProfilesTtl(86400)
                        .evictionPolicy("LRU")
                        .maxCacheSize(1000000)
                        .enableDistributedCache(true)
                        .build())
                .performanceConfig(RegionConfig.PerformanceConfig.builder()
                        .maxConcurrentRequests(1000)
                        .circuitBreakerThreshold(5)
                        .circuitBreakerTimeout(30000L)
                        .retryAttempts(3)
                        .retryDelay(1000L)
                        .loadBalancingWeight(BigDecimal.valueOf(1.0))
                        .routingStrategy("weighted")
                        .healthCheckInterval(30)
                        .build())
                .businessRules(RegionConfig.BusinessRuleConfig.builder()
                        .preferredPartners(List.of("marriott", "hilton", "ihg"))
                        .inventoryPriority("availability")
                        .enableDynamicPricing(true)
                        .enablePromotions(true)
                        .localPaymentMethods(List.of("visa", "mastercard", "amex", "paypal"))
                        .build())
                .compliance(RegionConfig.ComplianceConfig.builder()
                        .gdprCompliant(false)
                        .ccpaCompliant(true)
                        .dataRetentionPolicies(List.of("7_years_booking", "2_years_search"))
                        .requiresUserConsent(false)
                        .build())
                .isActive(true)
                .lastUpdated(OffsetDateTime.now())
                .build();
    }

    private RegionConfig createUSWestConfig() {
        return RegionConfig.builder()
                .regionId("us-west")
                .regionName("US West")
                .regionCode("USW")
                .countryCodes(List.of("US"))
                .timeZones(List.of("America/Los_Angeles", "America/Denver"))
                .primaryLanguage("en-US")
                .supportedLanguages(List.of("en-US", "es-US"))
                .primaryCurrency("USD")
                .supportedCurrencies(List.of("USD"))
                .bounds(RegionConfig.GeographicBounds.builder()
                        .northLat(BigDecimal.valueOf(49.0))
                        .southLat(BigDecimal.valueOf(32.0))
                        .eastLng(BigDecimal.valueOf(-102.0))
                        .westLng(BigDecimal.valueOf(-125.0))
                        .coordinateSystem("WGS84")
                        .zoomLevel(4)
                        .build())
                .isActive(true)
                .lastUpdated(OffsetDateTime.now())
                .build();
    }

    private RegionConfig createEUCentralConfig() {
        return RegionConfig.builder()
                .regionId("eu-central")
                .regionName("EU Central")
                .regionCode("EUC")
                .countryCodes(List.of("DE", "FR", "IT", "ES", "NL", "BE", "AT", "CH"))
                .timeZones(List.of("Europe/Berlin", "Europe/Paris", "Europe/Rome"))
                .primaryLanguage("en-GB")
                .supportedLanguages(List.of("en-GB", "de", "fr", "it", "es", "nl"))
                .primaryCurrency("EUR")
                .supportedCurrencies(List.of("EUR", "GBP", "CHF"))
                .bounds(RegionConfig.GeographicBounds.builder()
                        .northLat(BigDecimal.valueOf(55.0))
                        .southLat(BigDecimal.valueOf(35.0))
                        .eastLng(BigDecimal.valueOf(15.0))
                        .westLng(BigDecimal.valueOf(-5.0))
                        .coordinateSystem("WGS84")
                        .zoomLevel(4)
                        .build())
                .compliance(RegionConfig.ComplianceConfig.builder()
                        .gdprCompliant(true)
                        .ccpaCompliant(false)
                        .requiresUserConsent(true)
                        .build())
                .isActive(true)
                .lastUpdated(OffsetDateTime.now())
                .build();
    }

    private RegionConfig createAsiaPacificConfig() {
        return RegionConfig.builder()
                .regionId("asia-pacific")
                .regionName("Asia Pacific")
                .regionCode("APAC")
                .countryCodes(List.of("JP", "KR", "AU", "NZ", "SG", "HK"))
                .timeZones(List.of("Asia/Tokyo", "Asia/Seoul", "Australia/Sydney", "Asia/Singapore"))
                .primaryLanguage("en-US")
                .supportedLanguages(List.of("en-US", "ja", "ko", "zh-CN", "zh-TW"))
                .primaryCurrency("USD")
                .supportedCurrencies(List.of("USD", "JPY", "KRW", "AUD", "SGD", "HKD"))
                .bounds(RegionConfig.GeographicBounds.builder()
                        .northLat(BigDecimal.valueOf(45.0))
                        .southLat(BigDecimal.valueOf(-45.0))
                        .eastLng(BigDecimal.valueOf(180.0))
                        .westLng(BigDecimal.valueOf(100.0))
                        .coordinateSystem("WGS84")
                        .zoomLevel(3)
                        .build())
                .isActive(true)
                .lastUpdated(OffsetDateTime.now())
                .build();
    }

    private RegionConfig createSoutheastAsiaConfig() {
        return RegionConfig.builder()
                .regionId("southeast-asia")
                .regionName("Southeast Asia")
                .regionCode("SEA")
                .countryCodes(List.of("ID", "TH", "MY", "PH", "VN", "SG"))
                .timeZones(List.of("Asia/Jakarta", "Asia/Bangkok", "Asia/Kuala_Lumpur", "Asia/Manila"))
                .primaryLanguage("en-US")
                .supportedLanguages(List.of("en-US", "id", "th", "ms", "vi", "zh-CN"))
                .primaryCurrency("USD")
                .supportedCurrencies(List.of("USD", "IDR", "THB", "MYR", "PHP", "VND", "SGD"))
                .bounds(RegionConfig.GeographicBounds.builder()
                        .northLat(BigDecimal.valueOf(20.0))
                        .southLat(BigDecimal.valueOf(-10.0))
                        .eastLng(BigDecimal.valueOf(140.0))
                        .westLng(BigDecimal.valueOf(95.0))
                        .coordinateSystem("WGS84")
                        .zoomLevel(4)
                        .build())
                .isActive(true)
                .lastUpdated(OffsetDateTime.now())
                .build();
    }

    private void validateRegionConfigs() {
        for (Map.Entry<String, RegionConfig> entry : regionConfigs.entrySet()) {
            validateRegionConfig(entry.getValue());
        }
    }

    private void validateRegionConfig(RegionConfig config) {
        if (config.getRegionId() == null || config.getRegionId().trim().isEmpty()) {
            throw new IllegalArgumentException("Region ID cannot be null or empty");
        }

        if (config.getCountryCodes() == null || config.getCountryCodes().isEmpty()) {
            throw new IllegalArgumentException("Country codes cannot be null or empty for region: " + config.getRegionId());
        }

        if (config.getPrimaryCurrency() == null || config.getPrimaryCurrency().trim().isEmpty()) {
            throw new IllegalArgumentException("Primary currency cannot be null or empty for region: " + config.getRegionId());
        }
    }

    private String selectBestRegionByHealth(List<String> candidateRegions) {
        return candidateRegions.stream()
                .filter(this::isRegionHealthy)
                .max(Comparator.comparing(region -> regionHealthScores.getOrDefault(region, 0)))
                .orElse(null);
    }

    private String findNearestRegion(BigDecimal latitude, BigDecimal longitude) {
        return regionConfigs.values().stream()
                .filter(RegionConfig::getIsActive)
                .filter(config -> config.getBounds() != null)
                .min(Comparator.comparing(config -> calculateDistance(latitude, longitude, config.getBounds())))
                .map(RegionConfig::getRegionId)
                .orElse(null);
    }

    private double calculateDistance(BigDecimal lat1, BigDecimal lng1, RegionConfig.GeographicBounds bounds) {
        BigDecimal centerLat = bounds.getNorthLat().add(bounds.getSouthLat()).divide(BigDecimal.valueOf(2));
        BigDecimal centerLng = bounds.getEastLng().add(bounds.getWestLng()).divide(BigDecimal.valueOf(2));

        // Simplified distance calculation
        double deltaLat = lat1.subtract(centerLat).doubleValue();
        double deltaLng = lng1.subtract(centerLng).doubleValue();

        return Math.sqrt(deltaLat * deltaLat + deltaLng * deltaLng);
    }

    private String getAnyHealthyRegion() {
        return regionConfigs.keySet().stream()
                .filter(this::isRegionHealthy)
                .findFirst()
                .orElse(null);
    }

    private CompletableFuture<Void> checkRegionHealthAsync(String regionId) {
        return CompletableFuture.runAsync(() -> {
            try {
                int healthScore = performHealthCheck(regionId);
                regionHealthScores.put(regionId, healthScore);
                regionHealthStatus.put(regionId, OffsetDateTime.now());

                if (healthScore < 50) {
                    log.warn("Region {} has low health score: {}", regionId, healthScore);
                }

            } catch (Exception e) {
                log.error("Health check failed for region: {}", regionId, e);
                regionHealthScores.put(regionId, 0);
            }
        });
    }

    private int performHealthCheck(String regionId) {
        // In production, this would perform actual health checks
        // For now, return a simulated score

        RegionConfig config = regionConfigs.get(regionId);
        if (config == null || !config.getIsActive()) {
            return 0;
        }

        // Simulate health check score (70-100 for healthy regions)
        return 70 + (int) (Math.random() * 30);
    }
}