package com.cena.traveloka.iam.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.util.List;

/**
 * Standardized paginated response wrapper
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PagedResponse<T> {

    private List<T> content;

    private PageInfo page;

    private boolean success = true;

    private String message;

    private Instant timestamp;

    private String traceId;

    public PagedResponse() {
        this.timestamp = Instant.now();
    }

    public PagedResponse(List<T> content, PageInfo page) {
        this();
        this.content = content;
        this.page = page;
    }

    /**
     * Page metadata
     */
    @Data
    public static class PageInfo {
        private int number;
        private int size;
        private int totalPages;
        private long totalElements;
        private boolean first;
        private boolean last;
        private int numberOfElements;
        private boolean empty;

        public PageInfo() {}

        public PageInfo(Page<?> page) {
            this.number = page.getNumber();
            this.size = page.getSize();
            this.totalPages = page.getTotalPages();
            this.totalElements = page.getTotalElements();
            this.first = page.isFirst();
            this.last = page.isLast();
            this.numberOfElements = page.getNumberOfElements();
            this.empty = page.isEmpty();
        }
    }

    // === Static Factory Methods ===

    public static <T> PagedResponse<T> of(Page<T> page) {
        PagedResponse<T> response = new PagedResponse<>();
        response.setContent(page.getContent());
        response.setPage(new PageInfo(page));
        return response;
    }

    public static <T> PagedResponse<T> of(List<T> content, Page<?> page) {
        PagedResponse<T> response = new PagedResponse<>();
        response.setContent(content);
        response.setPage(new PageInfo(page));
        return response;
    }

    public static <T> PagedResponse<T> of(List<T> content, PageInfo pageInfo) {
        return new PagedResponse<>(content, pageInfo);
    }

    public static <T> PagedResponse<T> empty() {
        PagedResponse<T> response = new PagedResponse<>();
        response.setContent(List.of());

        PageInfo pageInfo = new PageInfo();
        pageInfo.setNumber(0);
        pageInfo.setSize(0);
        pageInfo.setTotalPages(0);
        pageInfo.setTotalElements(0);
        pageInfo.setFirst(true);
        pageInfo.setLast(true);
        pageInfo.setNumberOfElements(0);
        pageInfo.setEmpty(true);

        response.setPage(pageInfo);
        return response;
    }

    // === Builder Pattern ===

    public static <T> PagedResponseBuilder<T> builder() {
        return new PagedResponseBuilder<>();
    }

    public static class PagedResponseBuilder<T> {
        private final PagedResponse<T> response = new PagedResponse<>();

        public PagedResponseBuilder<T> content(List<T> content) {
            response.setContent(content);
            return this;
        }

        public PagedResponseBuilder<T> page(PageInfo page) {
            response.setPage(page);
            return this;
        }

        public PagedResponseBuilder<T> page(Page<?> page) {
            response.setPage(new PageInfo(page));
            return this;
        }

        public PagedResponseBuilder<T> success(boolean success) {
            response.setSuccess(success);
            return this;
        }

        public PagedResponseBuilder<T> message(String message) {
            response.setMessage(message);
            return this;
        }

        public PagedResponseBuilder<T> traceId(String traceId) {
            response.setTraceId(traceId);
            return this;
        }

        public PagedResponse<T> build() {
            return response;
        }
    }

    // === Convenience Methods ===

    public boolean hasContent() {
        return content != null && !content.isEmpty();
    }

    public int getContentSize() {
        return content != null ? content.size() : 0;
    }

    public boolean isFirstPage() {
        return page != null && page.isFirst();
    }

    public boolean isLastPage() {
        return page != null && page.isLast();
    }

    public long getTotalElements() {
        return page != null ? page.getTotalElements() : 0;
    }

    public int getTotalPages() {
        return page != null ? page.getTotalPages() : 0;
    }
}