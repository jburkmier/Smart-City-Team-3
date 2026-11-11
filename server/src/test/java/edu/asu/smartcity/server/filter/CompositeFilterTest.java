package edu.asu.smartcity.server.filter;

import edu.asu.smartcity.server.catalog.EventCatalog;
import edu.asu.smartcity.server.dto.EventDto;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class CompositeFilterTest {

    @Test
    public void compositeKeepsEventsMatchingEverySelectedFilter() {
        EventDto matching = event("River cleanup", "Meet at the park", "ENVIRONMENT",
                "NORTH", LocalDateTime.of(2099, 6, 2, 9, 0), "0.00");
        EventDto wrongCategory = event("River cleanup", "Meet at the park", "CULTURAL",
                "NORTH", LocalDateTime.of(2099, 6, 2, 9, 0), "0.00");
        EventDto wrongDistrict = event("River cleanup", "Meet at the park", "ENVIRONMENT",
                "SOUTH", LocalDateTime.of(2099, 6, 2, 9, 0), "0.00");
        EventDto wrongDate = event("River cleanup", "Meet at the park", "ENVIRONMENT",
                "NORTH", LocalDateTime.of(2099, 6, 11, 9, 0), "0.00");
        EventDto wrongPrice = event("River cleanup", "Meet at the park", "ENVIRONMENT",
                "NORTH", LocalDateTime.of(2099, 6, 2, 9, 0), "5.00");

        CompositeFilter filters = new CompositeFilter(List.of(
                new KeywordFilter(" PARK "),
                new EventTypeFilter("ENVIRONMENT"),
                new DateFilter(LocalDate.of(2099, 6, 1), LocalDate.of(2099, 6, 10)),
                new LocationFilter("NORTH"),
                new PriceFilter(true)));

        assertEquals(List.of(matching), filters.filter(
                List.of(matching, wrongCategory, wrongDistrict, wrongDate, wrongPrice)));
    }

    @Test
    public void catalogReturnsAllEventsWithoutAFilter() {
        List<EventDto> events = List.of(event("Cleanup", null, "ENVIRONMENT", "NORTH",
                LocalDateTime.of(2099, 6, 2, 9, 0), "0.00"));
        EventCatalog catalog = new EventCatalog(events);

        assertEquals(events, catalog.getEvents());
        catalog.setStrategy(new CompositeFilter(List.of()));
        assertEquals(events, catalog.getEvents());
    }

    private static EventDto event(String title, String description, String category, String district,
                                  LocalDateTime startTime, String price) {
        return new EventDto(1, title, description, category, startTime, district, null,
                new BigDecimal(price), 0, false, 2);
    }
}
