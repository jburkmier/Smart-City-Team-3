package edu.asu.smartcity.server.service;

import edu.asu.smartcity.server.dao.ReportDAO;
import edu.asu.smartcity.server.domain.DepartmentManager;
import edu.asu.smartcity.server.domain.SystemUser;
import edu.asu.smartcity.server.dto.DepartmentReportDto;
import edu.asu.smartcity.server.dto.EngagementReportDto;
import edu.asu.smartcity.server.model.Role;
import edu.asu.smartcity.server.web.ApiException;

import java.sql.SQLException;
import java.util.Optional;

/** Enforces report permissions and coordinates computed report queries. */
public class ReportService {

    private final ReportDAO reports;

    public ReportService(ReportDAO reports) {
        this.reports = reports;
    }

    public EngagementReportDto engagement(SystemUser caller) throws SQLException {
        if (caller.getRole() != Role.CITY_ADMINISTRATOR) {
            throw forbidden();
        }
        return reports.engagement();
    }

    public DepartmentReportDto department(SystemUser caller, int departmentId) throws SQLException {
        if (caller.getRole() == Role.DEPARTMENT_MANAGER) {
            if (!(caller instanceof DepartmentManager manager)
                    || manager.getDepartmentId() == null || manager.getDepartmentId() != departmentId) {
                throw notFound();
            }
        } else if (caller.getRole() != Role.CITY_ADMINISTRATOR) {
            throw forbidden();
        }
        Optional<DepartmentReportDto> report = reports.department(departmentId);
        return report.orElseThrow(ReportService::notFound);
    }

    private static ApiException forbidden() {
        return new ApiException(403, "FORBIDDEN", "You don't have permission to view this report.");
    }

    private static ApiException notFound() {
        return new ApiException(404, "NOT_FOUND", "Department report not found.");
    }
}
