package edu.asu.smartcity.server.dto;

import edu.asu.smartcity.server.model.RequestStatus;
import edu.asu.smartcity.server.model.RequestType;

import java.time.LocalDateTime;

/** A service request as sent to the client (docs/api.md, RequestDto). */
public record RequestDto(int id, RequestType type, String description, String location, RequestStatus status,
                         int citizenId, String citizenName, Integer departmentId, String departmentName,
                         Integer assignedWorkerId, String assignedWorkerName, String rejectionReason,
                         String resolutionNotes, LocalDateTime submittedAt, LocalDateTime updatedAt,
                         LocalDateTime resolvedAt) {
}
