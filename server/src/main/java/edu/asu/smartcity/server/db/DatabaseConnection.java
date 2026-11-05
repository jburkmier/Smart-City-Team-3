package edu.asu.smartcity.server.db;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Singleton pattern: the one place that knows how to connect to MySQL.
 * Reads db.url, db.user and db.password from db.properties in the working directory
 * (the repo root when started with ./gradlew :server:run).
 *
 * getConnection() opens a new connection each time; callers close it with try-with-resources.
 */
public class DatabaseConnection {

    private static final Path PROPERTIES_FILE = Path.of("db.properties");

    private static DatabaseConnection instance;

    private final String url;
    private final String user;
    private final String password;

    private DatabaseConnection() {
        if (!Files.exists(PROPERTIES_FILE)) {
            throw new IllegalStateException(PROPERTIES_FILE.toAbsolutePath()
                    + " not found. Copy db.properties.example to db.properties and set db.password.");
        }
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(PROPERTIES_FILE)) {
            props.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + PROPERTIES_FILE.toAbsolutePath(), e);
        }
        url = require(props, "db.url");
        user = require(props, "db.user");
        password = require(props, "db.password");
    }

    /** Returns the only DatabaseConnection, creating it on first use. */
    public static synchronized DatabaseConnection instance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    /** Opens a new connection to the smartcity database. The caller must close it. */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    private static String require(Properties props, String key) {
        String value = props.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(key + " is missing from " + PROPERTIES_FILE.toAbsolutePath());
        }
        return value.trim();
    }
}
