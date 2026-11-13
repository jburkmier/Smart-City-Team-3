package edu.asu.smartcity.server.web;

import edu.asu.smartcity.server.domain.DepartmentManager;
import edu.asu.smartcity.server.domain.SystemUser;
import edu.asu.smartcity.server.dto.RequestDto;
import edu.asu.smartcity.server.model.RequestStatus;
import edu.asu.smartcity.server.model.RequestType;
import edu.asu.smartcity.server.model.Role;
import edu.asu.smartcity.server.service.RequestService;
import io.javalin.http.Context;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/** HTTP handlers for service-request lists. */
public class RequestController {

    private final RequestService requestService;
    private final AuthGuard guard;

    public RequestController(RequestService requestService, AuthGuard guard) {
        this.requestService = requestService;
        this.guard = guard;
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
