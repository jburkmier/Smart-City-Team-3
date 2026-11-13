package edu.asu.smartcity.server.model;

/** Service request statuses defined by docs/api.md. */
public enum RequestStatus {
    SUBMITTED,
    UNDER_REVIEW,
    ASSIGNED,
    IN_PROGRESS,
    RESOLVED,
    REJECTED
}
