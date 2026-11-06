package edu.asu.smartcity.server.web;

import edu.asu.smartcity.server.db.DatabaseConnection;
import edu.asu.smartcity.server.dto.HealthResponse;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

/** GET /api/health: the server is up; reports whether MySQL can be reached. */
public class HealthController {

    private static final Logger LOG = LoggerFactory.getLogger(HealthController.class);
    private static final int VALID_TIMEOUT_SECONDS = 2;

    private final DatabaseConnection database;

    public HealthController(DatabaseConnection database) {
        this.database = database;
    }

    public void health(Context ctx) {
        boolean databaseUp;
        try (Connection conn = database.getConnection()) {
            databaseUp = conn.isValid(VALID_TIMEOUT_SECONDS);
        } catch (SQLException e) {
            LOG.warn("Health check: database unreachable: {}", e.getMessage());
            databaseUp = false;
        }
        if (databaseUp) {
            ctx.json(new HealthResponse("UP", "UP"));
        } else {
            ctx.status(503).json(new HealthResponse("UP", "DOWN"));
        }
    }
}
