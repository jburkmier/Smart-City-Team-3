package edu.asu.smartcity.server.web;

import edu.asu.smartcity.server.dto.EventCreateRequest;
import edu.asu.smartcity.server.model.Role;
import edu.asu.smartcity.server.service.EventService;
import io.javalin.http.Context;

import java.sql.SQLException;
import java.util.Map;

/** HTTP handlers for event browsing, management, and registration. */
public class EventController {

    private final EventService eventService;
    private final AuthGuard guard;

    public EventController(EventService eventService, AuthGuard guard) {
        this.eventService = eventService;
        this.guard = guard;
    }

    /** GET /api/events: upcoming, non-cancelled, non-hidden events. */
    public void list(Context ctx) throws SQLException {
        int userId = guard.requireUser(ctx).getUserId();
        ctx.json(eventService.findUpcoming(userId, ctx.queryParam("q"), ctx.queryParam("category"),
                ctx.queryParam("from"), ctx.queryParam("to"), ctx.queryParam("district"),
                ctx.queryParam("free"), ctx.queryParam("sort")));
    }

    /** GET /api/events/{id}: one publicly visible event. */
    public void get(Context ctx) throws SQLException {
        int userId = guard.requireUser(ctx).getUserId();
        ctx.json(eventService.findVisibleById(eventId(ctx), userId));
    }

    /** POST /api/events: creates an event for the authenticated city administrator. */
    public void create(Context ctx) throws SQLException {
        int userId = guard.requireRole(ctx, Role.CITY_ADMINISTRATOR).getUserId();
        ctx.status(201).json(eventService.create(readRequest(ctx), userId));
    }

    /** PUT /api/events/{id}: updates an active event. */
    public void update(Context ctx) throws SQLException {
        int userId = guard.requireRole(ctx, Role.CITY_ADMINISTRATOR).getUserId();
        ctx.json(eventService.update(eventId(ctx), readRequest(ctx), userId));
    }

    /** DELETE /api/events/{id}: soft-cancels an event. */
    public void cancel(Context ctx) throws SQLException {
        guard.requireRole(ctx, Role.CITY_ADMINISTRATOR);
        eventService.cancel(eventId(ctx));
        ctx.status(204);
    }

    /** POST /api/events/{id}/registration: registers the authenticated citizen. */
    public void register(Context ctx) throws SQLException {
        int userId = guard.requireRole(ctx, Role.CITIZEN).getUserId();
        ctx.status(201).json(eventService.register(eventId(ctx), userId));
    }

    /** DELETE /api/events/{id}/registration: removes the caller's registration. */
    public void unregister(Context ctx) throws SQLException {
        int userId = guard.requireRole(ctx, Role.CITIZEN).getUserId();
        eventService.unregister(eventId(ctx), userId);
        ctx.status(204);
    }

    /** GET /api/me/registrations: all visible registrations, optionally upcoming only. */
    public void registrations(Context ctx) throws SQLException {
        int userId = guard.requireRole(ctx, Role.CITIZEN).getUserId();
        String upcomingParam = ctx.queryParam("upcoming");
        boolean upcoming = false;
        if (upcomingParam != null) {
            if (!"true".equalsIgnoreCase(upcomingParam) && !"false".equalsIgnoreCase(upcomingParam)) {
                throw new ApiException(400, "VALIDATION_FAILED",
                        "The upcoming parameter must be true or false.",
                        Map.of("upcoming", "Must be true or false."));
            }
            upcoming = "true".equalsIgnoreCase(upcomingParam);
        }
        ctx.json(eventService.findRegistrations(userId, upcoming));
    }

    private static EventCreateRequest readRequest(Context ctx) {
        try {
            return ctx.bodyAsClass(EventCreateRequest.class);
        } catch (Exception e) {
            throw new ApiException(400, "VALIDATION_FAILED", "The request body must be valid JSON.");
        }
    }

    private static int eventId(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            if (id > 0) {
                return id;
            }
        } catch (NumberFormatException ignored) {
            // Invalid resource identifiers follow the API's 404 convention.
        }
        throw new ApiException(404, "NOT_FOUND", "Event not found.");
    }
}
