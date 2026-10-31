package com.talentmatch.common.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global REST exception handler producing structured RFC 7807 problem details
 * while ensuring internal implementation, credentials, and SQL syntax are guarded.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> handleApiException(ApiException ex, WebRequest request) {
        log.warn("API Exception [{}]: {}", ex.getStatus(), ex.getMessage());
        return buildErrorResponse(ex.getStatus(), ex.getMessage(), request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(
            MethodArgumentNotValidException ex, WebRequest request) {

        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        log.warn("Validation failed for request: {}", fieldErrors);
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Validation failed for one or more fields", request, fieldErrors);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException ex, WebRequest request) {
        log.warn("Authentication failed: invalid credentials");
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Invalid email address or password.", request, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex, WebRequest request) {
        log.warn("Access denied for request: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.FORBIDDEN, "Access denied: You do not have sufficient permissions to perform this action.", request, null);
    }

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSizeExceeded(
            org.springframework.web.multipart.MaxUploadSizeExceededException ex, WebRequest request) {
        log.warn("Max upload size exceeded: {}", ex.getMessage());
        return buildErrorResponse(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "Uploaded file or batch exceeds the maximum allowed upload size limit.",
                request,
                null
        );
    }

    @ExceptionHandler(org.springframework.web.multipart.MultipartException.class)
    public ResponseEntity<Map<String, Object>> handleMultipartException(
            org.springframework.web.multipart.MultipartException ex, WebRequest request) {
        log.warn("Multipart upload processing failed: {}", ex.getMessage());
        String userFriendlyMessage = "File upload failed. The request contains too many files or exceeds size/part limits.";

        Throwable rootCause = ex.getRootCause();
        if (rootCause != null && rootCause.getClass().getSimpleName().contains("FileCountLimitExceededException")) {
            userFriendlyMessage = "The upload batch contains too many files for a single request. Maximum allowed is 500 files. Consider uploading a ZIP archive instead.";
        }

        return buildErrorResponse(HttpStatus.BAD_REQUEST, userFriendlyMessage, request, null);
    }

    @ExceptionHandler(org.springframework.web.client.ResourceAccessException.class)
    public ResponseEntity<Map<String, Object>> handleResourceAccessException(
            org.springframework.web.client.ResourceAccessException ex, WebRequest request) {
        log.warn("Downstream microservice connection failed: {}", ex.getMessage());
        return buildErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE,
                "The internal AI microservice (http://localhost:8000) is currently offline or unreachable. Please verify the Python service is running.",
                request,
                null
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception ex, WebRequest request) {
        log.error("Unhandled platform exception occurred", ex);
        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected internal error occurred. Please contact system support.",
                request,
                null
        );
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(
            HttpStatus status, String message, WebRequest request, Map<String, String> validationErrors) {

        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);

        if (request instanceof ServletWebRequest servletRequest) {
            body.put("path", servletRequest.getRequest().getRequestURI());
        }

        if (validationErrors != null && !validationErrors.isEmpty()) {
            body.put("validationErrors", validationErrors);
        }

        return new ResponseEntity<>(body, status);
    }
}
