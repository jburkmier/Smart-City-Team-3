package edu.asu.smartcity.server.service;

import edu.asu.smartcity.server.catalog.EventCatalog;
import edu.asu.smartcity.server.dao.EventDAO;
import edu.asu.smartcity.server.dto.EventCreateRequest;
import edu.asu.smartcity.server.dto.EventDto;
import edu.asu.smartcity.server.filter.CompositeFilter;
import edu.asu.smartcity.server.filter.DateFilter;
import edu.asu.smartcity.server.filter.EventFilterStrategy;
import edu.asu.smartcity.server.filter.EventTypeFilter;
import edu.asu.smartcity.server.filter.KeywordFilter;
import edu.asu.smartcity.server.filter.LocationFilter;
import edu.asu.smartcity.server.filter.PriceFilter;
import edu.asu.smartcity.server.web.ApiException;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Validates and creates community events. */
public class EventService {

    private static final Set<String> CATEGORIES = Set.of(
            "COMMUNITY_SERVICE", "SPORTS", "PUBLIC_SAFETY", "EDUCATION", "ENVIRONMENT", "CULTURAL");
    private static final Set<String> DISTRICTS = Set.of("DOWNTOWN", "NORTH", "SOUTH", "EAST", "WEST");

    private final EventDAO events;

    public EventService(EventDAO events) {
        this.events = events;
    }

    public List<EventDto> findUpcoming(int userId, String q, String category, String from, String to,
                                       String district, String free, String sort) throws SQLException {
        Map<String, String> fields = new LinkedHashMap<>();
        if (category != null && !CATEGORIES.contains(category)) {
            fields.put("category", "Must be a valid event category.");
        }
        if (district != null && !DISTRICTS.contains(district)) {
            fields.put("district", "Must be a valid district.");
        }
        LocalDate fromDate = parseDate("from", from, fields);
        LocalDate toDate = parseDate("to", to, fields);
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            fields.put("from", "Must be on or before 'to'.");
        }
        Boolean freeOnly = null;
        if (free != null) {
            if ("true".equalsIgnoreCase(free)) {
                freeOnly = true;
            } else if ("false".equalsIgnoreCase(free)) {
                freeOnly = false;
            } else {
                fields.put("free", "Must be true or false.");
            }
        }
        String normalizedSort = sort == null ? "date" : sort.toLowerCase(Locale.ROOT);
        if (!"date".equals(normalizedSort) && !"popularity".equals(normalizedSort)) {
            fields.put("sort", "Must be date or popularity.");
        }
        if (!fields.isEmpty()) {
            throw new ApiException(400, "VALIDATION_FAILED", "Some filters are invalid.", fields);
        }
        List<EventFilterStrategy> filters = new ArrayList<>();
        if (q != null && !q.isBlank()) {
            filters.add(new KeywordFilter(q));
        }
        if (category != null) {
            filters.add(new EventTypeFilter(category));
        }
        if (fromDate != null || toDate != null) {
            filters.add(new DateFilter(fromDate, toDate));
        }
        if (district != null) {
            filters.add(new LocationFilter(district));
        }
        if (freeOnly != null) {
            filters.add(new PriceFilter(freeOnly));
        }

        EventCatalog catalog = new EventCatalog(events.findUpcoming(userId, normalizedSort));
        catalog.setStrategy(new CompositeFilter(filters));
        return catalog.getEvents();
    }

    public EventDto findVisibleById(int eventId, int userId) throws SQLException {
        return events.findVisibleById(eventId, userId).orElseThrow(EventService::notFound);
    }

    public List<EventDto> findRegistrations(int userId, boolean upcoming) throws SQLException {
        return events.findRegistrations(userId, upcoming);
    }

    public EventDto create(EventCreateRequest request, int createdById) throws SQLException {
        EventCreateRequest valid = validate(request);
        return events.create(valid.title().trim(), valid.description(), valid.category(), valid.startTime(),
                valid.district(), valid.locationDetail(), valid.price(), createdById);
    }

    public EventDto update(int eventId, EventCreateRequest request, int userId) throws SQLException {
        EventCreateRequest valid = validate(request);
        return events.update(eventId, valid.title().trim(), valid.description(), valid.category(),
                valid.startTime(), valid.district(), valid.locationDetail(), valid.price(), userId)
                .orElseThrow(EventService::notFound);
    }

    public void cancel(int eventId) throws SQLException {
        if (!events.cancel(eventId)) {
            throw notFound();
        }
    }

    public EventDto register(int eventId, int userId) throws SQLException {
        if (!events.isVisible(eventId)) {
            throw notFound();
        }
        try {
            if (events.register(eventId, userId)) {
                return findVisibleById(eventId, userId);
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                throw new ApiException(409, "ALREADY_REGISTERED", "You are already registered for this event.");
            }
            throw e;
        }
        if (!events.isVisible(eventId)) {
            throw notFound();
        }
        throw new ApiException(400, "VALIDATION_FAILED", "This event has already started.");
    }

    public void unregister(int eventId, int userId) throws SQLException {
        if (!events.unregister(eventId, userId)) {
            throw notFound();
        }
    }

    private static EventCreateRequest validate(EventCreateRequest request) {
        Map<String, String> fields = new LinkedHashMap<>();
        if (request == null) {
            throw validationError();
        }

        String title = request.title() == null ? null : request.title().trim();
        if (title == null || title.isEmpty() || title.length() > 150) {
            fields.put("title", "Must be 1-150 characters.");
        }
        if (request.description() != null && request.description().length() > 2000) {
            fields.put("description", "Must be at most 2000 characters.");
        }
        if (request.category() == null || !CATEGORIES.contains(request.category())) {
            fields.put("category", "Must be a valid event category.");
        }
        if (request.startTime() == null || !request.startTime().isAfter(LocalDateTime.now())) {
            fields.put("startTime", "Must be a future date and time.");
        }
        if (request.district() == null || !DISTRICTS.contains(request.district())) {
            fields.put("district", "Must be a valid district.");
        }
        if (request.locationDetail() != null && request.locationDetail().length() > 255) {
            fields.put("locationDetail", "Must be at most 255 characters.");
        }
        if (request.price() == null || request.price().signum() < 0
                || request.price().scale() > 2 || request.price().compareTo(new BigDecimal("999999.99")) > 0) {
            fields.put("price", "Must be between 0 and 999999.99 with at most 2 decimal places.");
        }
        if (!fields.isEmpty()) {
            throw new ApiException(400, "VALIDATION_FAILED", "Some fields are invalid.", fields);
        }
        return request;
    }

    private static LocalDate parseDate(String name, String value, Map<String, String> fields) {
        if (value == null) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (java.time.format.DateTimeParseException e) {
            fields.put(name, "Must be a date in YYYY-MM-DD format.");
            return null;
        }
    }

    private static ApiException validationError() {
        return new ApiException(400, "VALIDATION_FAILED", "The request body must be valid JSON.");
    }

    private static ApiException notFound() {
        return new ApiException(404, "NOT_FOUND", "Event not found.");
    }
}
