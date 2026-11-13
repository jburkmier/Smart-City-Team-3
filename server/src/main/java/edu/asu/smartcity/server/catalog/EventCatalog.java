package edu.asu.smartcity.server.catalog;

import edu.asu.smartcity.server.dto.EventDto;
import edu.asu.smartcity.server.filter.EventFilterStrategy;

import java.util.List;
import java.util.Objects;

/** A request-scoped catalog of upcoming events with an optional filtering strategy. */
public class EventCatalog {

    private final List<EventDto> events;
    private EventFilterStrategy strategy;

    public EventCatalog(List<EventDto> events) {
        this.events = List.copyOf(events);
    }

    public void setStrategy(EventFilterStrategy strategy) {
        this.strategy = Objects.requireNonNull(strategy, "strategy");
    }

    public List<EventDto> getEvents() {
        return strategy == null ? events : strategy.filter(events);
    }
}
