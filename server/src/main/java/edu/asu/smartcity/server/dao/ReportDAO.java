package edu.asu.smartcity.server.dao;

import edu.asu.smartcity.server.db.DatabaseConnection;
import edu.asu.smartcity.server.dto.DepartmentReportDto;
import edu.asu.smartcity.server.dto.EngagementReportDto;
import edu.asu.smartcity.server.dto.WorkerTaskLoadDto;
import edu.asu.smartcity.server.model.RequestStatus;
import edu.asu.smartcity.server.model.RequestType;
import edu.asu.smartcity.server.model.Role;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Read-only aggregate queries for the reports endpoints. */
public class ReportDAO {

    private final DatabaseConnection database;

    public ReportDAO(DatabaseConnection database) {
        this.database = database;
    }

    public EngagementReportDto engagement() throws SQLException {
        String sql = """
                SELECT CURRENT_TIMESTAMP AS generated_at,
                       (SELECT COUNT(*) FROM users) AS total_users,
                       (SELECT COUNT(*) FROM users WHERE role_id = ?) AS total_citizens,
                       (SELECT COUNT(*) FROM users
                        WHERE last_login_at >= DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 7 DAY)) AS active_users,
                       (SELECT COUNT(*) FROM events
                        WHERE start_time >= CURRENT_TIMESTAMP AND is_cancelled = FALSE AND is_hidden = FALSE)
                           AS upcoming_events,
                       (SELECT COUNT(*) FROM event_registrations) AS total_registrations,
                       (SELECT COUNT(*) FROM flags WHERE status = 'OPEN') AS open_flags
                """;
        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, Role.CITIZEN.id());
            java.time.LocalDateTime generatedAt;
            int totalUsers;
            int totalCitizens;
            int activeUsers;
            int upcomingEvents;
            int totalRegistrations;
            int openFlags;
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Engagement report query returned no result.");
                }
                generatedAt = localDateTime(rs.getTimestamp("generated_at"));
                totalUsers = rs.getInt("total_users");
                totalCitizens = rs.getInt("total_citizens");
                activeUsers = rs.getInt("active_users");
                upcomingEvents = rs.getInt("upcoming_events");
                totalRegistrations = rs.getInt("total_registrations");
                openFlags = rs.getInt("open_flags");
            }
            return new EngagementReportDto(generatedAt, totalUsers, totalCitizens, activeUsers, upcomingEvents,
                    totalRegistrations, requestCounts(conn, "status", null, RequestStatus.class),
                    requestCounts(conn, "type", null, RequestType.class), openFlags);
        }
    }

    public Optional<DepartmentReportDto> department(int departmentId) throws SQLException {
        String sql = """
                SELECT d.department_id, d.name, CURRENT_TIMESTAMP AS generated_at,
                       (SELECT COUNT(*) FROM service_requests r
                        WHERE r.department_id = d.department_id) AS total_requests,
                       (SELECT AVG(TIMESTAMPDIFF(MICROSECOND, r.submitted_at, r.resolved_at)
                                   / 3600000000.0)
                        FROM service_requests r
                        WHERE r.department_id = d.department_id AND r.status = 'RESOLVED'
                          AND r.resolved_at IS NOT NULL) AS average_resolution_hours,
                       (SELECT COUNT(*) FROM service_requests r
                        WHERE r.department_id = d.department_id AND r.status = 'RESOLVED'
                          AND r.resolved_at >= DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 30 DAY))
                           AS resolved_last_30_days
                FROM departments d
                WHERE d.department_id = ?
                """;
        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, departmentId);
            String departmentName;
            java.time.LocalDateTime generatedAt;
            int totalRequests;
            Double averageResolutionHours;
            int resolvedLast30Days;
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                departmentName = rs.getString("name");
                generatedAt = localDateTime(rs.getTimestamp("generated_at"));
                totalRequests = rs.getInt("total_requests");
                double average = rs.getDouble("average_resolution_hours");
                averageResolutionHours = rs.wasNull() ? null : average;
                resolvedLast30Days = rs.getInt("resolved_last_30_days");
            }
            Map<RequestStatus, Integer> statuses =
                    requestCounts(conn, "status", departmentId, RequestStatus.class);
            List<WorkerTaskLoadDto> workerLoads = workerTaskLoads(conn, departmentId);
            return Optional.of(new DepartmentReportDto(departmentId, departmentName, generatedAt,
                    totalRequests, statuses, averageResolutionHours, resolvedLast30Days, workerLoads));
        }
    }

    private static <E extends Enum<E>> Map<E, Integer> requestCounts(
            Connection conn, String column, Integer departmentId, Class<E> enumType) throws SQLException {
        String sql = "SELECT r.`" + column + "` AS group_value, COUNT(*) AS total FROM service_requests r";
        if (departmentId != null) {
            sql += " WHERE r.department_id = ?";
        }
        sql += " GROUP BY r.`" + column + "`";
        Map<E, Integer> counts = new EnumMap<>(enumType);
        for (E value : enumType.getEnumConstants()) {
            counts.put(value, 0);
        }
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (departmentId != null) {
                stmt.setInt(1, departmentId);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    E value = Enum.valueOf(enumType, rs.getString("group_value"));
                    counts.put(value, rs.getInt("total"));
                }
            }
        }
        return counts;
    }

    private static List<WorkerTaskLoadDto> workerTaskLoads(Connection conn, int departmentId)
            throws SQLException {
        String sql = """
                SELECT w.user_id, w.name,
                       SUM(CASE WHEN r.status IN ('ASSIGNED', 'IN_PROGRESS') THEN 1 ELSE 0 END) AS open_tasks
                FROM users w
                LEFT JOIN tasks t ON t.worker_id = w.user_id
                LEFT JOIN service_requests r
                       ON r.request_id = t.request_id AND r.department_id = w.department_id
                WHERE w.department_id = ? AND w.role_id = ?
                GROUP BY w.user_id, w.name
                ORDER BY w.name, w.user_id
                """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, departmentId);
            stmt.setInt(2, Role.WORKER.id());
            try (ResultSet rs = stmt.executeQuery()) {
                List<WorkerTaskLoadDto> workers = new ArrayList<>();
                while (rs.next()) {
                    workers.add(new WorkerTaskLoadDto(rs.getInt("user_id"), rs.getString("name"),
                            rs.getInt("open_tasks")));
                }
                return workers;
            }
        }
    }

    private static java.time.LocalDateTime localDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
