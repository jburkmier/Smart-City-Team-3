package edu.asu.smartcity.server.web;

import edu.asu.smartcity.server.model.Role;
import edu.asu.smartcity.server.service.DepartmentService;
import io.javalin.http.Context;

import java.sql.SQLException;

/** HTTP handlers for departments and their workers. */
public class DepartmentController {

    private final DepartmentService departmentService;
    private final AuthGuard guard;

    public DepartmentController(DepartmentService departmentService, AuthGuard guard) {
        this.departmentService = departmentService;
        this.guard = guard;
    }

    /** GET /api/departments: every department, for the admin's "assign to department" dropdown. */
    public void list(Context ctx) throws SQLException {
        guard.requireRole(ctx, Role.CITY_ADMINISTRATOR);
        ctx.json(departmentService.findAll());
    }

    /** GET /api/departments/{id}/workers: a department's workers, for the "assign worker" dropdown. */
    public void workers(Context ctx) throws SQLException {
        var caller = guard.requireRole(ctx, Role.CITY_ADMINISTRATOR, Role.DEPARTMENT_MANAGER);
        ctx.json(departmentService.findWorkers(caller, departmentId(ctx)));
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
        throw new ApiException(404, "NOT_FOUND", "Department not found.");
    }
}
