package com.springpay.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standardized error response DTO.
 * Returned by GlobalExceptionHandler for all API errors.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    /**
     * Timestamp when the error occurred
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * HTTP status code (404, 400, 401, etc.)
     */
    private int status;

    /**
     * Brief error identifier (e.g., "NOT_FOUND", "VALIDATION_ERROR")
     */
    private String error;

    /**
     * Human-readable error message
     */
    private String message;

    /**
     * Request path that caused the error
     */
    private String path;

    /**
     * Field-specific validation errors (optional)
     * Only populated for ValidationException
     */
    private Map<String, String> fieldErrors;

    /**
     * Additional context (optional)
     * For debugging or providing extra details
     */
    private Map<String, Object> details;
}
