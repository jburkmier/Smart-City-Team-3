package edu.asu.smartcity.server.web;

import edu.asu.smartcity.server.domain.DepartmentManager;
import edu.asu.smartcity.server.domain.SystemUser;
import edu.asu.smartcity.server.dto.RequestAssignRequest;
import edu.asu.smartcity.server.dto.RequestCreateRequest;
import edu.asu.smartcity.server.dto.RequestDto;
import edu.asu.smartcity.server.dto.RequestRejectRequest;
import edu.asu.smartcity.server.dto.RequestResolveRequest;
import edu.asu.smartcity.server.dto.TaskAssignRequest;
import edu.asu.smartcity.server.model.RequestStatus;
import edu.asu.smartcity.server.model.RequestType;
import edu.asu.smartcity.server.model.Role;
import edu.asu.smartcity.server.service.RequestService;
import io.javalin.http.Context;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/** HTTP handlers for service-request submission and lists. */
public class RequestController {

    private final RequestService requestService;
    private final AuthGuard guard;

    public RequestController(RequestService requestService, AuthGuard guard) {
        this.requestService = requestService;
        this.guard = guard;
    }

    /** POST /api/requests: submits a service request for the authenticated citizen. */
    public void create(Context ctx) throws SQLException {
        int citizenId = guard.requireRole(ctx, Role.CITIZEN).getUserId();
        ctx.status(201).json(requestService.create(readRequest(ctx), citizenId));
    }

    /** PUT /api/requests/{id}: city administrators can edit request content without changing its status. */
    public void update(Context ctx) throws SQLException {
        guard.requireRole(ctx, Role.CITY_ADMINISTRATOR);
        ctx.json(requestService.updateContent(requestId(ctx), readRequest(ctx)));
    }

    /** POST /api/requests/{id}/review: moves a submitted request to under review. */
    public void review(Context ctx) throws SQLException {
        guard.requireRole(ctx, Role.CITY_ADMINISTRATOR);
        ctx.json(requestService.review(requestId(ctx)));
    }

    /** POST /api/requests/{id}/assign: assigns an under-review request to a department. */
    public void assign(Context ctx) throws SQLException {
        guard.requireRole(ctx, Role.CITY_ADMINISTRATOR);
        ctx.json(requestService.assign(requestId(ctx), readAssignment(ctx)));
    }

    /** POST /api/requests/{id}/reject: rejects an under-review request with a reason. */
    public void reject(Context ctx) throws SQLException {
        guard.requireRole(ctx, Role.CITY_ADMINISTRATOR);
        ctx.json(requestService.reject(requestId(ctx), readRejection(ctx)));
    }

    /** PUT /api/requests/{id}/task: assigns or reassigns a worker within the manager's department. */
    public void assignTask(Context ctx) throws SQLException {
        SystemUser manager = guard.requireRole(ctx, Role.DEPARTMENT_MANAGER);
        var result = requestService.assignTask(requestId(ctx), readTaskAssignment(ctx), manager);
        ctx.status(result.created() ? 201 : 200).json(result.task());
    }

    /** GET /api/me/tasks: the authenticated worker's assigned tasks. */
    public void myTasks(Context ctx) throws SQLException {
        int workerId = guard.requireRole(ctx, Role.WORKER).getUserId();
        ctx.json(requestService.findMyTasks(workerId));
    }

    /** POST /api/requests/{id}/start: begins work on an assigned request. */
    public void start(Context ctx) throws SQLException {
        SystemUser caller = guard.requireRole(ctx, Role.WORKER, Role.DEPARTMENT_MANAGER);
        ctx.json(requestService.start(requestId(ctx), caller));
    }

    /** POST /api/requests/{id}/resolve: resolves an in-progress request. */
    public void resolve(Context ctx) throws SQLException {
        SystemUser caller = guard.requireRole(ctx, Role.WORKER, Role.DEPARTMENT_MANAGER);
        ctx.json(requestService.resolve(requestId(ctx), readResolution(ctx), caller));
    }

    /** GET /api/me/requests: the authenticated citizen's requests. */
    public void mine(Context ctx) throws SQLException {
        int citizenId = guard.requireRole(ctx, Role.CITIZEN).getUserId();
        ctx.json(requestService.findByCitizen(citizenId));
    }

