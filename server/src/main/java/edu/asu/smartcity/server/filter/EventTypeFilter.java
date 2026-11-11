package edu.asu.smartcity.server.filter;

import edu.asu.smartcity.server.dto.EventDto;

import java.util.List;

/** Filters events by category. */
public class EventTypeFilter implements EventFilterStrategy {

    private final String category;

    public EventTypeFilter(String category) {
        this.category = category;
    }

    @Override
    public List<EventDto> filter(List<EventDto> events) {
        return events.stream()
                .filter(event -> category.equals(event.category()))
                .toList();
    }
}
