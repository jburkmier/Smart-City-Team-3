package edu.asu.smartcity.server.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import edu.asu.smartcity.server.model.NotificationType;
import org.junit.Test;

import java.time.LocalDateTime;

import static org.junit.Assert.*;

/** Checks that NotificationDto matches the JSON response documented in docs/api.md. */
public class NotificationDtoJsonTest {

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    public void notificationDtoWritesTheDocumentedFieldsAndDateFormat() {
        NotificationDto notification = new NotificationDto(88, NotificationType.REQUEST_STATUS,
                "Your request #41 (POTHOLE) is now IN_PROGRESS.", 41, false, LocalDateTime.of(2026, 11, 17, 9, 0));

        JsonNode json = mapper.valueToTree(notification);

        assertEquals(6, json.size());
        assertEquals(88, json.get("id").asInt());
        assertEquals("REQUEST_STATUS", json.get("type").asText());
        assertEquals("Your request #41 (POTHOLE) is now IN_PROGRESS.", json.get("message").asText());
        assertEquals(41, json.get("relatedId").asInt());
        assertFalse(json.get("read").asBoolean());
        assertEquals("2026-11-17T09:00:00", json.get("createdAt").asText());
    }

    @Test
    public void notificationDtoWritesMissingRelatedIdAsNull() {
        NotificationDto notification = new NotificationDto(89, NotificationType.ANNOUNCEMENT,
                "City offices are closed Friday.", null, true, LocalDateTime.of(2026, 11, 17, 9, 0));

        JsonNode json = mapper.valueToTree(notification);

        assertTrue(json.has("relatedId"));
        assertTrue(json.get("relatedId").isNull());
        assertTrue(json.get("read").asBoolean());
    }
}
