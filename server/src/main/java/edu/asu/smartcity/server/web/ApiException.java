package edu.asu.smartcity.server.web;

import java.util.Map;

/**
 * Thrown anywhere in the server to send an error response in the docs/api.md format.
 * ServerApp turns it into the HTTP status and an ErrorResponse body.
 */
public class ApiException extends RuntimeException {

    private final int status;
    private final String code;
    private final Map<String, String> fields;

    public ApiException(int status, String code, String message) {
        this(status, code, message, null);
    }

    /** fields maps input names to problems; only used for VALIDATION_FAILED. */
    public ApiException(int status, String code, String message, Map<String, String> fields) {
        super(message);
        this.status = status;
        this.code = code;
        this.fields = fields;
    }

    public int status() {
        return status;
    }

    public String code() {
        return code;
    }

    public Map<String, String> fields() {
        return fields;
    }
}
