package edu.asu.smartcity.server.dto;

/** Body of POST /api/requests/{id}/resolve. */
public record RequestResolveRequest(String resolutionNotes) {
}
