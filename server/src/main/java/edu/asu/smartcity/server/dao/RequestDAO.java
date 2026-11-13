package edu.asu.smartcity.server.dao;

import edu.asu.smartcity.server.domain.Request;
import edu.asu.smartcity.server.db.DatabaseConnection;
import edu.asu.smartcity.server.dto.RequestDto;
import edu.asu.smartcity.server.model.RequestStatus;
import edu.asu.smartcity.server.model.RequestType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** SQL for creating and listing service requests visible to the caller. */
public class RequestDAO {

    private static final String REQUEST_COLUMNS = """
            SELECT r.request_id, r.type,
                   CASE WHEN r.is_hidden THEN '[removed by administrator]' ELSE r.description END AS description,
                   r.location, r.status, c.user_id AS citizen_id, c.name AS citizen_name,
                   r.department_id, d.name AS department_name,
                   t.worker_id AS assigned_worker_id, w.name AS assigned_worker_name,
                   r.rejection_reason, r.resolution_notes, r.submitted_at, r.updated_at, r.resolved_at
            FROM service_requests r
            JOIN users c ON c.user_id = r.citizen_id
            LEFT JOIN departments d ON d.department_id = r.department_id
            LEFT JOIN tasks t ON t.request_id = r.request_id
            LEFT JOIN users w ON w.user_id = t.worker_id
            """;

    private final DatabaseConnection database;

    public RequestDAO(DatabaseConnection database) {
        this.database = database;
    }

    /** Persists a new request using the status supplied by its current state. */
    public RequestDto create(Request request) throws SQLException {
        String sql = """
                INSERT INTO service_requests (citizen_id, type, description, location, status)
                VALUES (?, ?, ?, ?, ?)
                """;
        int requestId;
        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, request.getCitizenId());
            stmt.setString(2, request.getType().name());
            stmt.setString(3, request.getDescription());
            stmt.setString(4, request.getLocation());
            stmt.setString(5, request.getStatusName());
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Creating request failed; no request_id was returned.");
                }
                requestId = keys.getInt(1);
            }
        }
        return findById(requestId).orElseThrow(
                () -> new SQLException("Creating request failed; the saved request could not be loaded."));
    }

    /** Returns one citizen's requests, newest first. */
    public List<RequestDto> findByCitizen(int citizenId) throws SQLException {
        return query(" WHERE r.citizen_id = ? ORDER BY r.submitted_at DESC, r.request_id DESC",
                List.of(citizenId));
    }

    private Optional<RequestDto> findById(int requestId) throws SQLException {
        return query(" WHERE r.request_id = ?", List.of(requestId)).stream().findFirst();
    }

    /** Returns all requests visible to an administrator, optionally filtered. */
    public List<RequestDto> findForAdministrator(RequestStatus status, RequestType type, Integer departmentId)
            throws SQLException {
        return findStaffRequests(null, status, type, departmentId);
    }

    /** Returns requests in the manager's department, optionally filtered. */
    public List<RequestDto> findForDepartmentManager(int departmentId, RequestStatus status, RequestType type)
            throws SQLException {
        return findStaffRequests(departmentId, status, type, null);
    }

    private List<RequestDto> findStaffRequests(Integer scopedDepartmentId, RequestStatus status,
                                               RequestType type, Integer departmentFilter)
            throws SQLException {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> parameters = new ArrayList<>();
        if (scopedDepartmentId != null) {
            where.append(" AND r.department_id = ?");
            parameters.add(scopedDepartmentId);
        }
        if (status != null) {
            where.append(" AND r.status = ?");
            parameters.add(status.name());
        }
        if (type != null) {
            where.append(" AND r.type = ?");
            parameters.add(type.name());
        }
        if (departmentFilter != null) {
            where.append(" AND r.department_id = ?");
            parameters.add(departmentFilter);
        }
        where.append(" ORDER BY r.submitted_at DESC, r.request_id DESC");
        return query(where.toString(), parameters);
    }

    private List<RequestDto> query(String suffix, List<Object> parameters) throws SQLException {
        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(REQUEST_COLUMNS + suffix)) {
            for (int i = 0; i < parameters.size(); i++) {
                stmt.setObject(i + 1, parameters.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                List<RequestDto> requests = new ArrayList<>();
                while (rs.next()) {
                    requests.add(toDto(rs));
                }
                return requests;
            }
        }
    }

    private static RequestDto toDto(ResultSet rs) throws SQLException {
        return new RequestDto(
                rs.getInt("request_id"),
                RequestType.valueOf(rs.getString("type")),
                rs.getString("description"),
                rs.getString("location"),
                RequestStatus.valueOf(rs.getString("status")),
                rs.getInt("citizen_id"),
                rs.getString("citizen_name"),
                rs.getObject("department_id", Integer.class),
                rs.getString("department_name"),
                rs.getObject("assigned_worker_id", Integer.class),
                rs.getString("assigned_worker_name"),
                rs.getString("rejection_reason"),
                rs.getString("resolution_notes"),
                localDateTime(rs.getTimestamp("submitted_at")),
                localDateTime(rs.getTimestamp("updated_at")),
                localDateTime(rs.getTimestamp("resolved_at")));
    }

    private static java.time.LocalDateTime localDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
