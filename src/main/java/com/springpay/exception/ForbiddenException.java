package com.springpay.exception;

/**
 * Exception thrown when a user lacks necessary permissions.
 * Maps to HTTP 403 Forbidden.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }

    public ForbiddenException() {
        super("Insufficient permissions");
    }
}
