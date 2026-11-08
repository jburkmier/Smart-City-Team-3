package edu.asu.smartcity.server.web;

import edu.asu.smartcity.server.dto.LoginRequest;
import edu.asu.smartcity.server.dto.RegisterRequest;
import edu.asu.smartcity.server.service.AuthService;
import edu.asu.smartcity.server.service.UserMapper;
import io.javalin.http.Context;

import java.sql.SQLException;

/** HTTP handlers for /api/auth/* and /api/me. Parses the request, then hands off to AuthService. */
public class AuthController {

    private final AuthService auth;
    private final AuthGuard guard;

    public AuthController(AuthService auth, AuthGuard guard) {
        this.auth = auth;
        this.guard = guard;
    }

    /** POST /api/auth/register: 201 with the new citizen. */
    public void register(Context ctx) throws SQLException {
        RegisterRequest request = readBody(ctx, RegisterRequest.class);
        ctx.status(201).json(auth.register(request));
    }

    /** POST /api/auth/login: 200 with the token and user. */
    public void login(Context ctx) throws SQLException {
        LoginRequest request = readBody(ctx, LoginRequest.class);
        ctx.json(auth.login(request));
    }

    /** POST /api/auth/logout: always 204, even if the token was already invalid. */
    public void logout(Context ctx) {
        auth.logout(AuthGuard.bearerToken(ctx));
        ctx.status(204);
    }

    /** GET /api/me: the logged-in user. */
    public void me(Context ctx) {
        ctx.json(UserMapper.toDto(guard.requireUser(ctx)));
    }

    /** Parses the JSON body, or 400 VALIDATION_FAILED if it is missing or not valid JSON. */
    private static <T> T readBody(Context ctx, Class<T> type) {
        T body;
        try {
            body = ctx.bodyAsClass(type);
        } catch (Exception e) {
            // Jackson's parse errors are checked exceptions that Javalin rethrows undeclared,
            // so catching RuntimeException alone would miss them.
            body = null;
        }
        if (body == null) {
            throw new ApiException(400, "VALIDATION_FAILED", "The request body must be valid JSON.");
        }
        return body;
    }
}
