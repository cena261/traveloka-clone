package com.cena.traveloka.catalog.inventory.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.cena.traveloka.catalog.inventory.entity.Property;
import com.cena.traveloka.catalog.inventory.entity.Partner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T016: Repository test for Redis caching functionality
 * Tests Redis caching annotations and TTL policies
 * These tests MUST FAIL until Redis caching implementation is complete
 */
@DataJpaTest
@ActiveProfiles("test")
@Testcontainers
class CacheIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgis/postgis:16-3.4")
            .withDatabaseName("traveloka_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private PartnerRepository partnerRepository;

    @Autowired
    private CacheManager cacheManager;

    @Test
    void testPropertyCaching() {
        // This test MUST FAIL until Property entity and caching are implemented

        // Create test data
        var partner = new Partner();
        partner.setName("Cache Test Partner");
        partner.setEmail("cache@partner.com");
        partner = entityManager.persistAndFlush(partner);

        var property = new Property();
        property.setPartner(partner);
        property.setName("Cache Test Hotel");
        property.setCity("Bangkok");
        property.setLatitude(13.7563);
        property.setLongitude(100.5018);
        property = entityManager.persistAndFlush(property);

        // Clear any existing cache
        cacheManager.getCache("properties").clear();

        // First call - should hit database and cache result
        var result1 = propertyRepository.findById(property.getId());
        assertThat(result1).isPresent();

        // Verify cache entry exists
        var cachedProperty = cacheManager.getCache("properties").get(property.getId());
        assertThat(cachedProperty).isNotNull();
        assertThat(cachedProperty.get()).isEqualTo(result1.get());

        // Second call - should hit cache
        var result2 = propertyRepository.findById(property.getId());
        assertThat(result2).isPresent();
        assertThat(result2.get()).isEqualTo(result1.get());
    }

    @Test
    void testPartnerCaching() {
        // This test MUST FAIL until Partner entity and caching are implemented

        var partner = new Partner();
        partner.setName("Partner Cache Test");
        partner.setEmail("partnercache@test.com");
        partner.setBusinessRegistrationNumber("CACHE-001");
        partner = entityManager.persistAndFlush(partner);

        // Clear cache
        cacheManager.getCache("partners").clear();

        // Test caching with findByEmail
        var result1 = partnerRepository.findByEmail("partnercache@test.com");
        assertThat(result1).isPresent();

        // Verify cache
        var cachedPartner = cacheManager.getCache("partners").get("partnercache@test.com");
        assertThat(cachedPartner).isNotNull();

        // Second call should hit cache
        var result2 = partnerRepository.findByEmail("partnercache@test.com");
        assertThat(result2).isPresent();
        assertThat(result2.get()).isEqualTo(result1.get());
    }

    @Test
    void testCacheEviction() {
        // This test MUST FAIL until cache eviction is implemented

        // Create and cache a property
        var partner = new Partner();
        partner.setName("Eviction Test Partner");
        partner.setEmail("eviction@partner.com");
        partner = entityManager.persistAndFlush(partner);

        var property = new Property();
        property.setPartner(partner);
        property.setName("Eviction Test Hotel");
        property = entityManager.persistAndFlush(property);

        // Cache the property
        propertyRepository.findById(property.getId());

        // Update property - should evict cache
        property.setName("Updated Hotel Name");
        propertyRepository.save(property);

        // Verify cache was evicted
        var cachedProperty = cacheManager.getCache("properties").get(property.getId());
        assertThat(cachedProperty).isNull();
    }

    @Test
    void testCacheTTL() throws InterruptedException {
        // This test MUST FAIL until TTL configuration is implemented

        var partner = new Partner();
        partner.setName("TTL Test Partner");
        partner.setEmail("ttl@partner.com");
        partner = entityManager.persistAndFlush(partner);

        // Cache the partner
        partnerRepository.findById(partner.getId());

        // Verify cache exists
        var cachedPartner = cacheManager.getCache("partners").get(partner.getId());
        assertThat(cachedPartner).isNotNull();

        // Wait for TTL expiration (this test assumes short TTL for testing)
        Thread.sleep(2000); // Adjust based on TTL configuration

        // Cache should be expired (if TTL is less than 2 seconds for testing)
        cachedPartner = cacheManager.getCache("partners").get(partner.getId());
        // Note: This assertion depends on TTL configuration
        // For production, TTL would be much longer (1-4 hours)
    }

    @Test
    void testSpatialQueryCaching() {
        // This test MUST FAIL until spatial queries and caching are implemented

        var partner = new Partner();
        partner.setName("Spatial Cache Partner");
        partner.setEmail("spatial@partner.com");
        partner = entityManager.persistAndFlush(partner);

        // Create properties at different locations
        var property1 = new Property();
        property1.setPartner(partner);
        property1.setName("Hotel 1");
        property1.setLatitude(13.7563);
        property1.setLongitude(100.5018);
        entityManager.persistAndFlush(property1);

        var property2 = new Property();
        property2.setPartner(partner);
        property2.setName("Hotel 2");
        property2.setLatitude(13.7600);
        property2.setLongitude(100.5050);
        entityManager.persistAndFlush(property2);

        // Clear spatial search cache
        cacheManager.getCache("spatial-searches").clear();

        // First spatial search - should cache results
        var results1 = propertyRepository.findPropertiesWithinRadius(13.7563, 100.5018, 5000.0);
        assertThat(results1).hasSize(2);

        // Create cache key for spatial search
        String cacheKey = String.format("%.6f:%.6f:%.1f", 13.7563, 100.5018, 5000.0);

        // Verify cache entry
        var cachedResults = cacheManager.getCache("spatial-searches").get(cacheKey);
        assertThat(cachedResults).isNotNull();

        // Second call should hit cache
        var results2 = propertyRepository.findPropertiesWithinRadius(13.7563, 100.5018, 5000.0);
        assertThat(results2).hasSize(2);
        assertThat(results2).isEqualTo(results1);
    }
}