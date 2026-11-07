package edu.asu.smartcity.server.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * The body of every error response (docs/api.md section 1, "Error format").
 * error is a fixed code such as VALIDATION_FAILED; message is text the client can show as-is.
 * fields maps input names to problems and is only present for VALIDATION_FAILED
 * (left out of the JSON when null).
 */
public record ErrorResponse(String error, String message,
                            @JsonInclude(JsonInclude.Include.NON_NULL) Map<String, String> fields) {

    /** An error without per-field details. */
    public ErrorResponse(String error, String message) {
        this(error, message, null);
    }
}
