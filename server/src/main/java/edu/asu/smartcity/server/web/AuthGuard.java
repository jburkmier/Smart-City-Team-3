package edu.asu.smartcity.server.web;

import edu.asu.smartcity.server.domain.SystemUser;
import edu.asu.smartcity.server.model.Role;
import edu.asu.smartcity.server.session.SessionManager;
import io.javalin.http.Context;

import java.util.Arrays;

/**
 * Server-side access checks. Call at the top of every protected handler:
 * hiding buttons in the browser is not enough, because requests can be sent by hand.
 */
public class AuthGuard {

    private static final String BEARER = "Bearer ";

    private final SessionManager sessions;

    public AuthGuard(SessionManager sessions) {
        this.sessions = sessions;
    }

    /** The logged-in user, or 401 UNAUTHORIZED if the token is missing, unknown or logged out. */
    public SystemUser requireUser(Context ctx) {
        SystemUser user = sessions.getUser(bearerToken(ctx));
        if (user == null) {
            throw new ApiException(401, "UNAUTHORIZED", "Please log in.");
        }
        return user;
    }

    /** The logged-in user if their role is one of the allowed roles; otherwise 401 or 403 FORBIDDEN. */
    public SystemUser requireRole(Context ctx, Role... allowed) {
        SystemUser user = requireUser(ctx);
        if (!Arrays.asList(allowed).contains(user.getRole())) {
            throw new ApiException(403, "FORBIDDEN", "You don't have permission to do this.");
        }
        return user;
    }

    /** The token from "Authorization: Bearer token", or null if the header is missing or malformed. */
    public static String bearerToken(Context ctx) {
        String header = ctx.header("Authorization");
        if (header == null || !header.startsWith(BEARER)) {
            return null;
        }
        String token = header.substring(BEARER.length()).trim();
        return token.isEmpty() ? null : token;
    }
}
