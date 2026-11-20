package edu.asu.smartcity.server.dao;

import edu.asu.smartcity.server.db.DatabaseConnection;
import edu.asu.smartcity.server.dto.FlagCreateRequest;
import edu.asu.smartcity.server.dto.FlagDto;
import edu.asu.smartcity.server.model.FlagContentType;
import edu.asu.smartcity.server.model.FlagStatus;
import edu.asu.smartcity.server.web.ApiException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** SQL for content flags and administrator moderation. */
public class FlagDAO {

    private static final String FLAG_COLUMNS = """
            SELECT f.flag_id, f.content_type, f.content_id, f.reason, f.flagged_by,
                   f.status, f.created_at, u.name AS flagged_by_name,
                   CASE f.content_type
                     WHEN 'EVENT' THEN (SELECT CONCAT(e.title, ' - ', e.district)
                                        FROM events e WHERE e.event_id = f.content_id)
                     WHEN 'REQUEST' THEN (SELECT CONCAT(r.type, ' - ', r.location)
                                          FROM service_requests r WHERE r.request_id = f.content_id)
                     WHEN 'ANNOUNCEMENT' THEN (SELECT a.title
                                               FROM announcements a WHERE a.announcement_id = f.content_id)
                   END AS content_summary
            FROM flags f
            JOIN users u ON u.user_id = f.flagged_by
            """;

    private final DatabaseConnection database;

    public FlagDAO(DatabaseConnection database) {
        this.database = database;
    }

    public FlagDto create(FlagCreateRequest request, int reporterId) throws SQLException {
        String sql = "INSERT INTO flags (content_type, content_id, reason, flagged_by) VALUES (?, ?, ?, ?)";
        int flagId;
        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, request.contentType().name());
            stmt.setInt(2, request.contentId());
            stmt.setString(3, request.reason());
            stmt.setInt(4, reporterId);
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Creating flag failed; no flag_id was returned.");
                }
                flagId = keys.getInt(1);
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                throw new ApiException(409, "ALREADY_FLAGGED", "You have already flagged this content.");
            }
            throw e;
        }
        return findById(flagId).orElseThrow(
                () -> new SQLException("Creating flag failed; the saved flag could not be loaded."));
    }

    public List<FlagDto> findAll(FlagStatus status) throws SQLException {
        String sql = FLAG_COLUMNS + (status == null ? "" : " WHERE f.status = ?")
                + " ORDER BY f.created_at DESC, f.flag_id DESC";
        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (status != null) {
                stmt.setString(1, status.name());
            }
            try (ResultSet rs = stmt.executeQuery()) {
                List<FlagDto> results = new ArrayList<>();
                while (rs.next()) {
                    results.add(toDto(rs));
                }
                return results;
            }
        }
    }

    public Optional<FlagDto> findById(int flagId) throws SQLException {
        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(FLAG_COLUMNS + " WHERE f.flag_id = ?")) {
            stmt.setInt(1, flagId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(toDto(rs)) : Optional.empty();
            }
        }
    }

    /**
     * Updates visibility and flag status together so a failed content update cannot leave a
     * flag marked REMOVED while its content is still public.
     */
    public boolean moderate(int flagId, FlagStatus nextStatus) throws SQLException {
        try (Connection conn = database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                FlagContentType contentType = null;
                int contentId = 0;
                if (nextStatus == FlagStatus.REMOVED) {
                    try (PreparedStatement select = conn.prepareStatement(
                            "SELECT content_type, content_id FROM flags "
                                    + "WHERE flag_id = ? AND status = 'OPEN' FOR UPDATE")) {
                        select.setInt(1, flagId);
                        try (ResultSet rs = select.executeQuery()) {
                            if (!rs.next()) {
                                conn.rollback();
                                return false;
                            }
                            contentType = FlagContentType.valueOf(rs.getString("content_type"));
                            contentId = rs.getInt("content_id");
                        }
                    }
                    if (!hideContent(conn, contentType, contentId)) {
                        conn.rollback();
                        return false;
                    }
                }
                try (PreparedStatement update = conn.prepareStatement(
                        "UPDATE flags SET status = ? WHERE flag_id = ? AND status = 'OPEN'")) {
                    update.setString(1, nextStatus.name());
                    update.setInt(2, flagId);
                    if (update.executeUpdate() != 1) {
                        conn.rollback();
                        return false;
                    }
                }
                conn.commit();
                return true;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /** Validates that a reportable target exists and is still visible. */
    public boolean contentIsVisible(FlagContentType type, int contentId) throws SQLException {
        String sql = switch (type) {
            case EVENT -> "SELECT 1 FROM events WHERE event_id = ? AND is_cancelled = FALSE AND is_hidden = FALSE";
            case REQUEST -> "SELECT 1 FROM service_requests WHERE request_id = ? AND is_hidden = FALSE";
            case ANNOUNCEMENT -> "SELECT 1 FROM announcements WHERE announcement_id = ? AND is_deleted = FALSE";
        };
        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, contentId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static boolean hideContent(Connection conn, FlagContentType type, int contentId) throws SQLException {
        String sql = switch (type) {
            case EVENT -> "UPDATE events SET is_hidden = TRUE WHERE event_id = ?";
            case REQUEST -> "UPDATE service_requests SET is_hidden = TRUE WHERE request_id = ?";
            case ANNOUNCEMENT -> "UPDATE announcements SET is_deleted = TRUE "
                    + "WHERE announcement_id = ?";
        };
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, contentId);
            stmt.executeUpdate();
        }
        String existenceSql = switch (type) {
            case EVENT -> "SELECT 1 FROM events WHERE event_id = ?";
            case REQUEST -> "SELECT 1 FROM service_requests WHERE request_id = ?";
            case ANNOUNCEMENT -> "SELECT 1 FROM announcements WHERE announcement_id = ?";
        };
        try (PreparedStatement stmt = conn.prepareStatement(existenceSql)) {
            stmt.setInt(1, contentId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static FlagDto toDto(ResultSet rs) throws SQLException {
        Timestamp createdAt = rs.getTimestamp("created_at");
        return new FlagDto(
                rs.getInt("flag_id"),
                FlagContentType.valueOf(rs.getString("content_type")),
                rs.getInt("content_id"),
                rs.getString("content_summary"),
                rs.getString("reason"),
                rs.getInt("flagged_by"),
                rs.getString("flagged_by_name"),
                FlagStatus.valueOf(rs.getString("status")),
                createdAt == null ? null : createdAt.toLocalDateTime());
    }
}
