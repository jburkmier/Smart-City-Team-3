package edu.asu.smartcity.server.session;

import edu.asu.smartcity.server.domain.SystemUser;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton pattern: the one record of who is logged in.
 * Each login gets a random token; the client sends it back as "Authorization: Bearer token".
 * Sessions live in memory only, so a server restart logs everyone out.
 */
public class SessionManager {

    private static SessionManager uniqueInstance;

    // Requests run on several threads at once, so the map must be thread-safe.
    private final Map<String, SystemUser> sessions = new ConcurrentHashMap<>();

    private SessionManager() {
    }

    /**
     * Returns the only SessionManager, creating it on first use.
     * synchronized so two requests arriving together can't each create one.
     */
    public static synchronized SessionManager instance() {
        if (uniqueInstance == null) {
            uniqueInstance = new SessionManager();
        }
        return uniqueInstance;
    }

    /** Starts a session for the user and returns its new token. */
    public String login(SystemUser user) {
        String token = UUID.randomUUID().toString();
        sessions.put(token, user);
        return token;
    }

    /** The user for this token, or null if the token is missing, unknown or logged out. */
    public SystemUser getUser(String token) {
        return token == null ? null : sessions.get(token);
    }

    /** Ends the session. Does nothing if the token is missing or already logged out. */
    public void logout(String token) {
        if (token != null) {
            sessions.remove(token);
        }
    }
}
