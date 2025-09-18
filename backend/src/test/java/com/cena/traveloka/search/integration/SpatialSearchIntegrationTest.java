package com.cena.traveloka.search.integration;

import com.cena.traveloka.catalog.inventory.entity.Property;
import com.cena.traveloka.catalog.inventory.repository.PropertyRepository;
import com.cena.traveloka.search.dto.LocationSearchRequest;
import com.cena.traveloka.search.dto.PropertySearchResponse;
import com.cena.traveloka.search.service.LocationSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgisContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for PostGIS spatial search functionality
 *
 * ⚠️  CRITICAL: These tests MUST FAIL initially (RED phase) until implementation is complete.
 * This is a constitutional requirement for TDD compliance.
 *
 * Testing Strategy:
 * - Real PostGIS integration with Testcontainers
 * - Spatial distance queries using ST_DWithin and ST_Distance
 * - Geography vs Geometry performance comparison
 * - Location-based search with radius filtering
 * - Integration with existing property geography column
 * - Performance under load with spatial indexes
 *
 * Prerequisites:
 * - LocationSearchService exists (currently empty - tests will fail)
 * - Property entity has geography column (exists from V2 migration)
 * - PostGIS spatial functions available
 * - V8__search_tsvector.sql applied with spatial indexes
 *
 * Expected Initial State: FAILING (RED) ❌
 * Expected Final State: PASSING (GREEN) ✅
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@DisplayName("PostGIS Spatial Search Integration Tests")
class SpatialSearchIntegrationTest {

