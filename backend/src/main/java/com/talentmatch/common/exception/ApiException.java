package com.talentmatch.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base custom API exception holding an HTTP status code and user-friendly error message.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
