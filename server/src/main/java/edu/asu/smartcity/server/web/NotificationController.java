package edu.asu.smartcity.server.web;

import edu.asu.smartcity.server.service.NotificationService;
import io.javalin.http.Context;

import java.sql.SQLException;
import java.util.Map;

/**
 * HTTP handlers for the caller's notifications. Every logged-in role may call them (docs/api.md);
 * each user only ever sees and changes their own notifications.
 */
public class NotificationController {

    private final NotificationService notificationService;
    private final AuthGuard guard;

    public NotificationController(NotificationService notificationService, AuthGuard guard) {
        this.notificationService = notificationService;
        this.guard = guard;
    }

    /** GET /api/me/notifications: the caller's notifications, newest first; ?unreadOnly=true for unread only. */
    public void list(Context ctx) throws SQLException {
        int userId = guard.requireUser(ctx).getUserId();
        ctx.json(notificationService.findMine(userId, unreadOnly(ctx.queryParam("unreadOnly"))));
    }

    /** POST /api/me/notifications/{id}/read: marks one of the caller's notifications read. */
    public void markRead(Context ctx) throws SQLException {
        int userId = guard.requireUser(ctx).getUserId();
        notificationService.markRead(userId, notificationId(ctx));
        ctx.status(204);
    }

    /** POST /api/me/notifications/read-all: marks all of the caller's notifications read. */
    public void markAllRead(Context ctx) throws SQLException {
        int userId = guard.requireUser(ctx).getUserId();
        notificationService.markAllRead(userId);
        ctx.status(204);
    }

    private static boolean unreadOnly(String value) {
        if (value == null) {
            return false;
        }
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        throw new ApiException(400, "VALIDATION_FAILED", "The unreadOnly parameter must be true or false.",
                Map.of("unreadOnly", "Must be true or false."));
    }

    private static int notificationId(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            if (id > 0) {
                return id;
            }
        } catch (NumberFormatException ignored) {
            // Invalid resource identifiers follow the API's 404 convention.
        }
        throw new ApiException(404, "NOT_FOUND", "Notification not found.");
    }
}
