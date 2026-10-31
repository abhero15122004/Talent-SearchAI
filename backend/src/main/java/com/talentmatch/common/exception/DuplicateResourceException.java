package com.talentmatch.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when attempting to create or update a resource with a unique constraint conflict.
 */
public class DuplicateResourceException extends ApiException {

    public DuplicateResourceException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
