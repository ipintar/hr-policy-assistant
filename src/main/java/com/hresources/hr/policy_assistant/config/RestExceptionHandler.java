package com.hresources.hr.policy_assistant.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

/**
 * Converts application and validation exceptions into consistent JSON error responses.
 */
@RestControllerAdvice
public class RestExceptionHandler {

    /**
     * Handles invalid request body payloads.
     *
     * @param exception validation exception raised by Spring MVC
     * @param request current HTTP request
     * @return structured bad request response
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        List<String> details = exception.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .toList();

        return buildResponse(HttpStatus.BAD_REQUEST, "Request validation failed.", details, request.getRequestURI());
    }

    /**
     * Handles parameter and constraint validation failures.
     *
     * @param exception validation exception raised by Jakarta Bean Validation
     * @param request current HTTP request
     * @return structured bad request response
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request) {
        List<String> details = exception.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .toList();

        return buildResponse(HttpStatus.BAD_REQUEST, "Constraint validation failed.", details, request.getRequestURI());
    }

    /**
     * Handles known application exceptions.
     *
     * @param exception domain-specific policy assistant exception
     * @param request current HTTP request
     * @return structured error response
     */
    @ExceptionHandler(PolicyAssistantException.class)
    public ResponseEntity<ApiErrorResponse> handlePolicyAssistantException(
            PolicyAssistantException exception,
            HttpServletRequest request) {
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage(), List.of(), request.getRequestURI());
    }

    /**
     * Handles uncaught runtime exceptions.
     *
     * @param exception unhandled runtime exception
     * @param request current HTTP request
     * @return structured internal server error response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(
            Exception exception,
            HttpServletRequest request) {
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred while processing the request.",
                List.of(exception.getClass().getSimpleName()),
                request.getRequestURI()
        );
    }

    /**
     * Formats a single field validation error into a user-facing string.
     *
     * @param fieldError field validation error
     * @return formatted field error message
     */
    private String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }

    /**
     * Builds a standardized error response entity.
     *
     * @param status HTTP status to return
     * @param message top-level error message
     * @param details optional diagnostic details
     * @param path request path where the error occurred
     * @return response entity containing the API error body
     */
    private ResponseEntity<ApiErrorResponse> buildResponse(
            HttpStatus status,
            String message,
            List<String> details,
            String path) {
        return ResponseEntity.status(status)
                .body(new ApiErrorResponse(
                        Instant.now(),
                        status.value(),
                        status.getReasonPhrase(),
                        message,
                        path,
                        details
                ));
    }
}
