package com.cena.traveloka.common.dto;

import com.cena.traveloka.common.enums.ResponseStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.ZonedDateTime;
import java.time.ZoneOffset;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final ResponseStatus status;
    private final String code;
    private final String message;
    private final T data;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private final ZonedDateTime timestamp;

    private ApiResponse(Builder<T> builder) {
        this.status = builder.status;
        this.code = builder.code != null ? builder.code : generateDefaultCode(builder.status);
        this.message = builder.message;
        this.data = builder.data;
        this.timestamp = builder.timestamp != null ? builder.timestamp : ZonedDateTime.now(ZoneOffset.UTC);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
            .status(ResponseStatus.SUCCESS)
            .message(message)
            .data(data)
            .build();
    }

    public static ApiResponse<Void> success(String message) {
        return ApiResponse.<Void>builder()
            .status(ResponseStatus.SUCCESS)
            .message(message)
            .build();
    }

    public static ApiResponse<Void> error(String code, String message) {
        return ApiResponse.<Void>builder()
            .status(ResponseStatus.ERROR)
            .code(code)
            .message(message)
            .build();
    }

    public static <T> ApiResponse<T> warning(String code, String message, T data) {
        return ApiResponse.<T>builder()
            .status(ResponseStatus.WARNING)
            .code(code)
            .message(message)
            .data(data)
            .build();
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    private String generateDefaultCode(ResponseStatus status) {
        return switch (status) {
            case SUCCESS -> "SUCCESS";
            case ERROR -> "ERROR";
            case WARNING -> "WARNING";
        };
    }

    public ResponseStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    public static class Builder<T> {
        private ResponseStatus status;
        private String code;
        private String message;
        private T data;
        private ZonedDateTime timestamp;

        public Builder<T> status(ResponseStatus status) {
            this.status = status;
            return this;
        }

        public Builder<T> code(String code) {
            this.code = code;
            return this;
        }

        public Builder<T> message(String message) {
            this.message = message;
            return this;
        }

        public Builder<T> data(T data) {
            this.data = data;
            return this;
        }

        public Builder<T> timestamp(ZonedDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public ApiResponse<T> build() {
            if (status == null) {
                throw new IllegalArgumentException("Response status is required");
            }
            if (message == null || message.trim().isEmpty()) {
                throw new IllegalArgumentException("Response message is required");
            }
            return new ApiResponse<>(this);
        }
    }

    @Override
    public String toString() {
        return String.format("ApiResponse{status=%s, code='%s', message='%s', data=%s, timestamp=%s}",
            status, code, message, data != null ? data.getClass().getSimpleName() : "null", timestamp);
    }
}