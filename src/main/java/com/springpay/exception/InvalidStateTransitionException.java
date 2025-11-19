package com.springpay.exception;

/**
 * Exception thrown when attempting an invalid state transition.
 * Example: trying to refund a payment that's not in SUCCESS status.
 * Maps to HTTP 400 Bad Request with specific error code.
 */
public class InvalidStateTransitionException extends RuntimeException {

    private final String currentState;
    private final String attemptedAction;

    public InvalidStateTransitionException(String currentState, String attemptedAction) {
        super(String.format("Cannot perform '%s' action on resource in '%s' state",
                attemptedAction, currentState));
        this.currentState = currentState;
        this.attemptedAction = attemptedAction;
    }

    public InvalidStateTransitionException(String message) {
        super(message);
        this.currentState = null;
        this.attemptedAction = null;
    }

    public String getCurrentState() {
        return currentState;
    }

    public String getAttemptedAction() {
        return attemptedAction;
    }
}
