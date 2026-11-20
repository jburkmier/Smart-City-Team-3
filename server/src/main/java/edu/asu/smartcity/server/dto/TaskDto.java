package edu.asu.smartcity.server.dto;

import java.time.LocalDateTime;

/** A worker assignment and the request it represents. */
public record TaskDto(int id, int requestId, RequestDto request, int workerId, String workerName,
                      int assignedById, String notes, LocalDateTime assignedAt) {
}
