package com.cena.traveloka.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Standard pagination response wrapper for paginated API responses.
 * Provides consistent pagination metadata across all paginated endpoints.
 *
 * Features:
 * - Compatible with Spring Data Page objects
 * - Support for content transformation (entity to DTO)
 * - Standard pagination metadata (total elements, pages, etc.)
 * - Builder pattern for flexible construction
 * - JSON serialization support
 *
 * @param <T> The type of content being paginated
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageResponse<T> {

    private final List<T> content;
    private final long totalElements;
    private final int totalPages;
    private final int size;
    private final int number;
    private final boolean first;
    private final boolean last;
    private final boolean empty;

    /**
     * Private constructor for builder pattern
     */
    private PageResponse(Builder<T> builder) {
        this.content = builder.content;
        this.totalElements = builder.totalElements;
        this.totalPages = builder.totalPages;
        this.size = builder.size;
        this.number = builder.number;
        this.first = builder.first;
        this.last = builder.last;
        this.empty = builder.empty;
    }

    /**
     * Creates a PageResponse from a Spring Data Page object
     *
     * @param page Spring Data Page
     * @param <T> Content type
     * @return PageResponse with the same content and metadata
     */
    public static <T> PageResponse<T> of(Page<T> page) {
        return PageResponse.<T>builder()
            .content(page.getContent())
            .totalElements(page.getTotalElements())
            .totalPages(page.getTotalPages())
            .size(page.getSize())
            .number(page.getNumber())
            .first(page.isFirst())
            .last(page.isLast())
            .empty(page.isEmpty())
            .build();
    }

    /**
     * Creates a PageResponse with transformed content while preserving pagination metadata
     *
     * @param page Original Spring Data Page
     * @param transformedContent Transformed content (e.g., entities converted to DTOs)
     * @param <S> Original content type
     * @param <T> Transformed content type
     * @return PageResponse with transformed content and original pagination metadata
     */
    public static <S, T> PageResponse<T> of(Page<S> page, List<T> transformedContent) {
        return PageResponse.<T>builder()
            .content(transformedContent)
            .totalElements(page.getTotalElements())
            .totalPages(page.getTotalPages())
            .size(page.getSize())
            .number(page.getNumber())
            .first(page.isFirst())
            .last(page.isLast())
            .empty(page.isEmpty())
            .build();
    }

    /**
     * Returns a new builder for constructing PageResponse instances
     *
     * @param <T> Content type
     * @return Builder instance
     */
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    /**
     * Gets the list of content for this page
     *
     * @return List of content items
     */
    public List<T> getContent() {
        return content;
    }

    /**
     * Gets the total number of elements across all pages
     *
     * @return Total elements count
     */
    public long getTotalElements() {
        return totalElements;
    }

    /**
     * Gets the total number of pages
     *
     * @return Total pages count
     */
    public int getTotalPages() {
        return totalPages;
    }

    /**
     * Gets the page size (number of elements per page)
     *
     * @return Page size
     */
    public int getSize() {
        return size;
    }

    /**
     * Gets the current page number (0-based)
     *
     * @return Current page number
     */
    public int getNumber() {
        return number;
    }

    /**
     * Checks if this is the first page
     *
     * @return true if this is the first page
     */
    public boolean isFirst() {
        return first;
    }

    /**
     * Checks if this is the last page
     *
     * @return true if this is the last page
     */
    public boolean isLast() {
        return last;
    }

    /**
     * Checks if this page is empty (no content)
     *
     * @return true if this page has no content
     */
    public boolean isEmpty() {
        return empty;
    }

    /**
     * Builder class for constructing PageResponse instances
     *
     * @param <T> Content type
     */
    public static class Builder<T> {
        private List<T> content;
        private long totalElements;
        private int totalPages;
        private int size;
        private int number;
        private boolean first;
        private boolean last;
        private boolean empty;

        /**
         * Sets the page content
         *
         * @param content List of content items
         * @return Builder instance for method chaining
         */
        public Builder<T> content(List<T> content) {
            this.content = content;
            return this;
        }

        /**
         * Sets the total number of elements
         *
         * @param totalElements Total elements count
         * @return Builder instance for method chaining
         */
        public Builder<T> totalElements(long totalElements) {
            this.totalElements = totalElements;
            return this;
        }

        /**
         * Sets the total number of pages
         *
         * @param totalPages Total pages count
         * @return Builder instance for method chaining
         */
        public Builder<T> totalPages(int totalPages) {
            this.totalPages = totalPages;
            return this;
        }

        /**
         * Sets the page size
         *
         * @param size Page size
         * @return Builder instance for method chaining
         */
        public Builder<T> size(int size) {
            this.size = size;
            return this;
        }

        /**
         * Sets the page number
         *
         * @param number Page number (0-based)
         * @return Builder instance for method chaining
         */
        public Builder<T> number(int number) {
            this.number = number;
            return this;
        }

        /**
         * Sets whether this is the first page
         *
         * @param first true if first page
         * @return Builder instance for method chaining
         */
        public Builder<T> first(boolean first) {
            this.first = first;
            return this;
        }

        /**
         * Sets whether this is the last page
         *
         * @param last true if last page
         * @return Builder instance for method chaining
         */
        public Builder<T> last(boolean last) {
            this.last = last;
            return this;
        }

        /**
         * Sets whether this page is empty
         *
         * @param empty true if page is empty
         * @return Builder instance for method chaining
         */
        public Builder<T> empty(boolean empty) {
            this.empty = empty;
            return this;
        }

        /**
         * Builds the PageResponse instance
         *
         * @return PageResponse instance
         * @throws IllegalArgumentException if required fields are missing
         */
        public PageResponse<T> build() {
            if (content == null) {
                throw new IllegalArgumentException("Content is required");
            }
            return new PageResponse<>(this);
        }
    }

    @Override
    public String toString() {
        return String.format("PageResponse{contentSize=%d, totalElements=%d, totalPages=%d, size=%d, number=%d, first=%s, last=%s, empty=%s}",
            content != null ? content.size() : 0, totalElements, totalPages, size, number, first, last, empty);
    }
}