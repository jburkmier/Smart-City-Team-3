package edu.asu.smartcity.server.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import edu.asu.smartcity.server.model.RequestStatus;
import edu.asu.smartcity.server.model.RequestType;
import org.junit.Test;

import java.time.LocalDateTime;

import static org.junit.Assert.*;

/** Checks that RequestDto matches the JSON response documented in docs/api.md. */
public class RequestDtoJsonTest {

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    public void requestDtoWritesTheDocumentedFieldsAndDateFormat() {
        LocalDateTime submittedAt = LocalDateTime.of(2026, 11, 9, 10, 15);
        RequestDto request = new RequestDto(41, RequestType.POTHOLE, "Large pothole in the right lane.",
                "5th St & Mill Ave", RequestStatus.ASSIGNED, 7, "Alex Rivera", 1, "Public Works",
                null, null, null, null, submittedAt, LocalDateTime.of(2026, 11, 10, 8, 2), null);

        JsonNode json = mapper.valueToTree(request);

        assertEquals(16, json.size());
        assertEquals(41, json.get("id").asInt());
        assertEquals("POTHOLE", json.get("type").asText());
        assertEquals("ASSIGNED", json.get("status").asText());
        assertEquals("Alex Rivera", json.get("citizenName").asText());
        assertEquals("2026-11-09T10:15:00", json.get("submittedAt").asText());
        assertTrue(json.has("assignedWorkerId"));
        assertTrue(json.get("assignedWorkerId").isNull());
        assertTrue(json.get("resolvedAt").isNull());
    }
}
