package edu.asu.smartcity.server.filter;

import edu.asu.smartcity.server.dto.EventDto;

import java.util.List;

/** Strategy interface for filtering an event collection. */
@FunctionalInterface
public interface EventFilterStrategy {
    List<EventDto> filter(List<EventDto> events);
}
