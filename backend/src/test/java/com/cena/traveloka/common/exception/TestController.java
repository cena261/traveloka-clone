package com.cena.traveloka.common.exception;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cena.traveloka.common.dto.ApiResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Test controller for GlobalExceptionHandler testing.
 * Provides endpoints that trigger various exception scenarios.
 */
@RestController
@RequestMapping("/test")
public class TestController {

    @PostMapping("/validate")
    public ApiResponse<String> validateInput(@Valid @RequestBody TestRequest request) {
        return ApiResponse.success("Validation passed", "OK");
    }

    @PostMapping("/not-found")
    public ApiResponse<String> triggerNotFound() {
        throw new EntityNotFoundException("Test entity not found");
    }

    @PostMapping("/business-error")
    public ApiResponse<String> triggerBusinessError() {
        throw new BusinessException("BUSINESS_RULE_VIOLATION", "Test business rule violation");
    }

    @PostMapping("/generic-error")
    public ApiResponse<String> triggerGenericError() {
        throw new RuntimeException("Test generic error");
    }

    /**
     * Test request DTO for validation testing
     */
    public static class TestRequest {

        @NotBlank(message = "Name is required")
        private String name;

        @Email(message = "Valid email is required")
        @NotBlank(message = "Email is required")
        private String email;

        public TestRequest() {}

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }
}