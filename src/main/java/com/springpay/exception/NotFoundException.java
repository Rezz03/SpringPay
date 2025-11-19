package com.springpay.exception;

/**
 * Exception thrown when a requested resource is not found.
 * Maps to HTTP 404 Not Found.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }

    public NotFoundException(String resource, Long id) {
        super(String.format("%s with id %d not found", resource, id));
    }

    public NotFoundException(String resource, String identifier, String value) {
        super(String.format("%s with %s '%s' not found", resource, identifier, value));
    }
}
