package edu.asu.smartcity.server.dto;

import edu.asu.smartcity.server.model.RequestStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Department service-request performance metrics computed when requested. */
public record DepartmentReportDto(int departmentId, String departmentName, LocalDateTime generatedAt,
                                  int totalRequests, Map<RequestStatus, Integer> requestsByStatus,
                                  Double averageResolutionHours, int resolvedLast30Days,
                                  List<WorkerTaskLoadDto> openTasksByWorker) {
}
