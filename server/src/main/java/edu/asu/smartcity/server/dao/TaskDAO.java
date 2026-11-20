package edu.asu.smartcity.server.dao;

import edu.asu.smartcity.server.db.DatabaseConnection;
import edu.asu.smartcity.server.dto.RequestDto;
import edu.asu.smartcity.server.dto.TaskAssignmentResult;
import edu.asu.smartcity.server.dto.TaskDto;
import edu.asu.smartcity.server.model.RequestStatus;
import edu.asu.smartcity.server.model.Role;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** SQL for assigning workers to service requests and listing their tasks. */
public class TaskDAO {

    private static final String TASK_COLUMNS = """
            SELECT t.task_id, r.request_id AS request_id, t.worker_id, w.name AS worker_name,
                   t.assigned_by, t.notes, t.assigned_at,
                   r.type,
                   CASE WHEN r.is_hidden THEN '[removed by administrator]' ELSE r.description END AS description,
                   r.location, r.status, c.user_id AS citizen_id, c.name AS citizen_name,
                   r.department_id, d.name AS department_name,
                   t.worker_id AS assigned_worker_id, w.name AS assigned_worker_name,
                   r.rejection_reason, r.resolution_notes, r.submitted_at, r.updated_at, r.resolved_at
            FROM tasks t
            JOIN service_requests r ON r.request_id = t.request_id
            JOIN users w ON w.user_id = t.worker_id
            JOIN users c ON c.user_id = r.citizen_id
            LEFT JOIN departments d ON d.department_id = r.department_id
            """;

    private final DatabaseConnection database;

    public TaskDAO(DatabaseConnection database) {
        this.database = database;
    }

    /**
     * Creates or reassigns the request's single task while locking the request row, so its
     * department and eligible status cannot change between validation and persistence.
     */
    public Optional<TaskAssignmentResult> assign(int requestId, int departmentId, int workerId,
                                                  int managerId, String notes) throws SQLException {
        boolean created;
        try (Connection conn = database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String requestSql = """
                        SELECT department_id, status FROM service_requests
                        WHERE request_id = ? FOR UPDATE
                        """;
                try (PreparedStatement stmt = conn.prepareStatement(requestSql)) {
                    stmt.setInt(1, requestId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (!rs.next()
                                || rs.getInt("department_id") != departmentId
                                || !(RequestStatus.ASSIGNED.name().equals(rs.getString("status"))
                                || RequestStatus.IN_PROGRESS.name().equals(rs.getString("status")))) {
                            conn.rollback();
                            return Optional.empty();
                        }
                    }
                }

                String workerSql = "SELECT 1 FROM users WHERE user_id = ? AND department_id = ? AND role_id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(workerSql)) {
                    stmt.setInt(1, workerId);
                    stmt.setInt(2, departmentId);
                    stmt.setInt(3, Role.WORKER.id());
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return Optional.empty();
                        }
                    }
                }

                String existingSql = "SELECT task_id FROM tasks WHERE request_id = ? FOR UPDATE";
                try (PreparedStatement stmt = conn.prepareStatement(existingSql)) {
                    stmt.setInt(1, requestId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        created = !rs.next();
                    }
                }

                String saveSql = """
                        INSERT INTO tasks (request_id, worker_id, assigned_by, notes)
                        VALUES (?, ?, ?, ?)
                        ON DUPLICATE KEY UPDATE worker_id = VALUES(worker_id),
                                                assigned_by = VALUES(assigned_by),
                                                notes = VALUES(notes),
                                                assigned_at = CURRENT_TIMESTAMP
                        """;
                try (PreparedStatement stmt = conn.prepareStatement(saveSql)) {
                    stmt.setInt(1, requestId);
                    stmt.setInt(2, workerId);
                    stmt.setInt(3, managerId);
                    stmt.setString(4, notes);
                    stmt.executeUpdate();
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }

        TaskDto task = findByRequestId(requestId).orElseThrow(
                () -> new SQLException("Assigned task could not be loaded."));
        return Optional.of(new TaskAssignmentResult(task, created));
    }

    public List<TaskDto> findByWorker(int workerId) throws SQLException {
        return query(" WHERE t.worker_id = ? ORDER BY t.assigned_at DESC, t.task_id DESC", workerId);
    }

    public Optional<TaskDto> findByRequestId(int requestId) throws SQLException {
        return query(" WHERE t.request_id = ?", requestId).stream().findFirst();
    }

    private List<TaskDto> query(String suffix, int id) throws SQLException {
        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(TASK_COLUMNS + suffix)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                List<TaskDto> tasks = new ArrayList<>();
                while (rs.next()) {
                    RequestDto request = RequestDAO.toDto(rs);
                    Timestamp assignedAt = rs.getTimestamp("assigned_at");
                    tasks.add(new TaskDto(rs.getInt("task_id"), rs.getInt("request_id"), request,
                            rs.getInt("worker_id"), rs.getString("worker_name"), rs.getInt("assigned_by"),
                            rs.getString("notes"), assignedAt == null ? null : assignedAt.toLocalDateTime()));
                }
                return tasks;
            }
        }
    }
}
