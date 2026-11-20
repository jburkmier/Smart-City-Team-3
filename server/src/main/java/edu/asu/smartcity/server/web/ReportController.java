package edu.asu.smartcity.server.web;

import edu.asu.smartcity.server.model.Role;
import edu.asu.smartcity.server.service.ReportService;
import io.javalin.http.Context;

import java.sql.SQLException;

/** HTTP handlers for computed engagement and department reports. */
public class ReportController {

    private final ReportService reports;
    private final AuthGuard guard;

    public ReportController(ReportService reports, AuthGuard guard) {
        this.reports = reports;
        this.guard = guard;
    }

    /** GET /api/reports/engagement: citywide statistics for a city administrator. */
    public void engagement(Context ctx) throws SQLException {
        ctx.json(reports.engagement(guard.requireRole(ctx, Role.CITY_ADMINISTRATOR)));
    }

    /** GET /api/reports/departments/{id}: department statistics within the caller's scope. */
    public void department(Context ctx) throws SQLException {
        var caller = guard.requireRole(ctx, Role.CITY_ADMINISTRATOR, Role.DEPARTMENT_MANAGER);
        ctx.json(reports.department(caller, departmentId(ctx)));
    }

    private static int departmentId(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            if (id > 0) {
                return id;
            }
        } catch (NumberFormatException ignored) {
            // Invalid resource identifiers follow the API's 404 convention.
        }
        throw new ApiException(404, "NOT_FOUND", "Department report not found.");
    }
}
