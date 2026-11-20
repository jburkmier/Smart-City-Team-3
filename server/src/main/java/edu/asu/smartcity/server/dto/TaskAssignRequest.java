package edu.asu.smartcity.server.dto;

/** Body of PUT /api/requests/{id}/task. */
public record TaskAssignRequest(Integer workerId, String notes) {
}
