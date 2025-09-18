package com.cena.traveloka.catalog.inventory.exception;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ErrorResponse {
    OffsetDateTime timestamp;
    Integer status;
    String error;
    String message;
    String path;
    Map<String, String> fieldErrors;
}