package edu.asu.smartcity.server.filter;

import edu.asu.smartcity.server.dto.EventDto;

import java.util.List;

/** Filters events by district. */
public class LocationFilter implements EventFilterStrategy {

    private final String district;

    public LocationFilter(String district) {
        this.district = district;
    }

    @Override
    public List<EventDto> filter(List<EventDto> events) {
        return events.stream()
                .filter(event -> district.equals(event.district()))
                .toList();
    }
}
