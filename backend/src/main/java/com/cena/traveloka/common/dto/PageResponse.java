package com.cena.traveloka.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.data.domain.Page;

import java.util.List;

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

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public List<T> getContent() {
        return content;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public int getSize() {
        return size;
    }

    public int getNumber() {
        return number;
    }

    public boolean isFirst() {
        return first;
    }

    public boolean isLast() {
        return last;
    }

    public boolean isEmpty() {
        return empty;
    }

    public static class Builder<T> {
        private List<T> content;
        private long totalElements;
        private int totalPages;
        private int size;
        private int number;
        private boolean first;
        private boolean last;
        private boolean empty;

        public Builder<T> content(List<T> content) {
            this.content = content;
            return this;
        }

        public Builder<T> totalElements(long totalElements) {
            this.totalElements = totalElements;
            return this;
        }

        public Builder<T> totalPages(int totalPages) {
            this.totalPages = totalPages;
            return this;
        }

        public Builder<T> size(int size) {
            this.size = size;
            return this;
        }

        public Builder<T> number(int number) {
            this.number = number;
            return this;
        }

        public Builder<T> first(boolean first) {
            this.first = first;
            return this;
        }

        public Builder<T> last(boolean last) {
            this.last = last;
            return this;
        }

        public Builder<T> empty(boolean empty) {
            this.empty = empty;
            return this;
        }

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