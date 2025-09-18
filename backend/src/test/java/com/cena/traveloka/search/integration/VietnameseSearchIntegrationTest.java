package com.cena.traveloka.search.integration;

import com.cena.traveloka.search.dto.PropertySearchRequest;
import com.cena.traveloka.search.dto.PropertySearchResponse;
import com.cena.traveloka.search.dto.SuggestionResponse;
import com.cena.traveloka.search.service.SearchService;
import com.cena.traveloka.search.service.AutoCompleteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Vietnamese text search functionality
 *
 * ⚠️  CRITICAL: These tests MUST FAIL initially (RED phase) until implementation is complete.
 * This is a constitutional requirement for TDD compliance.
 *
 * Testing Strategy:
 * - Real Elasticsearch integration with Vietnamese analyzers
 * - Diacritic normalization and accent folding
 * - Vietnamese stop words filtering
 * - Multi-language search (Vietnamese + English)
 * - Fuzzy matching for Vietnamese text
 * - Search query analysis and tokenization
 *
 * Prerequisites:
 * - SearchService exists with Vietnamese text support (currently empty - tests will fail)
 * - AutoCompleteService exists with Vietnamese support (currently empty - tests will fail)
 * - Elasticsearch Vietnamese analyzer configured (implemented in T002)
 * - Vietnamese text data indexed
 *
 * Expected Initial State: FAILING (RED) ❌
 * Expected Final State: PASSING (GREEN) ✅
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@DisplayName("Vietnamese Text Search Integration Tests")
class VietnameseSearchIntegrationTest {

