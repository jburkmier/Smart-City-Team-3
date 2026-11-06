package edu.asu.smartcity.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import edu.asu.smartcity.server.db.DatabaseConnection;
import edu.asu.smartcity.server.dto.ErrorResponse;
import edu.asu.smartcity.server.web.ApiException;
import edu.asu.smartcity.server.web.HealthController;
import io.javalin.Javalin;
import io.javalin.http.NotFoundResponse;
import io.javalin.http.staticfiles.Location;
import io.javalin.json.JavalinJackson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

/**
 * Entry point for SmartCity Hub.
 * Serves the web pages from src/main/resources/public (open http://localhost:7070)
 * and the REST API under /api (docs/api.md).
 */
public class ServerApp {

    public static final int PORT = 7070;

    private static final Logger LOG = LoggerFactory.getLogger(ServerApp.class);

    public static void main(String[] args) {
        createApp().start(PORT);
    }

    /**
     * Builds the Javalin app: creates every object once, hands each one what it needs
     * through its constructor, then registers the routes and error handlers.
     */
    public static Javalin createApp() {
        // Reads db.properties now, so a missing file stops startup instead of the first request.
        DatabaseConnection database = DatabaseConnection.instance();
        HealthController healthController = new HealthController(database);

        Javalin app = Javalin.create(config -> {
            config.staticFiles.add("/public", Location.CLASSPATH);
            config.jsonMapper(new JavalinJackson(objectMapper(), false));
        });

        app.get("/api/health", healthController::health);

        // Javalin throws NotFoundResponse when no route or page matches. Unknown /api paths get
        // the api.md error format; missing pages keep Javalin's plain-text 404.
        app.exception(NotFoundResponse.class, (e, ctx) -> {
            ctx.status(404);
            if (ctx.path().startsWith("/api/")) {
                ctx.json(new ErrorResponse("NOT_FOUND", "No such endpoint."));
            } else {
                ctx.result(e.getMessage());
            }
        });

        app.exception(ApiException.class, (e, ctx) ->
                ctx.status(e.status()).json(new ErrorResponse(e.code(), e.getMessage(), e.fields())));
        app.exception(SQLException.class, (e, ctx) -> {
            // SQLState class 08 means the connection to MySQL failed.
            if (e.getSQLState() != null && e.getSQLState().startsWith("08")) {
                LOG.warn("Database unavailable: {}", e.getMessage());
                ctx.status(503).json(new ErrorResponse("DATABASE_UNAVAILABLE", "The database is unavailable."));
            } else {
                LOG.error("Database error", e);
                ctx.status(500).json(new ErrorResponse("INTERNAL_ERROR", "Something went wrong."));
            }
        });
        app.exception(Exception.class, (e, ctx) -> {
            LOG.error("Unexpected error", e);
            ctx.status(500).json(new ErrorResponse("INTERNAL_ERROR", "Something went wrong."));
        });
        return app;
    }

    /** JSON settings: dates as ISO-8601 strings such as "2026-11-05T18:00:00", not numbers. */
    static ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
