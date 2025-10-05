package com.cena.traveloka.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.ZonedDateTime;
import java.time.ZoneOffset;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private final String code;
    private final String message;
    private final String field;
    private final String path;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private final ZonedDateTime timestamp;

    private ErrorResponse(Builder builder) {
        this.code = builder.code;
        this.message = builder.message;
        this.field = builder.field;
        this.path = builder.path;
        this.timestamp = builder.timestamp != null ? builder.timestamp : ZonedDateTime.now(ZoneOffset.UTC);
    }

    public static ErrorResponse of(String code, String message) {
        return builder()
            .code(code)
            .message(message)
            .build();
    }

    public static ErrorResponse fieldError(String field, String message) {
        return builder()
            .code("FIELD_VALIDATION_FAILED")
            .message(message)
            .field(field)
            .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getField() {
        return field;
    }

    public String getPath() {
        return path;
    }

    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    public static class Builder {
        private String code;
        private String message;
        private String field;
        private String path;
        private ZonedDateTime timestamp;

        public Builder code(String code) {
            this.code = code;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder field(String field) {
            this.field = field;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder timestamp(ZonedDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public ErrorResponse build() {
            if (code == null || code.trim().isEmpty()) {
                throw new IllegalArgumentException("Error code is required");
            }
            if (message == null || message.trim().isEmpty()) {
                throw new IllegalArgumentException("Error message is required");
            }
            return new ErrorResponse(this);
        }
    }

    @Override
    public String toString() {
        return String.format("ErrorResponse{code='%s', message='%s', field='%s', path='%s', timestamp=%s}",
            code, message, field, path, timestamp);
    }
}