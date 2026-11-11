package edu.asu.smartcity.server.service;

import edu.asu.smartcity.server.dao.EventDAO;
import edu.asu.smartcity.server.dto.EventCreateRequest;
import edu.asu.smartcity.server.dto.EventDto;
import edu.asu.smartcity.server.web.ApiException;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class EventServiceTest {

    @Test
    public void createRejectsInvalidEventFields() throws Exception {
        EventCreateRequest request = new EventCreateRequest(
                " ", "x".repeat(2001), "INVALID", LocalDateTime.now().minusMinutes(1),
                "INVALID", "x".repeat(256), new BigDecimal("-0.01"));
        EventService service = new EventService(null);

        try {
            service.create(request, 2);
            fail("Expected validation to fail.");
        } catch (ApiException e) {
            assertEquals(400, e.status());
            assertEquals("VALIDATION_FAILED", e.code());
            assertEquals(Map.of(
                    "title", "Must be 1-150 characters.",
                    "description", "Must be at most 2000 characters.",
                    "category", "Must be a valid event category.",
                    "startTime", "Must be a future date and time.",
                    "district", "Must be a valid district.",
                    "locationDetail", "Must be at most 255 characters.",
                    "price", "Must be between 0 and 999999.99 with at most 2 decimal places."
            ), e.fields());
        }
    }

    @Test
    public void createRejectsNullRequest() throws Exception {
        EventService service = new EventService(null);

        try {
            service.create(null, 2);
            fail("Expected validation to fail.");
        } catch (ApiException e) {
            assertEquals(400, e.status());
            assertEquals("VALIDATION_FAILED", e.code());
        }
    }

    @Test
    public void searchRejectsInvalidFilters() throws Exception {
        EventService service = new EventService(null);

        try {
            service.findUpcoming(1, null, "INVALID", "not-a-date", "2026-11-01",
                    "INVALID", "sometimes", "recent");
            fail("Expected filter validation to fail.");
        } catch (ApiException e) {
            assertEquals(400, e.status());
            assertEquals("VALIDATION_FAILED", e.code());
            assertEquals("Must be a valid event category.", e.fields().get("category"));
            assertEquals("Must be a date in YYYY-MM-DD format.", e.fields().get("from"));
            assertEquals("Must be a valid district.", e.fields().get("district"));
            assertEquals("Must be true or false.", e.fields().get("free"));
            assertEquals("Must be date or popularity.", e.fields().get("sort"));
        }
    }

    @Test
    public void searchRejectsReversedDateRange() throws Exception {
        EventService service = new EventService(null);

        try {
            service.findUpcoming(1, null, null, "2026-11-20", "2026-11-10",
                    null, null, null);
            fail("Expected filter validation to fail.");
        } catch (ApiException e) {
            assertEquals("Must be on or before 'to'.", e.fields().get("from"));
        }
    }

    @Test
    public void searchBuildsStrategiesAndAppliesThemToUpcomingEvents() throws Exception {
        EventDto matching = event("River cleanup", "Meet at the park", "ENVIRONMENT",
                "NORTH", LocalDate.of(2099, 6, 2), "0.00");
        EventDto wrongDistrict = event("River cleanup", "Meet at the park", "ENVIRONMENT",
                "SOUTH", LocalDate.of(2099, 6, 2), "0.00");
        EventDAO dao = new EventDAO(null) {
            @Override
            public List<EventDto> findUpcoming(int userId, String sort) {
                assertEquals(7, userId);
                assertEquals("popularity", sort);
                return List.of(matching, wrongDistrict);
            }
        };
        EventService service = new EventService(dao);

        assertEquals(List.of(matching), service.findUpcoming(7, " PARK ", "ENVIRONMENT",
                "2099-06-01", "2099-06-10", "NORTH", "true", "popularity"));
    }

    private static EventDto event(String title, String description, String category, String district,
                                  LocalDate startDate, String price) {
        return new EventDto(1, title, description, category, startDate.atTime(9, 0), district, null,
                new BigDecimal(price), 0, false, 2);
    }
}
