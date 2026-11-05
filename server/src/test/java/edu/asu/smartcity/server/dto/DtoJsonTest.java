package edu.asu.smartcity.server.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.asu.smartcity.server.model.Role;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

/** Checks that the DTOs read and write the JSON shapes in docs/api.md. */
public class DtoJsonTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void registerRequestReadsAllFieldsAndIgnoresRole() throws Exception {
        String json = "{\"name\":\"Alex Rivera\",\"email\":\"alex@example.com\",\"password\":\"secret123\","
                + "\"address\":\"123 Main St\",\"phone\":\"480-555-0101\",\"role\":\"CITY_ADMINISTRATOR\"}";

        RegisterRequest request = mapper.readValue(json, RegisterRequest.class);

        assertEquals("Alex Rivera", request.name());
        assertEquals("alex@example.com", request.email());
        assertEquals("secret123", request.password());
        assertEquals("123 Main St", request.address());
        assertEquals("480-555-0101", request.phone());
    }

    @Test
    public void registerRequestAllowsMissingOptionalFields() throws Exception {
        String json = "{\"name\":\"Alex\",\"email\":\"alex@example.com\",\"password\":\"secret123\"}";

        RegisterRequest request = mapper.readValue(json, RegisterRequest.class);

        assertNull(request.address());
        assertNull(request.phone());
    }

    @Test
    public void loginRequestReadsEmailAndPassword() throws Exception {
        LoginRequest request = mapper.readValue(
                "{\"email\":\"alex@example.com\",\"password\":\"secret123\"}", LoginRequest.class);

        assertEquals("alex@example.com", request.email());
        assertEquals("secret123", request.password());
    }

    @Test
    public void loginResponseHasTokenAndUserWithoutPassword() throws Exception {
        UserDto user = new UserDto(7, "Alex Rivera", "alex@example.com", Role.CITIZEN,
                "123 Main St", "480-555-0101", null);

        JsonNode json = mapper.valueToTree(new LoginResponse("abc-123", user));

        assertEquals("abc-123", json.get("token").asText());
        JsonNode u = json.get("user");
        assertEquals(7, u.get("id").asInt());
        assertEquals("Alex Rivera", u.get("name").asText());
        assertEquals("alex@example.com", u.get("email").asText());
        assertEquals("CITIZEN", u.get("role").asText());
        assertEquals("123 Main St", u.get("address").asText());
        assertEquals("480-555-0101", u.get("phone").asText());
        assertTrue("departmentId must be present as null", u.has("departmentId"));
        assertTrue(u.get("departmentId").isNull());
        assertEquals("UserDto has exactly the 7 fields from api.md", 7, u.size());
        assertFalse(json.toString().toLowerCase().contains("password"));
    }

    @Test
    public void staffUserHasDepartmentId() {
        UserDto worker = new UserDto(4, "Sam Lee", "worker@smartcity.test", Role.WORKER, null, null, 1);

        JsonNode json = mapper.valueToTree(worker);

        assertEquals("WORKER", json.get("role").asText());
        assertEquals(1, json.get("departmentId").asInt());
    }

    @Test
    public void errorResponseOmitsFieldsWhenThereAreNone() {
        JsonNode json = mapper.valueToTree(new ErrorResponse("EMAIL_TAKEN", "Email is already registered."));

        assertEquals("EMAIL_TAKEN", json.get("error").asText());
        assertEquals("Email is already registered.", json.get("message").asText());
        assertFalse(json.has("fields"));
    }

    @Test
    public void errorResponseIncludesFieldsForValidationErrors() {
        ErrorResponse error = new ErrorResponse("VALIDATION_FAILED", "Some fields are invalid.",
                Map.of("email", "Invalid email format"));

        JsonNode json = mapper.valueToTree(error);

        assertEquals("Invalid email format", json.get("fields").get("email").asText());
    }

    @Test
    public void healthResponseHasStatusAndDatabase() {
        JsonNode json = mapper.valueToTree(new HealthResponse("UP", "DOWN"));

        assertEquals("UP", json.get("status").asText());
        assertEquals("DOWN", json.get("database").asText());
    }
}
