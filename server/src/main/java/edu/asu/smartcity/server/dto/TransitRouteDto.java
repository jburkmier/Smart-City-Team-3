package edu.asu.smartcity.server.dto;

/**
 * One scheduled trip as sent to the client (docs/api.md section 8). Times are "HH:mm" strings:
 * the bus or train leaves {@code stop} at departureTime and reaches the end of the line at arrivalTime.
 */
public record TransitRouteDto(int id, String route, String stop, String departureTime, String arrivalTime) {
}
