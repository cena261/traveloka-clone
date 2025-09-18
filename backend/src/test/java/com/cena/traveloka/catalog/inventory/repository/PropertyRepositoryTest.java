package com.cena.traveloka.catalog.inventory.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.cena.traveloka.catalog.inventory.entity.Property;
import com.cena.traveloka.catalog.inventory.entity.Partner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T015: Repository test for PostGIS spatial queries
 * Tests custom repository methods with real PostGIS database
 * These tests MUST FAIL until repository implementation is complete
 */
@DataJpaTest
@ActiveProfiles("test")
@Testcontainers
class PropertyRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgis/postgis:16-3.4")
            .withDatabaseName("traveloka_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PropertyRepository propertyRepository;

    @Test
    void testFindPropertiesWithinRadius() {
        // This test MUST FAIL until Property entity and repository are implemented

        // Create test partner
        var partner = new Partner();
        partner.setName("Test Partner");
        partner.setEmail("test@partner.com");
        partner = entityManager.persistAndFlush(partner);

        // Create properties at different locations
        var centralProperty = new Property();
        centralProperty.setPartner(partner);
        centralProperty.setName("Central Hotel");
        centralProperty.setLatitude(13.7563);
        centralProperty.setLongitude(100.5018);
        entityManager.persistAndFlush(centralProperty);

        var nearbyProperty = new Property();
        nearbyProperty.setPartner(partner);
        nearbyProperty.setName("Nearby Hotel");
        nearbyProperty.setLatitude(13.7600);
        nearbyProperty.setLongitude(100.5050);
        entityManager.persistAndFlush(nearbyProperty);

        var farProperty = new Property();
        farProperty.setPartner(partner);
        farProperty.setName("Far Hotel");
        farProperty.setLatitude(13.8000);
        farProperty.setLongitude(100.6000);
        entityManager.persistAndFlush(farProperty);

        // Test spatial query within 5km radius
        var nearbyProperties = propertyRepository.findPropertiesWithinRadius(
                13.7563, 100.5018, 5000.0); // 5km in meters

        assertThat(nearbyProperties).hasSize(2);
        assertThat(nearbyProperties).extracting(Property::getName)
                .containsExactlyInAnyOrder("Central Hotel", "Nearby Hotel");

        // Test wider radius
        var allProperties = propertyRepository.findPropertiesWithinRadius(
                13.7563, 100.5018, 20000.0); // 20km in meters

        assertThat(allProperties).hasSize(3);
    }
}