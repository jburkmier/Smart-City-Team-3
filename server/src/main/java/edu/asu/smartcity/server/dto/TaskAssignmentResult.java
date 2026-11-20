package edu.asu.smartcity.server.dto;

/** Result of creating or reassigning a request's single worker task. */
public record TaskAssignmentResult(TaskDto task, boolean created) {
}
