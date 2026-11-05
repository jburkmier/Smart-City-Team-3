package edu.asu.smartcity.server;

import io.javalin.Javalin;

/**
 * Entry point for the SmartCity Hub REST server.
 * Routes, the JSON mapper, and exception handlers are added in later tasks.
 */
public class ServerApp {

    public static final int PORT = 7070;

    public static void main(String[] args) {
        Javalin.create().start(PORT);
    }
}
