package edu.asu.smartcity.server.filter;

import edu.asu.smartcity.server.dto.EventDto;

import java.util.List;

/** Applies each selected filter in sequence, retaining events that match all filters. */
public class CompositeFilter implements EventFilterStrategy {

    private final List<EventFilterStrategy> filters;

    public CompositeFilter(List<EventFilterStrategy> filters) {
        this.filters = List.copyOf(filters);
    }

    @Override
    public List<EventDto> filter(List<EventDto> events) {
        List<EventDto> filtered = events;
        for (EventFilterStrategy filter : filters) {
            filtered = filter.filter(filtered);
        }
        return filtered;
    }
}
