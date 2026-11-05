package edu.asu.smartcity.server.dto;

/** Response of POST /api/auth/login: the session token and the logged-in user. */
public record LoginResponse(String token, UserDto user) {
}
