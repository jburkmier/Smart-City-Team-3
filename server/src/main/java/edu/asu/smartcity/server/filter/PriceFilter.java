package edu.asu.smartcity.server.filter;

import edu.asu.smartcity.server.dto.EventDto;

import java.math.BigDecimal;
import java.util.List;

/** Filters for free events or events with a positive price. */
public class PriceFilter implements EventFilterStrategy {

    private static final BigDecimal FREE_PRICE = BigDecimal.ZERO;

    private final boolean free;

    public PriceFilter(boolean free) {
        this.free = free;
    }

    @Override
    public List<EventDto> filter(List<EventDto> events) {
        return events.stream()
                .filter(event -> (event.price().compareTo(FREE_PRICE) == 0) == free)
                .toList();
    }
}
