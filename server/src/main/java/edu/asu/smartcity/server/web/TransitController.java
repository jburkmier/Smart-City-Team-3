package edu.asu.smartcity.server.web;

import edu.asu.smartcity.server.service.TransitService;
import io.javalin.http.Context;

import java.sql.SQLException;

/** HTTP handler for the public transit schedule. */
public class TransitController {

    private final TransitService transitService;
    private final AuthGuard guard;

    public TransitController(TransitService transitService, AuthGuard guard) {
        this.transitService = transitService;
        this.guard = guard;
    }

    /** GET /api/transit?route=: any logged-in user; route is optional. */
    public void list(Context ctx) throws SQLException {
        guard.requireUser(ctx);
        ctx.json(transitService.findSchedule(ctx.queryParam("route")));
    }
}
