package edu.asu.smartcity.server.service;

import edu.asu.smartcity.server.dao.ReportDAO;
import edu.asu.smartcity.server.domain.CityAdministrator;
import edu.asu.smartcity.server.domain.DepartmentManager;
import edu.asu.smartcity.server.domain.Worker;
import edu.asu.smartcity.server.dto.DepartmentReportDto;
import edu.asu.smartcity.server.dto.EngagementReportDto;
import edu.asu.smartcity.server.model.RequestStatus;
import edu.asu.smartcity.server.model.RequestType;
import edu.asu.smartcity.server.web.ApiException;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ReportServiceTest {

    private static class FakeReportDAO extends ReportDAO {
        private final DepartmentReportDto departmentReport =
                new DepartmentReportDto(3, "Public Works", LocalDateTime.now(), 2,
                        Map.of(RequestStatus.ASSIGNED, 2), null, 0, java.util.List.of());
        private int requestedDepartmentId;

        FakeReportDAO() {
            super(null);
        }

        @Override
        public EngagementReportDto engagement() {
            return new EngagementReportDto(LocalDateTime.now(), 10, 8, 3, 1, 4,
                    Map.of(RequestStatus.SUBMITTED, 2), Map.of(RequestType.POTHOLE, 2), 0);
        }

        @Override
        public Optional<DepartmentReportDto> department(int departmentId) {
            requestedDepartmentId = departmentId;
            return departmentId == 99 ? Optional.empty()
                    : Optional.of(new DepartmentReportDto(departmentId, departmentReport.departmentName(),
                    departmentReport.generatedAt(), departmentReport.totalRequests(),
                    departmentReport.requestsByStatus(), departmentReport.averageResolutionHours(),
                    departmentReport.resolvedLast30Days(), departmentReport.openTasksByWorker()));
        }
    }

    private final FakeReportDAO dao = new FakeReportDAO();
    private final ReportService reports = new ReportService(dao);

    @Test
    public void cityAdministratorCanReadEngagementAndAnyDepartment() throws Exception {
        CityAdministrator admin = new CityAdministrator();

        assertEquals(10, reports.engagement(admin).totalUsers());
        assertEquals(8, reports.department(admin, 8).departmentId());
        assertEquals(8, dao.requestedDepartmentId);
    }

    @Test
    public void departmentManagerCanReadOnlyOwnDepartment() throws Exception {
        DepartmentManager manager = new DepartmentManager();
        manager.setDepartmentId(3);

        assertEquals(3, reports.department(manager, 3).departmentId());
        ApiException error = expectApiException(() -> reports.department(manager, 8));
        assertEquals(404, error.status());
        assertEquals(3, dao.requestedDepartmentId);
    }

    @Test
    public void unknownDepartmentReturnsNotFound() {
        ApiException error = expectApiException(() -> reports.department(new CityAdministrator(), 99));
        assertEquals(404, error.status());
    }

    @Test
    public void workersCannotReadReports() {
        ApiException departmentError = expectApiException(() -> reports.department(new Worker(), 3));
        ApiException engagementError = expectApiException(() -> reports.engagement(new Worker()));
        assertEquals(403, departmentError.status());
        assertEquals(403, engagementError.status());
    }

    private static ApiException expectApiException(ThrowingRunnable action) {
        try {
            action.run();
        } catch (ApiException e) {
            return e;
        } catch (Exception e) {
            throw new AssertionError("Expected ApiException but got " + e, e);
        }
        fail("Expected ApiException");
        return null;
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
