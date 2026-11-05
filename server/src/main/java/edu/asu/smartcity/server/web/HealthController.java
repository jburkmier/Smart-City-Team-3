package edu.asu.smartcity.server.web;

import edu.asu.smartcity.server.db.DatabaseConnection;
import edu.asu.smartcity.server.dto.HealthResponse;
import io.javalin.http.Context;

import java.sql.Connection;
import java.sql.SQLException;

/** GET /api/health: the server is up; reports whether MySQL can be reached. */
public class HealthController {

    private static final int VALID_TIMEOUT_SECONDS = 2;

    public static void health(Context ctx) {
        boolean databaseUp;
        try (Connection conn = DatabaseConnection.instance().getConnection()) {
            databaseUp = conn.isValid(VALID_TIMEOUT_SECONDS);
        } catch (SQLException e) {
            databaseUp = false;
        }
        if (databaseUp) {
            ctx.json(new HealthResponse("UP", "UP"));
        } else {
            ctx.status(503).json(new HealthResponse("UP", "DOWN"));
        }
    }
}
