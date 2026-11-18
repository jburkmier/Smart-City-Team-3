package edu.asu.smartcity.server.dao;

import edu.asu.smartcity.server.db.DatabaseConnection;
import edu.asu.smartcity.server.dto.NotificationDto;
import edu.asu.smartcity.server.model.NotificationType;
import edu.asu.smartcity.server.observer.Notice;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/** SQL for saving notifications and reading or marking the caller's own notifications. */
public class NotificationDAO {

    private final DatabaseConnection database;

    public NotificationDAO(DatabaseConnection database) {
        this.database = database;
    }

    /** Saves one notification for one user; called by UserInbox.update(). */
    public void insert(int userId, Notice notice) throws SQLException {
        String sql = """
                INSERT INTO notifications (user_id, type, message, related_id)
                VALUES (?, ?, ?, ?)
                """;
        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, notice.type().name());
            stmt.setString(3, notice.message());
            stmt.setObject(4, notice.relatedId());
            stmt.executeUpdate();
        }
    }

    /** Returns one user's notifications, newest first, optionally only unread ones. */
    public List<NotificationDto> findByUser(int userId, boolean unreadOnly) throws SQLException {
        String sql = """
                SELECT notification_id, type, message, related_id, is_read, created_at
                FROM notifications
                WHERE user_id = ?
                """
                + (unreadOnly ? " AND is_read = FALSE" : "")
                + " ORDER BY created_at DESC, notification_id DESC";
        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                List<NotificationDto> notifications = new ArrayList<>();
                while (rs.next()) {
                    notifications.add(toDto(rs));
                }
                return notifications;
            }
        }
    }

    /**
     * Marks one notification read, only if it belongs to the user.
     * Returns false if no such notification exists for this user. Marking one that is
     * already read still returns true: MySQL Connector/J counts matched rows by default.
     */
    public boolean markRead(int notificationId, int userId) throws SQLException {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE notification_id = ? AND user_id = ?";
        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, notificationId);
            stmt.setInt(2, userId);
            return stmt.executeUpdate() == 1;
        }
    }

    /** Marks every unread notification of the user as read. */
    public void markAllRead(int userId) throws SQLException {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE user_id = ? AND is_read = FALSE";
        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.executeUpdate();
        }
    }

    private static NotificationDto toDto(ResultSet rs) throws SQLException {
        Timestamp createdAt = rs.getTimestamp("created_at");
        return new NotificationDto(
                rs.getInt("notification_id"),
                NotificationType.valueOf(rs.getString("type")),
                rs.getString("message"),
                rs.getObject("related_id", Integer.class),
                rs.getBoolean("is_read"),
                createdAt == null ? null : createdAt.toLocalDateTime());
    }
}
