package edu.asu.smartcity.server;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;

/**
 * Entry point for SmartCity Hub.
 * Serves the web pages from src/main/resources/public (open http://localhost:7070)
 * and, in later tasks, the REST API under /api.
 */
public class ServerApp {

    public static final int PORT = 7070;

    public static void main(String[] args) {
        Javalin.create(config -> config.staticFiles.add("/public", Location.CLASSPATH))
                .start(PORT);
    }
}
