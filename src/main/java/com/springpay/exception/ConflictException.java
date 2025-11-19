package com.springpay.exception;

/**
 * Exception thrown when a request conflicts with existing state.
 * Common cases: duplicate email, duplicate API key, invalid state transition.
 * Maps to HTTP 409 Conflict.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }

    public ConflictException(String resource, String field, String value) {
        super(String.format("%s with %s '%s' already exists", resource, field, value));
    }
}
