package edu.asu.smartcity.server.dto;

import edu.asu.smartcity.server.model.NotificationType;

import java.time.LocalDateTime;

/** A notification as sent to the client (docs/api.md, NotificationDto). */
public record NotificationDto(int id, NotificationType type, String message, Integer relatedId, boolean read,
                              LocalDateTime createdAt) {
}
