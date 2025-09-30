package com.cena.traveloka.common.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.Arrays;
import java.util.List;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test class for PageResponse pagination wrapper functionality.
 * Tests pagination metadata, Spring Data integration, and JSON serialization.
 *
 * CRITICAL: These tests MUST FAIL initially (TDD requirement).
 * PageResponse implementation does not exist yet.
 */
class PageResponseTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void shouldCreatePageResponseFromSpringDataPage() {
        // Given: Spring Data Page with content
        List<String> content = Arrays.asList("item1", "item2", "item3");
        Pageable pageable = PageRequest.of(0, 20);
        Page<String> page = new PageImpl<>(content, pageable, 50);

        // When: PageResponse is created from Spring Data Page
        PageResponse<String> pageResponse = PageResponse.of(page);

        // Then: All pagination metadata is correctly transferred
        assertThat(pageResponse.getContent()).hasSize(3);
        assertThat(pageResponse.getContent()).containsExactly("item1", "item2", "item3");
        assertThat(pageResponse.getTotalElements()).isEqualTo(50L);
        assertThat(pageResponse.getTotalPages()).isEqualTo(3);
        assertThat(pageResponse.getSize()).isEqualTo(20);
        assertThat(pageResponse.getNumber()).isEqualTo(0);
        assertThat(pageResponse.isFirst()).isTrue();
        assertThat(pageResponse.isLast()).isFalse();
        assertThat(pageResponse.isEmpty()).isFalse();
    }

    @Test
    void shouldCreatePageResponseWithTransformedContent() {
        // Given: Spring Data Page with entity content
        List<TestEntity> entities = Arrays.asList(
            new TestEntity(1L, "Entity 1"),
            new TestEntity(2L, "Entity 2")
        );
        Page<TestEntity> entityPage = new PageImpl<>(entities, PageRequest.of(0, 10), 2);

        // Transform entities to DTOs
        List<TestDto> dtos = entities.stream()
            .map(entity -> new TestDto(entity.getId(), entity.getName().toUpperCase()))
            .toList();

        // When: PageResponse is created with transformed content
        PageResponse<TestDto> pageResponse = PageResponse.of(entityPage, dtos);

        // Then: Content is transformed but metadata preserved
        assertThat(pageResponse.getContent()).hasSize(2);
        assertThat(pageResponse.getContent().get(0).getName()).isEqualTo("ENTITY 1");
        assertThat(pageResponse.getContent().get(1).getName()).isEqualTo("ENTITY 2");
        assertThat(pageResponse.getTotalElements()).isEqualTo(2L);
        assertThat(pageResponse.getTotalPages()).isEqualTo(1);
        assertThat(pageResponse.getSize()).isEqualTo(10);
    }

    @Test
    void shouldHandleEmptyPage() {
        // Given: Empty Spring Data Page
        Page<String> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0);

        // When: PageResponse is created from empty page
        PageResponse<String> pageResponse = PageResponse.of(emptyPage);

        // Then: Empty page metadata is correct
        assertThat(pageResponse.getContent()).isEmpty();
        assertThat(pageResponse.getTotalElements()).isEqualTo(0L);
        assertThat(pageResponse.getTotalPages()).isEqualTo(0);
        assertThat(pageResponse.getSize()).isEqualTo(20);
        assertThat(pageResponse.getNumber()).isEqualTo(0);
        assertThat(pageResponse.isFirst()).isTrue();
        assertThat(pageResponse.isLast()).isTrue();
        assertThat(pageResponse.isEmpty()).isTrue();
    }

    @Test
    void shouldHandleLastPage() {
        // Given: Last page of results
        List<String> content = Arrays.asList("item21", "item22");
        Pageable pageable = PageRequest.of(2, 10); // Page 2 (0-based), 10 items per page
        Page<String> lastPage = new PageImpl<>(content, pageable, 22); // Total 22 items

        // When: PageResponse is created
        PageResponse<String> pageResponse = PageResponse.of(lastPage);

        // Then: Last page metadata is correct
        assertThat(pageResponse.getContent()).hasSize(2);
        assertThat(pageResponse.getTotalElements()).isEqualTo(22L);
        assertThat(pageResponse.getTotalPages()).isEqualTo(3); // Pages 0, 1, 2
        assertThat(pageResponse.getSize()).isEqualTo(10);
        assertThat(pageResponse.getNumber()).isEqualTo(2);
        assertThat(pageResponse.isFirst()).isFalse();
        assertThat(pageResponse.isLast()).isTrue();
        assertThat(pageResponse.isEmpty()).isFalse();
    }

    @Test
    void shouldHandleMiddlePage() {
        // Given: Middle page of results
        List<Integer> content = Arrays.asList(11, 12, 13, 14, 15);
        Pageable pageable = PageRequest.of(1, 5); // Page 1 (0-based), 5 items per page
        Page<Integer> middlePage = new PageImpl<>(content, pageable, 23); // Total 23 items

        // When: PageResponse is created
        PageResponse<Integer> pageResponse = PageResponse.of(middlePage);

        // Then: Middle page metadata is correct
        assertThat(pageResponse.getContent()).hasSize(5);
        assertThat(pageResponse.getTotalElements()).isEqualTo(23L);
        assertThat(pageResponse.getTotalPages()).isEqualTo(5); // 23 items / 5 per page = 5 pages
        assertThat(pageResponse.getSize()).isEqualTo(5);
        assertThat(pageResponse.getNumber()).isEqualTo(1);
        assertThat(pageResponse.isFirst()).isFalse();
        assertThat(pageResponse.isLast()).isFalse();
        assertThat(pageResponse.isEmpty()).isFalse();
    }

    @Test
    void shouldSerializeToJsonCorrectly() throws JsonProcessingException {
        // Given: PageResponse with data
        List<String> items = Arrays.asList("test1", "test2");
        Page<String> page = new PageImpl<>(items, PageRequest.of(0, 20), 50);
        PageResponse<String> pageResponse = PageResponse.of(page);

        // When: PageResponse is serialized to JSON
        String json = objectMapper.writeValueAsString(pageResponse);

        // Then: JSON contains all pagination fields
        assertThat(json).contains("\"content\":[\"test1\",\"test2\"]");
        assertThat(json).contains("\"totalElements\":50");
        assertThat(json).contains("\"totalPages\":3");
        assertThat(json).contains("\"size\":20");
        assertThat(json).contains("\"number\":0");
        assertThat(json).contains("\"first\":true");
        assertThat(json).contains("\"last\":false");
        assertThat(json).contains("\"empty\":false");
    }

    @Test
    void shouldDeserializeFromJsonCorrectly() throws JsonProcessingException {
        // Given: JSON string representing PageResponse
        String json = """
            {
                "content": ["item1", "item2"],
                "totalElements": 100,
                "totalPages": 5,
                "size": 20,
                "number": 1,
                "first": false,
                "last": false,
                "empty": false
            }
            """;

        // When: JSON is deserialized to PageResponse
        PageResponse<?> pageResponse = objectMapper.readValue(json, PageResponse.class);

        // Then: All fields are correctly deserialized
        assertThat(pageResponse.getContent()).hasSize(2);
        assertThat(pageResponse.getTotalElements()).isEqualTo(100L);
        assertThat(pageResponse.getTotalPages()).isEqualTo(5);
        assertThat(pageResponse.getSize()).isEqualTo(20);
        assertThat(pageResponse.getNumber()).isEqualTo(1);
        assertThat(pageResponse.isFirst()).isFalse();
        assertThat(pageResponse.isLast()).isFalse();
        assertThat(pageResponse.isEmpty()).isFalse();
    }

    @Test
    void shouldCalculatePaginationStateCorrectly() {
        // Test Case 1: Single page result
        Page<String> singlePage = new PageImpl<>(Arrays.asList("only"), PageRequest.of(0, 10), 1);
        PageResponse<String> singlePageResponse = PageResponse.of(singlePage);

        assertThat(singlePageResponse.isFirst()).isTrue();
        assertThat(singlePageResponse.isLast()).isTrue();

        // Test Case 2: First page of many
        Page<String> firstPage = new PageImpl<>(Arrays.asList("a", "b"), PageRequest.of(0, 2), 10);
        PageResponse<String> firstPageResponse = PageResponse.of(firstPage);

        assertThat(firstPageResponse.isFirst()).isTrue();
        assertThat(firstPageResponse.isLast()).isFalse();

        // Test Case 3: Last page of many
        Page<String> lastPage = new PageImpl<>(Arrays.asList("y", "z"), PageRequest.of(4, 2), 10);
        PageResponse<String> lastPageResponse = PageResponse.of(lastPage);

        assertThat(lastPageResponse.isFirst()).isFalse();
        assertThat(lastPageResponse.isLast()).isTrue();
    }

    @Test
    void shouldSupportDifferentPageSizes() {
        // Test default page size (20)
        Page<String> defaultSizePage = new PageImpl<>(
            Arrays.asList("item1"), PageRequest.of(0, 20), 1);
        PageResponse<String> defaultResponse = PageResponse.of(defaultSizePage);
        assertThat(defaultResponse.getSize()).isEqualTo(20);

        // Test maximum page size (100)
        Page<String> maxSizePage = new PageImpl<>(
            Arrays.asList("item1"), PageRequest.of(0, 100), 1);
        PageResponse<String> maxResponse = PageResponse.of(maxSizePage);
        assertThat(maxResponse.getSize()).isEqualTo(100);

        // Test custom page size
        Page<String> customSizePage = new PageImpl<>(
            Arrays.asList("item1"), PageRequest.of(0, 15), 1);
        PageResponse<String> customResponse = PageResponse.of(customSizePage);
        assertThat(customResponse.getSize()).isEqualTo(15);
    }

    @Test
    void shouldHandleComplexGenericTypes() {
        // Given: Page with complex DTO objects
        List<TestDto> dtos = Arrays.asList(
            new TestDto(1L, "First"),
            new TestDto(2L, "Second")
        );
        Page<TestDto> dtoPage = new PageImpl<>(dtos, PageRequest.of(0, 10), 2);

        // When: PageResponse is created with generic type
        PageResponse<TestDto> pageResponse = PageResponse.of(dtoPage);

        // Then: Generic type is preserved
        assertThat(pageResponse.getContent()).hasSize(2);
        assertThat(pageResponse.getContent().get(0)).isInstanceOf(TestDto.class);
        assertThat(pageResponse.getContent().get(0).getId()).isEqualTo(1L);
        assertThat(pageResponse.getContent().get(1).getName()).isEqualTo("Second");
    }

    @Test
    void shouldSupportBuilderPattern() {
        // Given: Manual builder usage
        List<String> content = Arrays.asList("manual1", "manual2");

        // When: PageResponse is built manually
        PageResponse<String> pageResponse = PageResponse.<String>builder()
            .content(content)
            .totalElements(25L)
            .totalPages(3)
            .size(10)
            .number(1)
            .first(false)
            .last(false)
            .empty(false)
            .build();

        // Then: All fields are set correctly
        assertThat(pageResponse.getContent()).isEqualTo(content);
        assertThat(pageResponse.getTotalElements()).isEqualTo(25L);
        assertThat(pageResponse.getTotalPages()).isEqualTo(3);
        assertThat(pageResponse.getSize()).isEqualTo(10);
        assertThat(pageResponse.getNumber()).isEqualTo(1);
        assertThat(pageResponse.isFirst()).isFalse();
        assertThat(pageResponse.isLast()).isFalse();
        assertThat(pageResponse.isEmpty()).isFalse();
    }

    /**
     * Test entity for pagination testing
     */
    public static class TestEntity {
        private final Long id;
        private final String name;

        public TestEntity(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        public Long getId() { return id; }
        public String getName() { return name; }
    }

    /**
     * Test DTO for pagination testing
     */
    public static class TestDto {
        private final Long id;
        private final String name;

        public TestDto(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        public Long getId() { return id; }
        public String getName() { return name; }
    }
}