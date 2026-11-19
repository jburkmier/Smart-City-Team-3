package edu.asu.smartcity.server.web;

import edu.asu.smartcity.server.dto.AnnouncementCreateRequest;
import edu.asu.smartcity.server.model.Role;
import edu.asu.smartcity.server.service.AnnouncementService;
import io.javalin.http.Context;

import java.sql.SQLException;

/** HTTP handlers for announcements: everyone can read them; only city administrators post or delete. */
public class AnnouncementController {

    private final AnnouncementService announcementService;
    private final AuthGuard guard;

    public AnnouncementController(AnnouncementService announcementService, AuthGuard guard) {
        this.announcementService = announcementService;
        this.guard = guard;
    }

    /** GET /api/announcements: every announcement that hasn't been deleted, newest first. */
    public void list(Context ctx) throws SQLException {
        guard.requireUser(ctx);
        ctx.json(announcementService.findActive());
    }

    /** POST /api/announcements: posts an announcement and notifies every citizen. */
    public void create(Context ctx) throws SQLException {
        int administratorId = guard.requireRole(ctx, Role.CITY_ADMINISTRATOR).getUserId();
        ctx.status(201).json(announcementService.create(readRequest(ctx), administratorId));
    }

    /** PUT /api/announcements/{id}: edits content without resending the announcement notification. */
    public void update(Context ctx) throws SQLException {
        guard.requireRole(ctx, Role.CITY_ADMINISTRATOR);
        ctx.json(announcementService.update(announcementId(ctx), readRequest(ctx)));
    }

    /** DELETE /api/announcements/{id}: removes an announcement from the list; sent notifications stay. */
    public void delete(Context ctx) throws SQLException {
        guard.requireRole(ctx, Role.CITY_ADMINISTRATOR);
        announcementService.delete(announcementId(ctx));
        ctx.status(204);
    }

    private static AnnouncementCreateRequest readRequest(Context ctx) {
        try {
            return ctx.bodyAsClass(AnnouncementCreateRequest.class);
        } catch (Exception e) {
            throw new ApiException(400, "VALIDATION_FAILED", "The request body must be valid JSON.");
        }
    }

    private static int announcementId(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            if (id > 0) {
                return id;
            }
        } catch (NumberFormatException ignored) {
            // Invalid resource identifiers follow the API's 404 convention.
        }
        throw new ApiException(404, "NOT_FOUND", "Announcement not found.");
    }
}
