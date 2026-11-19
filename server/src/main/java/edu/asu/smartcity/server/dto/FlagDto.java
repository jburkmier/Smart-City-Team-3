package edu.asu.smartcity.server.dto;

import edu.asu.smartcity.server.model.FlagContentType;
import edu.asu.smartcity.server.model.FlagStatus;

import java.time.LocalDateTime;

public record FlagDto(int id, FlagContentType contentType, int contentId, String contentSummary,
                      String reason, int flaggedById, String flaggedByName, FlagStatus status,
                      LocalDateTime createdAt) {
}
