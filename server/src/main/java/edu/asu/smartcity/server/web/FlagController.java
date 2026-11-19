package edu.asu.smartcity.server.web;

import edu.asu.smartcity.server.domain.SystemUser;
import edu.asu.smartcity.server.dto.FlagCreateRequest;
import edu.asu.smartcity.server.model.FlagStatus;
import edu.asu.smartcity.server.model.Role;
import edu.asu.smartcity.server.service.FlagService;
import io.javalin.http.Context;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;

/** HTTP handlers for reporting and administrator moderation of content flags. */
public class FlagController {

    private final FlagService flags;
    private final AuthGuard guard;

    public FlagController(FlagService flags, AuthGuard guard) {
        this.flags = flags;
        this.guard = guard;
    }

    public void create(Context ctx) throws SQLException {
        SystemUser reporter = guard.requireUser(ctx);
        ctx.status(201).json(flags.create(readRequest(ctx), reporter));
    }

    public void list(Context ctx) throws SQLException {
        guard.requireRole(ctx, Role.CITY_ADMINISTRATOR);
        ctx.json(flags.findAll(parseStatus(ctx.queryParam("status"))));
    }

    public void dismiss(Context ctx) throws SQLException {
        guard.requireRole(ctx, Role.CITY_ADMINISTRATOR);
        ctx.json(flags.dismiss(flagId(ctx)));
    }

    public void remove(Context ctx) throws SQLException {
        guard.requireRole(ctx, Role.CITY_ADMINISTRATOR);
        ctx.json(flags.remove(flagId(ctx)));
    }

    private static FlagCreateRequest readRequest(Context ctx) {
        try {
            return ctx.bodyAsClass(FlagCreateRequest.class);
        } catch (Exception e) {
            throw new ApiException(400, "VALIDATION_FAILED", "The request body must be valid JSON.");
        }
    }

    private static FlagStatus parseStatus(String value) {
        if (value == null || value.isBlank()) {
            return FlagStatus.OPEN;
        }
        try {
            return FlagStatus.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new ApiException(400, "VALIDATION_FAILED", "Some fields are invalid.",
                    Map.of("status", "Must be one of: "
                            + String.join(", ", Arrays.stream(FlagStatus.values()).map(Enum::name).toList()) + "."));
        }
    }

    private static int flagId(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            if (id > 0) {
                return id;
            }
        } catch (NumberFormatException ignored) {
            // Invalid resource identifiers follow the API's 404 convention.
        }
        throw new ApiException(404, "NOT_FOUND", "Flag not found.");
    }
}
