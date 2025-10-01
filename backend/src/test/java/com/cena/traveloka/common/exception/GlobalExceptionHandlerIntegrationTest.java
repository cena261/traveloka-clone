package com.cena.traveloka.common.exception;

import com.cena.traveloka.common.dto.ApiResponse;
import com.cena.traveloka.common.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * T045: Integration test for GlobalExceptionHandler with @SpringBootTest.
 *
 * This test verifies:
 * - GlobalExceptionHandler intercepts exceptions correctly
 * - Standardized error responses are returned
 * - HTTP status codes are mapped correctly (404, 400, 500)
 * - ErrorResponse structure matches specifications
 * - Validation errors are handled properly
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GlobalExceptionHandlerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void entityNotFoundExceptionReturns404() throws Exception {
        MvcResult result = mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        String content = result.getResponse().getContentAsString();
        ErrorResponse errorResponse = objectMapper.readValue(content, ErrorResponse.class);

        assertThat(errorResponse).isNotNull();
        assertThat(errorResponse.getCode()).contains("NOT_FOUND");
        assertThat(errorResponse.getMessage()).isNotBlank();
        assertThat(errorResponse.getTimestamp()).isNotNull();
        assertThat(errorResponse.getPath()).isEqualTo("/test/not-found");
    }

    @Test
    void businessExceptionReturns400() throws Exception {
        MvcResult result = mockMvc.perform(get("/test/business-error"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        String content = result.getResponse().getContentAsString();
        ErrorResponse errorResponse = objectMapper.readValue(content, ErrorResponse.class);

        assertThat(errorResponse).isNotNull();
        assertThat(errorResponse.getCode()).isEqualTo("BUSINESS_ERROR");
        assertThat(errorResponse.getMessage()).contains("Business rule violation");
        assertThat(errorResponse.getTimestamp()).isNotNull();
        assertThat(errorResponse.getPath()).isEqualTo("/test/business-error");
    }

    @Test
    void validationExceptionReturns400() throws Exception {
        MvcResult result = mockMvc.perform(get("/test/validation-error"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        String content = result.getResponse().getContentAsString();
        ErrorResponse errorResponse = objectMapper.readValue(content, ErrorResponse.class);

        assertThat(errorResponse).isNotNull();
        assertThat(errorResponse.getCode()).contains("VALIDATION");
        assertThat(errorResponse.getField()).isEqualTo("email");
        assertThat(errorResponse.getTimestamp()).isNotNull();
        assertThat(errorResponse.getPath()).isEqualTo("/test/validation-error");
    }

    @Test
    void genericExceptionReturns500() throws Exception {
        MvcResult result = mockMvc.perform(get("/test/generic-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        String content = result.getResponse().getContentAsString();
        ErrorResponse errorResponse = objectMapper.readValue(content, ErrorResponse.class);

        assertThat(errorResponse).isNotNull();
        assertThat(errorResponse.getCode()).contains("INTERNAL_ERROR");
        assertThat(errorResponse.getMessage()).isNotBlank();
        assertThat(errorResponse.getTimestamp()).isNotNull();
        assertThat(errorResponse.getPath()).isEqualTo("/test/generic-error");
    }

    @Test
    void errorResponseHasCorrectStructure() throws Exception {
        MvcResult result = mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        ErrorResponse errorResponse = objectMapper.readValue(content, ErrorResponse.class);

        // Verify all required fields from data-model.md
        assertThat(errorResponse.getCode()).isNotNull();
        assertThat(errorResponse.getMessage()).isNotNull();
        assertThat(errorResponse.getTimestamp()).isNotNull();
        assertThat(errorResponse.getPath()).isNotNull();
        // field is optional
    }

    /**
     * Test controller to simulate various exception scenarios
     */
    @RestController
    @RequestMapping("/test")
    static class TestExceptionController {

        @GetMapping("/not-found")
        public void throwEntityNotFoundException() {
            throw new EntityNotFoundException("Test entity not found");
        }

        @GetMapping("/business-error")
        public void throwBusinessException() {
            throw new BusinessException("BUSINESS_ERROR", "Business rule violation");
        }

        @GetMapping("/validation-error")
        public void throwValidationException() {
            ValidationException exception = new ValidationException("Validation failed");
            exception.setField("email");
            throw exception;
        }

        @GetMapping("/generic-error")
        public void throwGenericException() {
            throw new RuntimeException("Generic error occurred");
        }

        @GetMapping("/success")
        public ApiResponse<String> successEndpoint() {
            return ApiResponse.success("Operation successful", "test-data");
        }
    }
}

/**
 * Custom exception classes for testing
 * These mirror the actual exception classes that should be in common.exception package
 */
class EntityNotFoundException extends RuntimeException {
    public EntityNotFoundException(String message) {
        super(message);
    }
}

class BusinessException extends RuntimeException {
    private final String code;

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}

class ValidationException extends RuntimeException {
    private String field;

    public ValidationException(String message) {
        super(message);
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }
}
