package edu.asu.smartcity.server.dao;

import edu.asu.smartcity.server.db.DatabaseConnection;
import edu.asu.smartcity.server.dto.EventDto;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** SQL for reading visible upcoming events. */
public class EventDAO {

    private static final String EVENT_COLUMNS = """
            SELECT e.event_id, e.title, e.description, e.category, e.start_time, e.district,
                   e.location_detail, e.price, e.created_by,
                   (SELECT COUNT(*) FROM event_registrations r WHERE r.event_id = e.event_id) AS registration_count,
                   EXISTS (SELECT 1 FROM event_registrations r
                           WHERE r.event_id = e.event_id AND r.user_id = ?) AS registered_by_me
            FROM events e
            """;

    private final DatabaseConnection database;

    public EventDAO(DatabaseConnection database) {
        this.database = database;
    }

    /** Returns upcoming visible events; the catalog applies optional filters. */
    public List<EventDto> findUpcoming(int userId, String sort) throws SQLException {
        String sql = new StringBuilder(EVENT_COLUMNS)
                .append(" WHERE e.start_time > CURRENT_TIMESTAMP")
                .append(" AND e.is_cancelled = FALSE AND e.is_hidden = FALSE")
                .append(" ORDER BY ")
                .append("popularity".equals(sort)
                ? "registration_count DESC, e.start_time ASC"
                : "e.start_time ASC")
                .toString();
        return query(sql, List.of(userId));
    }

    /** Finds one event visible to all logged-in users. */
    public Optional<EventDto> findVisibleById(int eventId, int userId) throws SQLException {
        List<EventDto> rows = query(EVENT_COLUMNS
                + " WHERE e.event_id = ? AND e.is_cancelled = FALSE AND e.is_hidden = FALSE",
                List.of(userId, eventId));
        return rows.stream().findFirst();
    }

    /** Events a citizen registered for, including past events unless upcoming is requested. */
    public List<EventDto> findRegistrations(int userId, boolean upcoming) throws SQLException {
        String sql = EVENT_COLUMNS + """
                 JOIN event_registrations mine ON mine.event_id = e.event_id
                 WHERE mine.user_id = ? AND e.is_cancelled = FALSE AND e.is_hidden = FALSE
                """ + (upcoming ? " AND e.start_time > CURRENT_TIMESTAMP" : "")
                + " ORDER BY e.start_time ASC";
        return query(sql, List.of(userId, userId));
    }

    /** Inserts an event and returns its response DTO. */
    public EventDto create(String title, String description, String category, LocalDateTime startTime,
                           String district, String locationDetail, BigDecimal price, int createdById)
            throws SQLException {
        String sql = """
                INSERT INTO events (title, description, category, start_time, district,
                                    location_detail, price, created_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, title);
            stmt.setString(2, description);
            stmt.setString(3, category);
            stmt.setTimestamp(4, Timestamp.valueOf(startTime));
            stmt.setString(5, district);
            stmt.setString(6, locationDetail);
            stmt.setBigDecimal(7, price);
            stmt.setInt(8, createdById);
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Creating event failed; no event_id was returned.");
                }
                return new EventDto(keys.getInt(1), title, description, category, startTime, district,
                        locationDetail, price, 0, false, createdById);
            }
        }
    }

    /** Updates an active event. Returns empty when the id is missing or no longer active. */
    public Optional<EventDto> update(int eventId, String title, String description, String category,
                                     LocalDateTime startTime, String district, String locationDetail,
                                     BigDecimal price, int userId) throws SQLException {
        String sql = """
                UPDATE events SET title = ?, description = ?, category = ?, start_time = ?,
                                  district = ?, location_detail = ?, price = ?
                WHERE event_id = ? AND is_cancelled = FALSE AND is_hidden = FALSE
                """;
        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, title);
            stmt.setString(2, description);
            stmt.setString(3, category);
            stmt.setTimestamp(4, Timestamp.valueOf(startTime));
            stmt.setString(5, district);
            stmt.setString(6, locationDetail);
            stmt.setBigDecimal(7, price);
            stmt.setInt(8, eventId);
            stmt.executeUpdate();
        }
        return findVisibleById(eventId, userId);
    }

    /** Soft-cancels an active event. */
    public boolean cancel(int eventId) throws SQLException {
        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE events SET is_cancelled = TRUE WHERE event_id = ? AND is_cancelled = FALSE")) {
            stmt.setInt(1, eventId);
            return stmt.executeUpdate() > 0;
        }
    }

    /** Registers a citizen if the event is visible and has not started yet. */
    public boolean register(int eventId, int userId) throws SQLException {
        String sql = """
                INSERT INTO event_registrations (user_id, event_id)
                SELECT ?, event_id FROM events
                WHERE event_id = ? AND is_cancelled = FALSE AND is_hidden = FALSE
                  AND start_time > CURRENT_TIMESTAMP
                """;
        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, eventId);
            return stmt.executeUpdate() > 0;
        }
    }

    /** Returns whether the event exists and remains publicly visible. */
    public boolean isVisible(int eventId) throws SQLException {
        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT 1 FROM events WHERE event_id = ? AND is_cancelled = FALSE AND is_hidden = FALSE")) {
            stmt.setInt(1, eventId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Removes a citizen's registration. */
    public boolean unregister(int eventId, int userId) throws SQLException {
        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "DELETE FROM event_registrations WHERE event_id = ? AND user_id = ?")) {
            stmt.setInt(1, eventId);
            stmt.setInt(2, userId);
            return stmt.executeUpdate() > 0;
        }
    }

    private List<EventDto> query(String sql, List<?> parameters) throws SQLException {
        List<EventDto> events = new ArrayList<>();
        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < parameters.size(); i++) {
                stmt.setObject(i + 1, parameters.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    events.add(toDto(rs));
                }
            }
        }
        return events;
    }

    private static EventDto toDto(ResultSet rs) throws SQLException {
        return new EventDto(
                rs.getInt("event_id"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getString("category"),
                rs.getTimestamp("start_time").toLocalDateTime(),
                rs.getString("district"),
                rs.getString("location_detail"),
                rs.getBigDecimal("price"),
                rs.getInt("registration_count"),
                rs.getBoolean("registered_by_me"),
                rs.getInt("created_by"));
    }
}
