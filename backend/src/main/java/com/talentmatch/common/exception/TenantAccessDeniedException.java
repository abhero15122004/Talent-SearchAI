package com.talentmatch.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an authenticated user attempts to access or mutate resources outside their organization scope.
 */
public class TenantAccessDeniedException extends ApiException {

    public TenantAccessDeniedException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }

    public TenantAccessDeniedException() {
        super("Access denied: You do not have permission to access resources outside your assigned organization.", HttpStatus.FORBIDDEN);
    }
}
