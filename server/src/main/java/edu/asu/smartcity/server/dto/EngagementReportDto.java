package edu.asu.smartcity.server.dto;

import edu.asu.smartcity.server.model.RequestStatus;
import edu.asu.smartcity.server.model.RequestType;

import java.time.LocalDateTime;
import java.util.Map;

/** Citywide engagement metrics computed when the report is requested. */
public record EngagementReportDto(LocalDateTime generatedAt, int totalUsers, int totalCitizens,
                                  int activeUsersLast7Days, int upcomingEvents, int totalEventRegistrations,
                                  Map<RequestStatus, Integer> requestsByStatus,
                                  Map<RequestType, Integer> requestsByType, int openFlags) {
}
