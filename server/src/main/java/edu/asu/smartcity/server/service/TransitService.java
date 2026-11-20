package edu.asu.smartcity.server.service;

import edu.asu.smartcity.server.dao.TransitDAO;
import edu.asu.smartcity.server.dto.TransitRouteDto;
import edu.asu.smartcity.server.web.ApiException;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/** Check Public Transit Schedule: reads the seeded schedule, optionally for one route. */
public class TransitService {

    /** The longest name the transit_routes.route column holds. */
    static final int MAX_ROUTE_LENGTH = 20;

    private final TransitDAO transit;

    public TransitService(TransitDAO transit) {
        this.transit = transit;
    }

    /** A blank or missing route returns the whole schedule; an unknown route returns an empty list. */
    public List<TransitRouteDto> findSchedule(String route) throws SQLException {
        String trimmed = route == null || route.isBlank() ? null : route.trim();
        if (trimmed != null && trimmed.length() > MAX_ROUTE_LENGTH) {
            throw new ApiException(400, "VALIDATION_FAILED", "Some filters are invalid.",
                    Map.of("route", "Must be at most " + MAX_ROUTE_LENGTH + " characters."));
        }
        return transit.findSchedule(trimmed);
    }
}
