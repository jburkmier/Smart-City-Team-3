package edu.asu.smartcity.server.filter;

import edu.asu.smartcity.server.dto.EventDto;

import java.time.LocalDate;
import java.util.List;

/** Filters events by their local start date, inclusively. */
public class DateFilter implements EventFilterStrategy {

    private final LocalDate from;
    private final LocalDate to;

    public DateFilter(LocalDate from, LocalDate to) {
        this.from = from;
        this.to = to;
    }

    @Override
    public List<EventDto> filter(List<EventDto> events) {
        return events.stream()
                .filter(event -> from == null || !event.startTime().toLocalDate().isBefore(from))
                .filter(event -> to == null || !event.startTime().toLocalDate().isAfter(to))
                .toList();
    }
}
