package edu.asu.smartcity.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import edu.asu.smartcity.server.dao.EventDAO;
import edu.asu.smartcity.server.dao.RequestDAO;
import edu.asu.smartcity.server.dao.UserDAO;
import edu.asu.smartcity.server.db.DatabaseConnection;
import edu.asu.smartcity.server.dto.ErrorResponse;
import edu.asu.smartcity.server.service.AuthService;
import edu.asu.smartcity.server.service.EventService;
import edu.asu.smartcity.server.service.RequestService;
import edu.asu.smartcity.server.session.SessionManager;
import edu.asu.smartcity.server.web.ApiException;
import edu.asu.smartcity.server.web.AuthController;
import edu.asu.smartcity.server.web.AuthGuard;
import edu.asu.smartcity.server.web.EventController;
import edu.asu.smartcity.server.web.HealthController;
import edu.asu.smartcity.server.web.RequestController;
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
        SessionManager sessions = SessionManager.instance();
        UserDAO userDAO = new UserDAO(database);
        AuthService authService = new AuthService(userDAO, sessions);
        RequestService requestService = new RequestService(new RequestDAO(database));
        AuthGuard authGuard = new AuthGuard(sessions);

        HealthController healthController = new HealthController(database);
        AuthController authController = new AuthController(authService, authGuard);
        EventDAO eventDAO = new EventDAO(database);
        EventController eventController = new EventController(new EventService(eventDAO), authGuard);
        RequestController requestController = new RequestController(requestService, authGuard);

        Javalin app = Javalin.create(config -> {
            config.staticFiles.add("/public", Location.CLASSPATH);
            config.jsonMapper(new JavalinJackson(objectMapper(), false));
        });

        app.get("/api/health", healthController::health);
        app.get("/api/events", eventController::list);
        app.get("/api/events/{id}", eventController::get);
        app.post("/api/events", eventController::create);
        app.put("/api/events/{id}", eventController::update);
        app.delete("/api/events/{id}", eventController::cancel);
        app.post("/api/events/{id}/registration", eventController::register);
        app.delete("/api/events/{id}/registration", eventController::unregister);
        app.get("/api/me/registrations", eventController::registrations);
        app.post("/api/auth/register", authController::register);
        app.post("/api/auth/login", authController::login);
        app.post("/api/auth/logout", authController::logout);
        app.get("/api/me", authController::me);
        app.get("/api/me/requests", requestController::mine);
        app.get("/api/requests", requestController::list);
        app.post("/api/requests", requestController::create);

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
