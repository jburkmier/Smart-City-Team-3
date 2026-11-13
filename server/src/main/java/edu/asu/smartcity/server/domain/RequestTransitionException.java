package edu.asu.smartcity.server.domain;

/** Raised when an action is not allowed in a request's current state. */
public class RequestTransitionException extends RuntimeException {

    public RequestTransitionException(String message) {
        super(message);
    }
}
