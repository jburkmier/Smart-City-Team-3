package edu.asu.smartcity.server.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Body of POST /api/auth/register. Address and phone are optional (may be null).
 * There is no role field: registering always creates a CITIZEN, and a "role" sent by the
 * client is ignored (ignoreUnknown) instead of rejected.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RegisterRequest(String name, String email, String password, String address, String phone) {
}
