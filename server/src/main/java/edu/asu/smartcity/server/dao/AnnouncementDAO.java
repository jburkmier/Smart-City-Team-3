package edu.asu.smartcity.server.dao;

import edu.asu.smartcity.server.db.DatabaseConnection;
import edu.asu.smartcity.server.domain.Announcement;
import edu.asu.smartcity.server.model.AnnouncementType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** SQL for announcements. Deleting only sets is_deleted, so notifications already sent still make sense. */
public class AnnouncementDAO {

    private static final String COLUMNS =
            "SELECT announcement_id, type, title, message, created_by, created_at FROM announcements";

    private final DatabaseConnection database;

    public AnnouncementDAO(DatabaseConnection database) {
        this.database = database;
    }

    /** Saves a new announcement and returns it as stored, with its ID and creation time. */
    public Announcement create(AnnouncementType type, String title, String message, int createdById)
            throws SQLException {
        String sql = "INSERT INTO announcements (type, title, message, created_by) VALUES (?, ?, ?, ?)";
        int announcementId;
        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, type.name());
            stmt.setString(2, title);
            stmt.setString(3, message);
            stmt.setInt(4, createdById);
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Creating announcement failed; no announcement_id was returned.");
                }
                announcementId = keys.getInt(1);
            }
        }
        return findActiveById(announcementId).orElseThrow(
                () -> new SQLException("Creating announcement failed; the saved announcement could not be loaded."));
    }

    public Optional<Announcement> findActiveById(int announcementId) throws SQLException {
        return query(COLUMNS + " WHERE announcement_id = ? AND is_deleted = FALSE", announcementId)
                .stream().findFirst();
    }

    /** Every announcement that hasn't been deleted, newest first. */
    public List<Announcement> findActive() throws SQLException {
        return query(COLUMNS + " WHERE is_deleted = FALSE ORDER BY created_at DESC, announcement_id DESC", null);
    }

    /** Updates announcement content without resending the original announcement notification. */
    public Optional<Announcement> update(int announcementId, AnnouncementType type, String title, String message)
            throws SQLException {
        String sql = """
                UPDATE announcements
                SET type = ?, title = ?, message = ?
                WHERE announcement_id = ? AND is_deleted = FALSE
                """;
        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, type.name());
            stmt.setString(2, title);
            stmt.setString(3, message);
            stmt.setInt(4, announcementId);
            stmt.executeUpdate();
        }
        return findActiveById(announcementId);
    }

    /** Hides an announcement. Returns false if it doesn't exist or was already deleted. */
    public boolean delete(int announcementId) throws SQLException {
        String sql = "UPDATE announcements SET is_deleted = TRUE WHERE announcement_id = ? AND is_deleted = FALSE";
        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, announcementId);
            return stmt.executeUpdate() == 1;
        }
    }

    private List<Announcement> query(String sql, Integer parameter) throws SQLException {
        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (parameter != null) {
                stmt.setInt(1, parameter);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                List<Announcement> announcements = new ArrayList<>();
                while (rs.next()) {
                    announcements.add(toAnnouncement(rs));
                }
                return announcements;
            }
        }
    }

    private static Announcement toAnnouncement(ResultSet rs) throws SQLException {
        Timestamp createdAt = rs.getTimestamp("created_at");
        return new Announcement(
                rs.getInt("announcement_id"),
                AnnouncementType.valueOf(rs.getString("type")),
                rs.getString("title"),
                rs.getString("message"),
                rs.getInt("created_by"),
                createdAt == null ? null : createdAt.toLocalDateTime());
    }
}
