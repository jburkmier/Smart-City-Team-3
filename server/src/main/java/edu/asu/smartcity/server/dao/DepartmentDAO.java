package edu.asu.smartcity.server.dao;

import edu.asu.smartcity.server.db.DatabaseConnection;
import edu.asu.smartcity.server.dto.DepartmentDto;
import edu.asu.smartcity.server.dto.WorkerDto;
import edu.asu.smartcity.server.model.Role;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** SQL for the departments table and the workers that belong to each department. */
public class DepartmentDAO {

    private final DatabaseConnection database;

    public DepartmentDAO(DatabaseConnection database) {
        this.database = database;
    }

    /** Every department, sorted by name. */
    public List<DepartmentDto> findAll() throws SQLException {
        String sql = "SELECT department_id, name FROM departments ORDER BY name";
        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            List<DepartmentDto> departments = new ArrayList<>();
            while (rs.next()) {
                departments.add(toDepartment(rs));
            }
            return departments;
        }
    }

    /** One department, or empty if the id doesn't exist. Used to validate a department id before saving it. */
    public Optional<DepartmentDto> findById(int id) throws SQLException {
        String sql = "SELECT department_id, name FROM departments WHERE department_id = ?";
        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(toDepartment(rs)) : Optional.empty();
            }
        }
    }

    /** The workers in a department, sorted by name. Empty if the department has none. */
    public List<WorkerDto> findWorkers(int departmentId) throws SQLException {
        String sql = "SELECT user_id, name, skill FROM users "
                + "WHERE department_id = ? AND role_id = ? ORDER BY name";
        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, departmentId);
            stmt.setInt(2, Role.WORKER.id());
            try (ResultSet rs = stmt.executeQuery()) {
                List<WorkerDto> workers = new ArrayList<>();
                while (rs.next()) {
                    workers.add(new WorkerDto(rs.getInt("user_id"), rs.getString("name"), rs.getString("skill")));
                }
                return workers;
            }
        }
    }

    private static DepartmentDto toDepartment(ResultSet rs) throws SQLException {
        return new DepartmentDto(rs.getInt("department_id"), rs.getString("name"));
    }
}
