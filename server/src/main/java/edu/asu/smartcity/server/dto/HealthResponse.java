package edu.asu.smartcity.server.dto;

/** Response of GET /api/health. Each value is "UP" or "DOWN". */
public record HealthResponse(String status, String database) {
}