    /** GET /api/requests: all administrator requests or the manager's own department requests. */
    public void list(Context ctx) throws SQLException {
        SystemUser caller = guard.requireRole(ctx, Role.CITY_ADMINISTRATOR, Role.DEPARTMENT_MANAGER);
        if (caller.getRole() == Role.DEPARTMENT_MANAGER
                && ctx.queryParam("departmentId") != null
                && !ctx.queryParam("departmentId").isBlank()) {
            throw new ApiException(403, "FORBIDDEN", "Only a city administrator can filter by department.");
        }
        RequestStatus status = parseEnum(ctx.queryParam("status"), RequestStatus.class, "status");
        RequestType type = parseEnum(ctx.queryParam("type"), RequestType.class, "type");
        Integer departmentFilter = parseDepartmentId(ctx.queryParam("departmentId"));

        List<RequestDto> requests;
        if (caller.getRole() == Role.CITY_ADMINISTRATOR) {
            requests = requestService.findForAdministrator(status, type, departmentFilter);
        } else {
            DepartmentManager manager = (DepartmentManager) caller;
            Integer departmentId = manager.getDepartmentId();
            if (departmentId == null) {
                throw new ApiException(403, "FORBIDDEN", "Your account is not assigned to a department.");
            }
            requests = requestService.findForDepartmentManager(departmentId, status, type);
        }
        ctx.json(requests);
    }

    private static RequestCreateRequest readRequest(Context ctx) {
        try {
            RequestCreateRequest request = ctx.bodyAsClass(RequestCreateRequest.class);
            if (request != null) {
                return request;
            }
        } catch (Exception e) {
            throw new ApiException(400, "VALIDATION_FAILED", "The request body must be valid JSON.");
        }
        throw new ApiException(400, "VALIDATION_FAILED", "The request body must be valid JSON.");
    }

    private static RequestAssignRequest readAssignment(Context ctx) {
        try {
            RequestAssignRequest assignment = ctx.bodyAsClass(RequestAssignRequest.class);
            if (assignment != null) {
                return assignment;
            }
        } catch (Exception e) {
            throw new ApiException(400, "VALIDATION_FAILED", "The request body must be valid JSON.");
        }
        throw new ApiException(400, "VALIDATION_FAILED", "The request body must be valid JSON.");
    }

    private static RequestRejectRequest readRejection(Context ctx) {
        try {
            RequestRejectRequest rejection = ctx.bodyAsClass(RequestRejectRequest.class);
            if (rejection != null) {
                return rejection;
            }
        } catch (Exception e) {
            throw new ApiException(400, "VALIDATION_FAILED", "The request body must be valid JSON.");
        }
        throw new ApiException(400, "VALIDATION_FAILED", "The request body must be valid JSON.");
    }

    private static TaskAssignRequest readTaskAssignment(Context ctx) {
        try {
            TaskAssignRequest assignment = ctx.bodyAsClass(TaskAssignRequest.class);
            if (assignment != null) {
                return assignment;
            }
        } catch (Exception e) {
            throw new ApiException(400, "VALIDATION_FAILED", "The request body must be valid JSON.");
        }
        throw new ApiException(400, "VALIDATION_FAILED", "The request body must be valid JSON.");
    }

    private static RequestResolveRequest readResolution(Context ctx) {
        if (ctx.body().isBlank()) {
            return new RequestResolveRequest(null);
        }
        try {
            RequestResolveRequest resolution = ctx.bodyAsClass(RequestResolveRequest.class);
            if (resolution != null) {
                return resolution;
            }
        } catch (Exception e) {
            throw new ApiException(400, "VALIDATION_FAILED", "The request body must be valid JSON.");
        }
        throw new ApiException(400, "VALIDATION_FAILED", "The request body must be valid JSON.");
    }

    private static Integer parseDepartmentId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            int departmentId = Integer.parseInt(value);
            if (departmentId > 0) {
                return departmentId;
            }
        } catch (NumberFormatException ignored) {
            // Invalid filter values are reported below using the API validation format.
        }
        throw new ApiException(400, "VALIDATION_FAILED", "Some fields are invalid.",
                Map.of("departmentId", "Must be a positive integer."));
    }

    private static int requestId(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            if (id > 0) {
                return id;
            }
        } catch (NumberFormatException ignored) {
            // Invalid resource identifiers follow the API's 404 convention.
        }
        throw new ApiException(404, "NOT_FOUND", "Request not found.");
    }

    private static <E extends Enum<E>> E parseEnum(String value, Class<E> enumType, String field) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(enumType, value);
        } catch (IllegalArgumentException e) {
            String allowed = String.join(", ", java.util.Arrays.stream(enumType.getEnumConstants())
                    .map(Enum::name)
                    .toList());
            throw new ApiException(400, "VALIDATION_FAILED", "Some fields are invalid.",
                    Map.of(field, "Must be one of: " + allowed + "."));
        }
    }
}
