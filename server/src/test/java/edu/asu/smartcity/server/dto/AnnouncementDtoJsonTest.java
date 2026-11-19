package edu.asu.smartcity.server.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import edu.asu.smartcity.server.model.AnnouncementType;
import org.junit.Test;

import java.time.LocalDateTime;

import static org.junit.Assert.*;

/** Checks that announcement JSON matches docs/api.md. */
public class AnnouncementDtoJsonTest {

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    public void announcementDtoWritesTheDocumentedFieldsAndDateFormat() {
        AnnouncementDto announcement = new AnnouncementDto(2, AnnouncementType.EMERGENCY, "Heat Advisory",
                "Temperatures above 110°F expected. Stay hydrated.", 2, LocalDateTime.of(2026, 11, 17, 7, 30));

        JsonNode json = mapper.valueToTree(announcement);

        assertEquals(6, json.size());
        assertEquals(2, json.get("id").asInt());
        assertEquals("EMERGENCY", json.get("type").asText());
        assertEquals("Heat Advisory", json.get("title").asText());
        assertEquals("Temperatures above 110°F expected. Stay hydrated.", json.get("message").asText());
        assertEquals(2, json.get("createdById").asInt());
        assertEquals("2026-11-17T07:30:00", json.get("createdAt").asText());
    }

    @Test
    public void announcementCreateRequestReadsTheDocumentedFields() throws Exception {
        AnnouncementCreateRequest request = mapper.readValue(
                "{\"type\":\"EMERGENCY\",\"title\":\"Heat Advisory\",\"message\":\"Stay hydrated.\"}",
                AnnouncementCreateRequest.class);

        assertEquals(AnnouncementType.EMERGENCY, request.type());
        assertEquals("Heat Advisory", request.title());
        assertEquals("Stay hydrated.", request.message());
    }
}
