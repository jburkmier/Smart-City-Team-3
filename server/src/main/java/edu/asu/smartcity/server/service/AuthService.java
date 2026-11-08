package edu.asu.smartcity.server.service;

import edu.asu.smartcity.server.dao.UserDAO;
import edu.asu.smartcity.server.dao.UserRecord;
import edu.asu.smartcity.server.domain.SystemUser;
import edu.asu.smartcity.server.dto.LoginRequest;
import edu.asu.smartcity.server.dto.LoginResponse;
import edu.asu.smartcity.server.dto.RegisterRequest;
import edu.asu.smartcity.server.dto.UserDto;
import edu.asu.smartcity.server.factory.UserCreators;
import edu.asu.smartcity.server.model.Role;
import edu.asu.smartcity.server.session.SessionManager;
import edu.asu.smartcity.server.web.ApiException;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/** Register, login and logout (docs/api.md section 2). */
public class AuthService {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final String INVALID_CREDENTIALS = "Invalid email or password.";

    private final UserDAO users;
    private final SessionManager sessions;

    public AuthService(UserDAO users, SessionManager sessions) {
        this.users = users;
        this.sessions = sessions;
    }

    /** Creates a citizen account. Any role sent by the client is ignored. */
    public UserDto register(RegisterRequest request) throws SQLException {
        String name = trimToNull(request.name());
        String email = normalizeEmail(request.email());
        String password = request.password();
        String address = trimToNull(request.address());
        String phone = trimToNull(request.phone());

        Map<String, String> errors = new LinkedHashMap<>();
        if (name == null) {
            errors.put("name", "Name is required");
        } else if (name.length() > 100) {
            errors.put("name", "Must be at most 100 characters");
        }
        if (email == null) {
            errors.put("email", "Email is required");
        } else if (email.length() > 255 || !EMAIL.matcher(email).matches()) {
            errors.put("email", "Invalid email format");
        }
        if (password == null || password.isEmpty()) {
            errors.put("password", "Password is required");
        } else if (password.length() < 8) {
            errors.put("password", "Must be at least 8 characters");
        } else if (password.length() > 72) {
            errors.put("password", "Must be at most 72 characters");
        }
        if (address != null && address.length() > 255) {
            errors.put("address", "Must be at most 255 characters");
        }
        if (phone != null && phone.length() > 20) {
            errors.put("phone", "Must be at most 20 characters");
        }
        if (!errors.isEmpty()) {
            throw new ApiException(400, "VALIDATION_FAILED", "Some fields are invalid.", errors);
        }

        if (users.existsByEmail(email)) {
            throw emailTaken();
        }
        int id;
        try {
            id = users.create(name, email, PasswordHasher.hash(password), Role.CITIZEN, address, phone);
        } catch (SQLIntegrityConstraintViolationException e) {
            throw emailTaken(); // another request registered the same email in the meantime
        }
        UserRecord row = new UserRecord(id, name, email, null, Role.CITIZEN, address, phone, null, null);
        return UserMapper.toDto(UserCreators.forRole(Role.CITIZEN).loadUser(row));
    }

    /**
     * Checks the password, builds the right kind of user with the Factory, and starts a session.
     * Unknown email and wrong password give the same error, so callers can't probe for accounts.
     */
    public LoginResponse login(LoginRequest request) throws SQLException {
        String email = normalizeEmail(request.email());
        String password = request.password();

        Map<String, String> errors = new LinkedHashMap<>();
        if (email == null) {
            errors.put("email", "Email is required");
        }
        if (password == null || password.isEmpty()) {
            errors.put("password", "Password is required");
        }
        if (!errors.isEmpty()) {
            throw new ApiException(400, "VALIDATION_FAILED", "Some fields are invalid.", errors);
        }

        Optional<UserRecord> row = users.findByEmail(email);
        if (row.isEmpty() || !PasswordHasher.matches(password, row.get().passwordHash())) {
            throw new ApiException(401, "INVALID_CREDENTIALS", INVALID_CREDENTIALS);
        }
        SystemUser user = UserCreators.forRole(row.get().role()).loadUser(row.get());
        String token = sessions.login(user);
        return new LoginResponse(token, UserMapper.toDto(user));
    }

    /** Ends the session. Safe to call with a missing or already logged-out token. */
    public void logout(String token) {
        sessions.logout(token);
    }

    private static ApiException emailTaken() {
        return new ApiException(409, "EMAIL_TAKEN", "An account with this email already exists.");
    }

    private static String normalizeEmail(String email) {
        String trimmed = trimToNull(email);
        return trimmed == null ? null : trimmed.toLowerCase(Locale.ROOT);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