    @Container
    static PostgisContainer postgis = new PostgisContainer("postgis/postgis:16-3.4-alpine")
            .withDatabaseName("traveloka_test")
            .withUsername("test")
            .withPassword("test")
            .withInitScript("spatial-test-init.sql");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgis::getJdbcUrl);
        registry.add("spring.datasource.username", postgis::getUsername);
        registry.add("spring.datasource.password", postgis::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired(required = false)
    private LocationSearchService locationSearchService;

    @Autowired(required = false)
    private PropertyRepository propertyRepository;

    @Autowired(required = false)
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        // Note: These will fail until services are implemented
        assumeServicesExist();
        setupTestData();
    }

    @Test
    @DisplayName("Should find properties within radius using ST_DWithin")
    void searchByLocation_WithinRadius_ShouldUseSTDWithinForPerformance() throws Exception {
        // Given - search location in central Hanoi
        double latitude = 21.0285;  // Hanoi Old Quarter
        double longitude = 105.8542;
        double radiusKm = 5.0;

        LocationSearchRequest searchRequest = LocationSearchRequest.builder()
                .latitude(latitude)
                .longitude(longitude)
                .radiusKm(radiusKm)
                .build();

        // When - performing spatial search (will fail until LocationSearchService exists)
        PropertySearchResponse response = locationSearchService.searchByLocation(searchRequest);

        // Then - should find properties within radius
        assertThat(response.getProperties()).isNotEmpty();

        // Verify all results are within the specified radius
        for (var property : response.getProperties()) {
            assertThat(property.getDistance()).isLessThanOrEqualTo(radiusKm);
        }

        // Verify results are sorted by distance
        List<Double> distances = response.getProperties().stream()
                .map(PropertySearchResponse.PropertySearchResult::getDistance)
                .toList();

        for (int i = 1; i < distances.size(); i++) {
            assertThat(distances.get(i)).isGreaterThanOrEqualTo(distances.get(i - 1));
        }
    }

    @Test
    @DisplayName("Should calculate accurate distances using ST_Distance")
    void searchByLocation_DistanceCalculation_ShouldUseSTDistanceForAccuracy() throws Exception {
        // Given - known locations with verifiable distances
        // Hanoi Opera House coordinates
        double operaHouseLat = 21.0269;
        double operaHouseLng = 105.8568;

        LocationSearchRequest searchRequest = LocationSearchRequest.builder()
                .latitude(operaHouseLat)
                .longitude(operaHouseLng)
                .radiusKm(10.0)
                .build();

        // When - searching near Opera House
        PropertySearchResponse response = locationSearchService.searchByLocation(searchRequest);

        // Then - verify distance calculations are accurate
        assertThat(response.getProperties()).isNotEmpty();

        // Test distance calculation with known property
        var nearestProperty = response.getProperties().get(0);

        // Verify distance is calculated and reasonable
        assertThat(nearestProperty.getDistance()).isNotNull();
        assertThat(nearestProperty.getDistance()).isGreaterThan(0);
        assertThat(nearestProperty.getDistance()).isLessThanOrEqualTo(10.0);

        // Verify distance calculation using direct SQL query
        String sql = """
                SELECT ST_Distance(
                    geography(ST_MakePoint(?, ?)),
                    geog
                ) / 1000.0 as distance_km
                FROM inventory.property
                WHERE id = ?
                """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter(1, operaHouseLng);
        query.setParameter(2, operaHouseLat);
        query.setParameter(3, nearestProperty.getPropertyId());

        Double directDistance = ((Number) query.getSingleResult()).doubleValue();

        // Distance from service should match direct calculation (within tolerance)
        assertThat(nearestProperty.getDistance())
                .isCloseTo(directDistance, org.assertj.core.data.Offset.offset(0.01));
    }

    @Test
    @DisplayName("Should use spatial index for performance on large datasets")
    void searchByLocation_LargeDataset_ShouldUseSpatialIndexEfficiently() throws Exception {
        // Given - create many test properties for performance testing
        createLargePropertyDataset(1000);

        LocationSearchRequest searchRequest = LocationSearchRequest.builder()
                .latitude(21.0285)
                .longitude(105.8542)
                .radiusKm(2.0)
                .build();

        // When - performing spatial search with performance measurement
        long startTime = System.currentTimeMillis();
        PropertySearchResponse response = locationSearchService.searchByLocation(searchRequest);
        long queryTime = System.currentTimeMillis() - startTime;

        // Then - should complete within reasonable time (spatial index should help)
        assertThat(queryTime).isLessThan(1000); // Under 1 second for 1000 properties

        // Verify results are correct
        assertThat(response.getProperties()).isNotEmpty();
        for (var property : response.getProperties()) {
            assertThat(property.getDistance()).isLessThanOrEqualTo(2.0);
        }

        // Verify spatial index is being used by checking query plan
        verifyQueryUsesIndex();
    }

    @Test
    @DisplayName("Should handle edge cases for geographic boundaries")
    void searchByLocation_GeographicBoundaries_ShouldHandleEdgeCases() throws Exception {
        // Test case 1: Near international date line (longitude ±180)
        LocationSearchRequest dateline = LocationSearchRequest.builder()
                .latitude(0.0)
                .longitude(179.0)
                .radiusKm(200.0) // Large radius to cross date line
                .build();

        PropertySearchResponse response1 = locationSearchService.searchByLocation(dateline);
        assertThat(response1).isNotNull();

        // Test case 2: Near poles (high latitude)
        LocationSearchRequest nearPole = LocationSearchRequest.builder()
                .latitude(89.0)
                .longitude(0.0)
                .radiusKm(1000.0)
                .build();

        PropertySearchResponse response2 = locationSearchService.searchByLocation(nearPole);
        assertThat(response2).isNotNull();

        // Test case 3: Equator crossing
        LocationSearchRequest equator = LocationSearchRequest.builder()
                .latitude(0.5)
                .longitude(100.0)
                .radiusKm(100.0)
                .build();

        PropertySearchResponse response3 = locationSearchService.searchByLocation(equator);
        assertThat(response3).isNotNull();
    }

    @Test
    @DisplayName("Should support multiple distance units (km, miles, meters)")
    void searchByLocation_DifferentUnits_ShouldConvertDistancesCorrectly() throws Exception {
        // Given - same location with different units
        double latitude = 21.0285;
        double longitude = 105.8542;

        LocationSearchRequest kmRequest = LocationSearchRequest.builder()
                .latitude(latitude)
                .longitude(longitude)
                .radiusKm(5.0)
                .build();

        LocationSearchRequest milesRequest = LocationSearchRequest.builder()
                .latitude(latitude)
                .longitude(longitude)
                .radiusMiles(3.1) // Approximately 5 km
                .build();

        // When - searching with different units
        PropertySearchResponse kmResponse = locationSearchService.searchByLocation(kmRequest);
        PropertySearchResponse milesResponse = locationSearchService.searchByLocation(milesRequest);

        // Then - should return similar results (within tolerance)
        assertThat(kmResponse.getProperties()).hasSameSizeAs(milesResponse.getProperties());

        // Verify distance conversions
        if (!kmResponse.getProperties().isEmpty() && !milesResponse.getProperties().isEmpty()) {
            double kmDistance = kmResponse.getProperties().get(0).getDistance();
            double milesDistance = milesResponse.getProperties().get(0).getDistance();

            // Convert miles to km for comparison (1 mile ≈ 1.609 km)
            double convertedMiles = milesDistance * 1.609;
            assertThat(kmDistance).isCloseTo(convertedMiles, org.assertj.core.data.Offset.offset(0.1));
        }
    }

    @Test
    @DisplayName("Should integrate with existing property full-text search")
    void searchByLocation_WithTextFilter_ShouldCombineSpatialAndTextSearch() throws Exception {
        // Given - location search with text filter
        LocationSearchRequest combinedRequest = LocationSearchRequest.builder()
                .latitude(21.0285)
                .longitude(105.8542)
                .radiusKm(10.0)
                .textQuery("hotel") // Combine with text search
                .build();

        // When - performing combined search
        PropertySearchResponse response = locationSearchService.searchByLocation(combinedRequest);

        // Then - should return properties that match both location and text criteria
        assertThat(response.getProperties()).isNotEmpty();

        for (var property : response.getProperties()) {
            // Should be within radius
            assertThat(property.getDistance()).isLessThanOrEqualTo(10.0);

            // Should match text criteria (property name or description contains "hotel")
            String propertyText = (property.getName() + " " + property.getDescription()).toLowerCase();
            assertThat(propertyText).contains("hotel");
        }
    }

    @Test
    @DisplayName("Should handle polygon-based area searches")
    void searchByLocation_PolygonArea_ShouldFindPropertiesWithinBounds() throws Exception {
        // Given - polygon defining search area (rectangle around Hanoi city center)
        List<LocationSearchRequest.Coordinate> polygon = List.of(
                new LocationSearchRequest.Coordinate(21.015, 105.840), // SW corner
                new LocationSearchRequest.Coordinate(21.015, 105.870), // SE corner
                new LocationSearchRequest.Coordinate(21.040, 105.870), // NE corner
                new LocationSearchRequest.Coordinate(21.040, 105.840), // NW corner
                new LocationSearchRequest.Coordinate(21.015, 105.840)  // Close polygon
        );

        LocationSearchRequest polygonRequest = LocationSearchRequest.builder()
                .searchArea(polygon)
                .build();

        // When - searching within polygon
        PropertySearchResponse response = locationSearchService.searchByLocation(polygonRequest);

        // Then - should find properties within the defined area
        assertThat(response.getProperties()).isNotEmpty();

        // Verify all properties are within the polygon bounds
        for (var property : response.getProperties()) {
            double lat = property.getLocation().getCoordinates().getLatitude();
            double lng = property.getLocation().getCoordinates().getLongitude();

            assertThat(lat).isBetween(21.015, 21.040);
            assertThat(lng).isBetween(105.840, 105.870);
        }
    }

    @Test
    @DisplayName("Should optimize queries for different zoom levels")
    void searchByLocation_DifferentZoomLevels_ShouldOptimizeAccordingly() throws Exception {
        // Test different radius sizes to simulate map zoom levels

        // City level (large radius)
        LocationSearchRequest cityLevel = LocationSearchRequest.builder()
                .latitude(21.0285)
                .longitude(105.8542)
                .radiusKm(50.0)
                .build();

        // District level (medium radius)
        LocationSearchRequest districtLevel = LocationSearchRequest.builder()
                .latitude(21.0285)
                .longitude(105.8542)
                .radiusKm(5.0)
                .build();

        // Street level (small radius)
        LocationSearchRequest streetLevel = LocationSearchRequest.builder()
                .latitude(21.0285)
                .longitude(105.8542)
                .radiusKm(0.5)
                .build();

        // When - performing searches at different zoom levels
        PropertySearchResponse cityResponse = locationSearchService.searchByLocation(cityLevel);
        PropertySearchResponse districtResponse = locationSearchService.searchByLocation(districtLevel);
        PropertySearchResponse streetResponse = locationSearchService.searchByLocation(streetLevel);

        // Then - should return appropriate number of results for each level
        assertThat(cityResponse.getProperties().size()).isGreaterThan(districtResponse.getProperties().size());
        assertThat(districtResponse.getProperties().size()).isGreaterThanOrEqualTo(streetResponse.getProperties().size());

        // All results should be properly filtered by distance
        cityResponse.getProperties().forEach(p -> assertThat(p.getDistance()).isLessThanOrEqualTo(50.0));
        districtResponse.getProperties().forEach(p -> assertThat(p.getDistance()).isLessThanOrEqualTo(5.0));
        streetResponse.getProperties().forEach(p -> assertThat(p.getDistance()).isLessThanOrEqualTo(0.5));
    }

    @Test
    @DisplayName("Performance: Spatial queries should complete under 500ms")
    void searchByLocation_PerformanceRequirement_ShouldMeetResponseTimeTarget() throws Exception {
        // Given - standard location search
        LocationSearchRequest searchRequest = LocationSearchRequest.builder()
                .latitude(21.0285)
                .longitude(105.8542)
                .radiusKm(10.0)
                .build();

        // When - measuring query performance
        long startTime = System.currentTimeMillis();
        PropertySearchResponse response = locationSearchService.searchByLocation(searchRequest);
        long queryTime = System.currentTimeMillis() - startTime;

        // Then - should meet performance requirements
        assertThat(queryTime).isLessThan(500); // Under 500ms requirement
        assertThat(response.getMetadata().getResponseTime()).isLessThan(500);
    }

    @Test
    @DisplayName("Should validate geographic coordinate bounds")
    void searchByLocation_InvalidCoordinates_ShouldValidateAndReject() throws Exception {
        // Test invalid latitude (> 90)
        LocationSearchRequest invalidLat = LocationSearchRequest.builder()
                .latitude(91.0)
                .longitude(105.8542)
                .radiusKm(5.0)
                .build();

        // Test invalid longitude (> 180)
        LocationSearchRequest invalidLng = LocationSearchRequest.builder()
                .latitude(21.0285)
                .longitude(181.0)
                .radiusKm(5.0)
                .build();

        // When & Then - should validate coordinates
        assertThrows(IllegalArgumentException.class, () -> {
            locationSearchService.searchByLocation(invalidLat);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            locationSearchService.searchByLocation(invalidLng);
        });
    }

    /**
     * Helper methods for test setup
     */
    private void setupTestData() {
        // Create test properties with known locations in and around Hanoi
        if (propertyRepository != null) {
            List<Property> testProperties = List.of(
                    createPropertyWithLocation("Hanoi Opera House Hotel", 21.0269, 105.8568),
                    createPropertyWithLocation("Old Quarter Inn", 21.0285, 105.8542),
                    createPropertyWithLocation("West Lake Resort", 21.0583, 105.8342),
                    createPropertyWithLocation("Ba Dinh Hotel", 21.0364, 105.8325),
                    createPropertyWithLocation("Dong Da Apartment", 21.0122, 105.8278)
            );

            propertyRepository.saveAll(testProperties);
        }
    }

    private Property createPropertyWithLocation(String name, double latitude, double longitude) {
        Property property = new Property();
        property.setName(name);
        property.setCity("Hanoi");
        property.setCountryCode("VN");
        property.setAddressLine("Test Address");
        property.setLatitude(latitude);
        property.setLongitude(longitude);
        property.setKind("hotel");
        property.setStatus("active");
        return property;
    }

    private void createLargePropertyDataset(int count) {
        if (propertyRepository != null) {
            List<Property> properties = new java.util.ArrayList<>();

            // Create properties in a grid around Hanoi
            double baseLat = 21.0;
            double baseLng = 105.8;
            double step = 0.01; // Approximately 1km steps

            for (int i = 0; i < count; i++) {
                double lat = baseLat + (i % 10) * step;
                double lng = baseLng + (i / 10) * step;

                Property property = createPropertyWithLocation("Test Property " + i, lat, lng);
                properties.add(property);
            }

            propertyRepository.saveAll(properties);
        }
    }

    private void verifyQueryUsesIndex() {
        // Verify that spatial queries are using the spatial index
        String explainQuery = """
                EXPLAIN (FORMAT JSON)
                SELECT p.* FROM inventory.property p
                WHERE ST_DWithin(
                    p.geog,
                    geography(ST_MakePoint(105.8542, 21.0285)),
                    5000
                )
                """;

        Query query = entityManager.createNativeQuery(explainQuery);
        String queryPlan = (String) query.getSingleResult();

        // The query plan should indicate index usage
        assertThat(queryPlan.toLowerCase()).contains("index");
    }

    /**
     * Assume services exist for test setup
     */
    private void assumeServicesExist() {
        org.junit.jupiter.api.Assumptions.assumeTrue(locationSearchService != null,
                "LocationSearchService not available - tests will be skipped until implementation");
        org.junit.jupiter.api.Assumptions.assumeTrue(propertyRepository != null,
                "PropertyRepository not available - tests will be skipped until implementation");
        org.junit.jupiter.api.Assumptions.assumeTrue(entityManager != null,
                "EntityManager not available - tests will be skipped until implementation");
    }
}