package com.hresources.hr.policy_assistant.config;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/**
 * Standard API error payload returned for validation, configuration, and runtime failures.
 *
 * @param timestamp time when the error response was generated
 * @param status HTTP status code
 * @param error short HTTP error name
 * @param message human-readable error message
 * @param path request path associated with the error
 * @param details optional field or validation details
 */
public record ApiErrorResponse(
        @Schema(description = "Time when the error occurred.")
        Instant timestamp,
        @Schema(description = "HTTP status code.", example = "400")
        int status,
        @Schema(description = "HTTP error name.", example = "Bad Request")
        String error,
        @Schema(description = "Human-readable error message.")
        String message,
        @Schema(description = "Request path associated with the error.", example = "/api/policies/ask")
        String path,
        @Schema(description = "Additional validation or diagnostic details.")
        List<String> details
) {
}
