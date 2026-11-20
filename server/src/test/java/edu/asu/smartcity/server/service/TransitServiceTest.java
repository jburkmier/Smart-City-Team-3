package edu.asu.smartcity.server.service;

import edu.asu.smartcity.server.dao.TransitDAO;
import edu.asu.smartcity.server.dto.TransitRouteDto;
import edu.asu.smartcity.server.web.ApiException;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** Runs TransitService against an in-memory TransitDAO, so no database is needed. */
public class TransitServiceTest {

    private static class FakeTransitDAO extends TransitDAO {
        private final List<String> requestedRoutes = new ArrayList<>();
        private final List<TransitRouteDto> trips = List.of(
                new TransitRouteDto(1, "21A", "City Hall Plaza", "07:15", "07:42"),
                new TransitRouteDto(5, "45", "Riverside Park", "06:50", "07:20"));

        FakeTransitDAO() {
            super(null);
        }

        @Override
        public List<TransitRouteDto> findSchedule(String route) {
            requestedRoutes.add(route);
            return route == null ? trips : trips.stream().filter(trip -> trip.route().equals(route)).toList();
        }
    }

    private final FakeTransitDAO dao = new FakeTransitDAO();
    private final TransitService service = new TransitService(dao);

    @Test
    public void missingOrBlankRouteReturnsTheWholeSchedule() throws Exception {
        assertEquals(2, service.findSchedule(null).size());
        assertEquals(2, service.findSchedule("  ").size());
        assertNull(dao.requestedRoutes.get(0));
        assertNull(dao.requestedRoutes.get(1));
    }

    @Test
    public void routeIsTrimmedAndFiltersTheSchedule() throws Exception {
        List<TransitRouteDto> trips = service.findSchedule(" 45 ");

        assertEquals("45", dao.requestedRoutes.get(0));
        assertEquals(1, trips.size());
        assertEquals("Riverside Park", trips.get(0).stop());
    }

    @Test
    public void unknownRouteReturnsAnEmptyList() throws Exception {
        assertTrue(service.findSchedule("99X").isEmpty());
    }

    @Test
    public void routeLongerThanTheColumnIsRejected() throws Exception {
        try {
            service.findSchedule("x".repeat(TransitService.MAX_ROUTE_LENGTH + 1));
            fail("Expected VALIDATION_FAILED");
        } catch (ApiException e) {
            assertEquals(400, e.status());
            assertTrue(e.fields().containsKey("route"));
        }
        assertTrue(dao.requestedRoutes.isEmpty());
    }
}
