package edu.asu.smartcity.server.dao;

import edu.asu.smartcity.server.db.DatabaseConnection;
import edu.asu.smartcity.server.model.Role;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

/** SQL for the users table. Every query uses a PreparedStatement. */
public class UserDAO {

    private static final String COLUMNS =
            "user_id, name, email, password_hash, role_id, address, phone, department_id, skill";

    /** Finds a user by email. The email must already be trimmed and lowercased. */
    public Optional<UserRecord> findByEmail(String email) throws SQLException {
        return findOne("SELECT " + COLUMNS + " FROM users WHERE email = ?", email);
    }

    public Optional<UserRecord> findById(int id) throws SQLException {
        return findOne("SELECT " + COLUMNS + " FROM users WHERE user_id = ?", id);
    }

    public boolean existsByEmail(String email) throws SQLException {
        try (Connection conn = DatabaseConnection.instance().getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT 1 FROM users WHERE email = ?")) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Inserts a new user and returns the generated user_id.
     * address and phone may be null. Department and skill stay empty (staff come from the seed data).
     */
    public int create(String name, String email, String passwordHash, Role role,
                      String address, String phone) throws SQLException {
        String sql = "INSERT INTO users (name, email, password_hash, role_id, address, phone) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.instance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, name);
            stmt.setString(2, email);
            stmt.setString(3, passwordHash);
            stmt.setInt(4, role.id());
            stmt.setString(5, address);
            stmt.setString(6, phone);
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    private Optional<UserRecord> findOne(String sql, Object param) throws SQLException {
        try (Connection conn = DatabaseConnection.instance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, param);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(toRecord(rs)) : Optional.empty();
            }
        }
    }

    private static UserRecord toRecord(ResultSet rs) throws SQLException {
        return new UserRecord(
                rs.getInt("user_id"),
                rs.getString("name"),
                rs.getString("email"),
                rs.getString("password_hash"),
                Role.fromId(rs.getInt("role_id")),
                rs.getString("address"),
                rs.getString("phone"),
                rs.getObject("department_id", Integer.class),
                rs.getString("skill"));
    }
}
