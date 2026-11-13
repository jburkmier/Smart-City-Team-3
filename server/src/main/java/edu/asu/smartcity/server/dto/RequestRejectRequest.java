package edu.asu.smartcity.server.dto;

/** Body of POST /requests/{id}/reject. */
public record RequestRejectRequest(String reason) {
}