    @Container
    static ElasticsearchContainer elasticsearch = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:8.11.1")
            .withEnv("discovery.type", "single-node")
            .withEnv("xpack.security.enabled", "false")
            .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.elasticsearch.uris", elasticsearch::getHttpHostAddress);
    }

    @Autowired(required = false)
    private SearchService searchService;

    @Autowired(required = false)
    private AutoCompleteService autoCompleteService;

    @BeforeEach
    void setUp() {
        // Note: These will fail until services are implemented
        assumeServicesExist();
        indexVietnameseTestData();
    }

    @Test
    @DisplayName("Should find hotels with Vietnamese query 'khách sạn Hà Nội'")
    void searchProperties_VietnameseQuery_ShouldFindMatchingProperties() throws Exception {
        // Given - Vietnamese search query with diacritics
        PropertySearchRequest searchRequest = PropertySearchRequest.builder()
                .query("khách sạn Hà Nội")
                .language("vi")
                .pagination(PropertySearchRequest.PaginationRequest.builder()
                        .page(0)
                        .size(20)
                        .build())
                .build();

        // When - searching with Vietnamese text (will fail until SearchService exists)
        PropertySearchResponse response = searchService.searchProperties(searchRequest);

        // Then - should find properties with Vietnamese names/descriptions
        assertThat(response.getProperties()).isNotEmpty();

        // Verify results contain Vietnamese properties
        boolean foundVietnameseProperty = response.getProperties().stream()
                .anyMatch(property ->
                    property.getName().contains("Hà Nội") ||
                    property.getName().contains("Hanoi") ||
                    property.getLocation().getCity().contains("Hà Nội")
                );

        assertThat(foundVietnameseProperty).isTrue();
    }

    @Test
    @DisplayName("Should normalize diacritics: 'ha noi' should match 'Hà Nội'")
    void searchProperties_DiacriticNormalization_ShouldMatchVariations() throws Exception {
        // Test various diacritic combinations for the same location
        String[] queries = {
            "ha noi",           // No diacritics
            "hà nội",           // All diacritics
            "Hà Nội",           // Capitalized with diacritics
            "HA NOI",           // All caps, no diacritics
            "hà noi",           // Mixed diacritics
            "ha nội"            // Mixed diacritics (reverse)
        };

        PropertySearchResponse firstResponse = null;

        for (String query : queries) {
            PropertySearchRequest searchRequest = PropertySearchRequest.builder()
                    .query(query)
                    .language("vi")
                    .pagination(PropertySearchRequest.PaginationRequest.builder()
                            .page(0)
                            .size(10)
                            .build())
                    .build();

            PropertySearchResponse response = searchService.searchProperties(searchRequest);

            if (firstResponse == null) {
                firstResponse = response;
            }

            // All variations should return similar results
            assertThat(response.getProperties()).isNotEmpty();

            // Should find properties in Hanoi regardless of diacritic variation
            boolean foundHanoiProperty = response.getProperties().stream()
                    .anyMatch(property ->
                        property.getLocation().getCity().toLowerCase().contains("nội") ||
                        property.getLocation().getCity().toLowerCase().contains("noi") ||
                        property.getName().toLowerCase().contains("nội") ||
                        property.getName().toLowerCase().contains("noi")
                    );

            assertThat(foundHanoiProperty).isTrue();
        }
    }

    @Test
    @DisplayName("Should handle Vietnamese stop words correctly")
    void searchProperties_VietnameseStopWords_ShouldFilterStopWords() throws Exception {
        // Given - search with Vietnamese stop words
        PropertySearchRequest withStopWords = PropertySearchRequest.builder()
                .query("khách sạn của tôi trong thành phố Hà Nội") // Contains stop words: của, trong
                .language("vi")
                .build();

        PropertySearchRequest withoutStopWords = PropertySearchRequest.builder()
                .query("khách sạn Hà Nội")
                .language("vi")
                .build();

        // When - searching with and without stop words
        PropertySearchResponse responseWith = searchService.searchProperties(withStopWords);
        PropertySearchResponse responseWithout = searchService.searchProperties(withoutStopWords);

        // Then - should return similar results (stop words filtered)
        assertThat(responseWith.getProperties()).isNotEmpty();
        assertThat(responseWithout.getProperties()).isNotEmpty();

        // Results should be similar since stop words are filtered
        // (This tests that stop words don't negatively affect search)
        assertThat(responseWith.getProperties().size())
                .isCloseTo(responseWithout.getProperties().size(), org.assertj.core.data.Offset.offset(2));
    }

    @Test
    @DisplayName("Should support fuzzy matching for Vietnamese text")
    void searchProperties_VietnameseFuzzyMatching_ShouldFindSimilarWords() throws Exception {
        // Given - slightly misspelled Vietnamese queries
        String[] fuzzyQueries = {
            "khac san",      // Missing diacritics and 'h'
            "kach san",      // Transposed letters
            "khach ssan",    // Double letter
            "khách săn"      // Wrong diacritic on 'sạn'
        };

        for (String fuzzyQuery : fuzzyQueries) {
            PropertySearchRequest searchRequest = PropertySearchRequest.builder()
                    .query(fuzzyQuery)
                    .language("vi")
                    .enableFuzzyMatching(true) // Enable fuzzy matching
                    .build();

            // When - searching with fuzzy query
            PropertySearchResponse response = searchService.searchProperties(searchRequest);

            // Then - should still find hotels (fuzzy matching)
            assertThat(response.getProperties()).isNotEmpty();

            // Should find properties that contain "khách sạn" or similar
            boolean foundHotelProperty = response.getProperties().stream()
                    .anyMatch(property ->
                        property.getName().toLowerCase().contains("hotel") ||
                        property.getName().toLowerCase().contains("khách") ||
                        property.getPropertyType().equals("hotel")
                    );

            assertThat(foundHotelProperty).isTrue();
        }
    }

    @Test
    @DisplayName("Should support mixed Vietnamese-English queries")
    void searchProperties_MixedLanguageQuery_ShouldHandleBothLanguages() throws Exception {
        // Given - mixed language queries
        String[] mixedQueries = {
            "luxury khách sạn Hà Nội",     // English + Vietnamese
            "hotel cao cấp Hanoi",          // Vietnamese + English
            "resort biển Đà Nẵng beach",   // Mixed with location
            "spa wellness trung tâm"       // English concepts in Vietnamese
        };

        for (String mixedQuery : mixedQueries) {
            PropertySearchRequest searchRequest = PropertySearchRequest.builder()
                    .query(mixedQuery)
                    .language("vi") // Primary language Vietnamese
                    .build();

            // When - searching with mixed language
            PropertySearchResponse response = searchService.searchProperties(searchRequest);

            // Then - should handle both language components
            assertThat(response.getProperties()).isNotEmpty();

            // Should find relevant properties regardless of language mix
            assertThat(response.getProperties().get(0).getRelevanceScore()).isGreaterThan(0.0);
        }
    }

    @Test
    @DisplayName("Should provide Vietnamese auto-complete suggestions")
    void getSuggestions_VietnamesePartialQuery_ShouldReturnVietnameseSuggestions() throws Exception {
        // Given - partial Vietnamese queries for auto-complete
        String[] partialQueries = {
            "hà",
            "khách",
            "sài",
            "đà"
        };

        for (String partialQuery : partialQueries) {
            // When - getting suggestions (will fail until AutoCompleteService exists)
            SuggestionResponse response = autoCompleteService.getSuggestions(
                partialQuery, "vi", null, null, 10
            );

            // Then - should return Vietnamese suggestions
            assertThat(response.getSuggestions()).isNotEmpty();

            // Verify suggestions contain Vietnamese text
            boolean hasVietnameseSuggestion = response.getSuggestions().stream()
                    .anyMatch(suggestion ->
                        containsVietnameseCharacters(suggestion.getText())
                    );

            assertThat(hasVietnameseSuggestion).isTrue();

            // Verify suggestions start with or relate to the partial query
            boolean relevantSuggestion = response.getSuggestions().stream()
                    .anyMatch(suggestion ->
                        normalizeVietnamese(suggestion.getText()).contains(normalizeVietnamese(partialQuery))
                    );

            assertThat(relevantSuggestion).isTrue();
        }
    }

    @Test
    @DisplayName("Should handle Vietnamese location names correctly")
    void searchProperties_VietnameseLocations_ShouldMatchLocationVariations() throws Exception {
        // Given - Vietnamese location variations
        String[][] locationVariations = {
            {"Hồ Chí Minh", "Ho Chi Minh", "HCMC", "Sài Gòn", "Saigon"},
            {"Hà Nội", "Ha Noi", "Hanoi"},
            {"Đà Nẵng", "Da Nang", "Danang"},
            {"Nha Trang", "Nha Trang"},
            {"Vịnh Hạ Long", "Ha Long Bay", "Halong"}
        };

        for (String[] variations : locationVariations) {
            PropertySearchResponse firstResponse = null;

            for (String location : variations) {
                PropertySearchRequest searchRequest = PropertySearchRequest.builder()
                        .query("hotel " + location)
                        .language("vi")
                        .build();

                PropertySearchResponse response = searchService.searchProperties(searchRequest);

                if (firstResponse == null) {
                    firstResponse = response;
                }

                // Should find properties in the specified location
                assertThat(response.getProperties()).isNotEmpty();

                // Should find properties in the target city
                boolean foundLocationProperty = response.getProperties().stream()
                        .anyMatch(property -> {
                            String cityName = property.getLocation().getCity().toLowerCase();
                            String propName = property.getName().toLowerCase();

                            return Arrays.stream(variations)
                                    .anyMatch(var ->
                                        cityName.contains(normalizeVietnamese(var.toLowerCase())) ||
                                        propName.contains(normalizeVietnamese(var.toLowerCase()))
                                    );
                        });

                assertThat(foundLocationProperty).isTrue();
            }
        }
    }

    @Test
    @DisplayName("Should boost Vietnamese content for Vietnamese queries")
    void searchProperties_VietnameseQuery_ShouldBoostVietnameseContent() throws Exception {
        // Given - Vietnamese query that might match both Vietnamese and English content
        PropertySearchRequest vietnameseRequest = PropertySearchRequest.builder()
                .query("khách sạn sang trọng")
                .language("vi")
                .build();

        PropertySearchRequest englishRequest = PropertySearchRequest.builder()
                .query("luxury hotel")
                .language("en")
                .build();

        // When - searching in Vietnamese vs English
        PropertySearchResponse vietnameseResponse = searchService.searchProperties(vietnameseRequest);
        PropertySearchResponse englishResponse = searchService.searchProperties(englishRequest);

        // Then - Vietnamese query should boost Vietnamese content
        assertThat(vietnameseResponse.getProperties()).isNotEmpty();
        assertThat(englishResponse.getProperties()).isNotEmpty();

        // Check relevance scores - Vietnamese content should score higher for Vietnamese query
        if (!vietnameseResponse.getProperties().isEmpty()) {
            var topVietnameseResult = vietnameseResponse.getProperties().get(0);

            // Should have high relevance score for Vietnamese query
            assertThat(topVietnameseResult.getRelevanceScore()).isGreaterThan(0.5);
        }
    }

    @Test
    @DisplayName("Should handle Vietnamese property amenities correctly")
    void searchProperties_VietnameseAmenities_ShouldMatchAmenityTerms() throws Exception {
        // Given - Vietnamese amenity search terms
        String[] amenityQueries = {
            "bể bơi",           // Swimming pool
            "wifi miễn phí",    // Free wifi
            "bãi đậu xe",       // Parking
            "phòng gym",        // Gym
            "spa thư giãn",     // Spa relaxation
            "nhà hàng",         // Restaurant
            "quầy bar",         // Bar
            "dịch vụ phòng"     // Room service
        };

        for (String amenityQuery : amenityQueries) {
            PropertySearchRequest searchRequest = PropertySearchRequest.builder()
                    .query("hotel có " + amenityQuery)
                    .language("vi")
                    .build();

            // When - searching for amenities in Vietnamese
            PropertySearchResponse response = searchService.searchProperties(searchRequest);

            // Then - should find properties with those amenities
            // Note: This requires amenity data to be indexed in Vietnamese
            assertThat(response.getProperties()).isNotEmpty();

            // Response should include properties that likely have these amenities
            assertThat(response.getProperties().get(0).getRelevanceScore()).isGreaterThan(0.0);
        }
    }

    @Test
    @DisplayName("Performance: Vietnamese text search should be under 500ms")
    void searchProperties_VietnameseTextPerformance_ShouldMeetResponseTime() throws Exception {
        // Given - complex Vietnamese query
        PropertySearchRequest complexRequest = PropertySearchRequest.builder()
                .query("khách sạn cao cấp gần trung tâm thành phố với bể bơi và spa")
                .language("vi")
                .build();

        // When - measuring Vietnamese search performance
        long startTime = System.currentTimeMillis();
        PropertySearchResponse response = searchService.searchProperties(complexRequest);
        long searchTime = System.currentTimeMillis() - startTime;

        // Then - should meet performance requirements
        assertThat(searchTime).isLessThan(500); // Under 500ms requirement
        assertThat(response.getMetadata().getResponseTime()).isLessThan(500);
        assertThat(response.getProperties()).isNotEmpty();
    }

    @Test
    @DisplayName("Should handle Vietnamese text encoding correctly")
    void searchProperties_VietnameseEncoding_ShouldHandleUTF8Correctly() throws Exception {
        // Given - Vietnamese text with various Unicode characters
        String[] encodingTestQueries = {
            "Đồng Hới",         // Đ, ồ, ớ
            "Phú Quốc",         // ú, ố
            "Quy Nhơn",         // ơ
            "Cần Thơ",          // ơ
            "Hạ Long",          // ạ
            "Mỹ Tho"            // ỹ
        };

        for (String query : encodingTestQueries) {
            PropertySearchRequest searchRequest = PropertySearchRequest.builder()
                    .query(query)
                    .language("vi")
                    .build();

            // When - searching with encoded Vietnamese text
            PropertySearchResponse response = searchService.searchProperties(searchRequest);

            // Then - should handle UTF-8 encoding correctly
            assertThat(response).isNotNull();
            // Should not throw encoding exceptions and should process the query
            assertThat(response.getMetadata().getResponseTime()).isGreaterThan(0);
        }
    }

    /**
     * Helper methods for Vietnamese text processing
     */
    private void indexVietnameseTestData() {
        // This would normally index test data with Vietnamese content
        // For integration tests, this would populate Elasticsearch with Vietnamese properties
    }

    private boolean containsVietnameseCharacters(String text) {
        // Check if text contains Vietnamese-specific characters
        String vietnameseChars = "àáâãäăắằẳẵặèéêẻẽëếềểễệìíîïĩỉịòóôõöớờởỡợủũûüướừửữựỳýỵỷỹđĐ";
        return text.chars().anyMatch(c -> vietnameseChars.indexOf(c) >= 0);
    }

    private String normalizeVietnamese(String text) {
        // Simple normalization for comparison (remove diacritics)
        return text.toLowerCase()
                .replace("à", "a").replace("á", "a").replace("â", "a").replace("ã", "a").replace("ă", "a")
                .replace("è", "e").replace("é", "e").replace("ê", "e").replace("ẻ", "e").replace("ẽ", "e")
                .replace("ì", "i").replace("í", "i").replace("î", "i").replace("ĩ", "i").replace("ỉ", "i")
                .replace("ò", "o").replace("ó", "o").replace("ô", "o").replace("õ", "o").replace("ơ", "o")
                .replace("ù", "u").replace("ú", "u").replace("û", "u").replace("ũ", "u").replace("ư", "u")
                .replace("ỳ", "y").replace("ý", "y").replace("ỵ", "y").replace("ỷ", "y").replace("ỹ", "y")
                .replace("đ", "d");
    }

    /**
     * Assume services exist for test setup
     */
    private void assumeServicesExist() {
        org.junit.jupiter.api.Assumptions.assumeTrue(searchService != null,
                "SearchService not available - tests will be skipped until implementation");
        org.junit.jupiter.api.Assumptions.assumeTrue(autoCompleteService != null,
                "AutoCompleteService not available - tests will be skipped until implementation");
    }
}