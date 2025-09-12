package com.cena.traveloka.common.exception;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data @Builder
public class ErrorResponse {
    private String code;
    private String message;
    private int status;
    private String path;
    private String traceId;
    private OffsetDateTime timestamp;
    private List<ValidationError> errors;
}
