package edu.asu.smartcity.server.filter;

import edu.asu.smartcity.server.dto.EventDto;

import java.util.List;
import java.util.Locale;

/** Matches a case-insensitive keyword in an event title or description. */
public class KeywordFilter implements EventFilterStrategy {

    private final String keyword;

    public KeywordFilter(String keyword) {
        this.keyword = keyword.trim().toLowerCase(Locale.ROOT);
    }

    @Override
    public List<EventDto> filter(List<EventDto> events) {
        return events.stream()
                .filter(event -> event.title().toLowerCase(Locale.ROOT).contains(keyword)
                        || event.description() != null
                        && event.description().toLowerCase(Locale.ROOT).contains(keyword))
                .toList();
    }
}
