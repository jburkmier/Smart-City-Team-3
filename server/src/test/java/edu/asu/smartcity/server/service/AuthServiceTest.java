package edu.asu.smartcity.server.service;

import edu.asu.smartcity.server.dao.UserDAO;
import edu.asu.smartcity.server.dao.UserRecord;
import edu.asu.smartcity.server.domain.Worker;
import edu.asu.smartcity.server.dto.LoginRequest;
import edu.asu.smartcity.server.dto.LoginResponse;
import edu.asu.smartcity.server.dto.RegisterRequest;
import edu.asu.smartcity.server.dto.UserDto;
import edu.asu.smartcity.server.model.Role;
import edu.asu.smartcity.server.session.SessionManager;
import edu.asu.smartcity.server.web.ApiException;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** Runs AuthService against an in-memory UserDAO, so no database is needed. */
public class AuthServiceTest {

    /** Keeps users in a map instead of MySQL. */
    private static class FakeUserDAO extends UserDAO {
        final Map<String, UserRecord> byEmail = new HashMap<>();
        private int nextId = 1;

        FakeUserDAO() {
            super(null);
        }

        @Override
        public Optional<UserRecord> findByEmail(String email) {
            return Optional.ofNullable(byEmail.get(email));
        }

        @Override
        public Optional<UserRecord> findById(int id) {
            return byEmail.values().stream().filter(u -> u.id() == id).findFirst();
        }

        @Override
        public boolean existsByEmail(String email) {
            return byEmail.containsKey(email);
        }

        @Override
        public int create(String name, String email, String passwordHash, Role role, String address, String phone) {
            int id = nextId++;
            byEmail.put(email, new UserRecord(id, name, email, passwordHash, role, address, phone, null, null));
            return id;
        }
    }

    private final FakeUserDAO users = new FakeUserDAO();
    private final SessionManager sessions = SessionManager.instance();
    private final AuthService auth = new AuthService(users, sessions);

    private static RegisterRequest request(String name, String email, String password) {
        return new RegisterRequest(name, email, password, null, null);
    }

    /** Runs the action, expects an ApiException, and returns it. */
    private static ApiException expectError(ThrowingRunnable action) {
        try {
            action.run();
        } catch (ApiException e) {
            return e;
        } catch (Exception e) {
            throw new AssertionError("Expected ApiException but got " + e, e);
        }
        fail("Expected ApiException");
        return null;
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private void assertFieldError(RegisterRequest request, String field) {
        ApiException e = expectError(() -> auth.register(request));
        assertEquals(400, e.status());
        assertEquals("VALIDATION_FAILED", e.code());
        assertTrue("expected an error for " + field + " but got " + e.fields(), e.fields().containsKey(field));
    }

    // ---- register validation ----

    @Test
    public void registerRejectsBlankName() {
        assertFieldError(request("   ", "a@example.com", "password123"), "name");
    }

    @Test
    public void registerRejectsInvalidEmail() {
        assertFieldError(request("Alex", "not-an-email", "password123"), "email");
    }

    @Test
    public void registerRejectsSevenCharacterPassword() {
        assertFieldError(request("Alex", "a@example.com", "1234567"), "password");
    }

    @Test
    public void registerRejects73CharacterPassword() {
        assertFieldError(request("Alex", "a@example.com", "x".repeat(73)), "password");
    }

    @Test
    public void registerReportsEveryInvalidFieldAtOnce() {
        ApiException e = expectError(() -> auth.register(request("", "bad", "short")));
        assertEquals(3, e.fields().size());
    }

    @Test
    public void registerAcceptsValidRequestAsCitizenWithTrimmedLowercaseEmail() throws Exception {
        UserDto user = auth.register(new RegisterRequest(" Alex Rivera ", "  Alex@Example.COM ", "password123",
                "123 Main St", " "));
        assertEquals("alex@example.com", user.email());
        assertEquals("Alex Rivera", user.name());
        assertEquals(Role.CITIZEN, user.role());
        assertEquals("123 Main St", user.address());
        assertNull(user.phone());

        UserRecord stored = users.byEmail.get("alex@example.com");
        assertEquals(Role.CITIZEN, stored.role());
        assertNotEquals("password123", stored.passwordHash());
        assertTrue(PasswordHasher.matches("password123", stored.passwordHash()));
    }

    @Test
    public void registerRejectsDuplicateEmailIgnoringCase() throws Exception {
        auth.register(request("Alex", "alex@example.com", "password123"));
        ApiException e = expectError(() -> auth.register(request("Other", "ALEX@example.com", "password123")));
        assertEquals(409, e.status());
        assertEquals("EMAIL_TAKEN", e.code());
    }

    // ---- login ----

    @Test
    public void loginReturnsTokenAndBuildsTheRightUserType() throws Exception {
        users.byEmail.put("publicworks.worker@smartcity.test", new UserRecord(4, "Sam Lee", "publicworks.worker@smartcity.test",
                PasswordHasher.hash("password123"), Role.WORKER, null, null, 1, "Road repair"));

        LoginResponse response = auth.login(new LoginRequest(" PublicWorks.Worker@SmartCity.test ", "password123"));

        assertEquals(Role.WORKER, response.user().role());
        assertEquals(Integer.valueOf(1), response.user().departmentId());
        assertTrue(sessions.getUser(response.token()) instanceof Worker);
    }

    @Test
    public void wrongPasswordAndUnknownEmailGiveTheSameError() throws Exception {
        auth.register(request("Alex", "alex@example.com", "password123"));

        ApiException wrongPassword = expectError(() -> auth.login(new LoginRequest("alex@example.com", "wrongpass1")));
        ApiException unknownEmail = expectError(() -> auth.login(new LoginRequest("nobody@example.com", "password123")));

        for (ApiException e : new ApiException[] {wrongPassword, unknownEmail}) {
            assertEquals(401, e.status());
            assertEquals("INVALID_CREDENTIALS", e.code());
            assertEquals("Invalid email or password.", e.getMessage());
        }
    }

    @Test
    public void loginWithMissingFieldsIsAValidationError() {
        ApiException e = expectError(() -> auth.login(new LoginRequest(null, "")));
        assertEquals(400, e.status());
        assertTrue(e.fields().containsKey("email"));
        assertTrue(e.fields().containsKey("password"));
    }

    @Test
    public void logoutEndsTheLoginSession() throws Exception {
        auth.register(request("Alex", "alex@example.com", "password123"));
        String token = auth.login(new LoginRequest("alex@example.com", "password123")).token();
        auth.logout(token);
        assertNull(sessions.getUser(token));
    }
}
