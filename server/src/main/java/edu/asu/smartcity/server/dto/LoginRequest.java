package edu.asu.smartcity.server.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Body of POST /api/auth/login. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LoginRequest(String email, String password) {
}
