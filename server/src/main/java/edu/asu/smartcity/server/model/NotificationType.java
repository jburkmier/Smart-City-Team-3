package edu.asu.smartcity.server.model;

/** Notification types defined by docs/api.md (section 7, Notifications). */
public enum NotificationType {
    ANNOUNCEMENT,
    EMERGENCY_ALERT,
    REQUEST_STATUS,
    TASK_ASSIGNED,
    EVENT_CANCELLED,
    FLAG_SUBMITTED
}
