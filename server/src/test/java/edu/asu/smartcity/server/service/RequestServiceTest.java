package edu.asu.smartcity.server.service;

import edu.asu.smartcity.server.dao.RequestDAO;
import edu.asu.smartcity.server.domain.Request;
import edu.asu.smartcity.server.dto.RequestCreateRequest;
import edu.asu.smartcity.server.dto.RequestDto;
import edu.asu.smartcity.server.model.RequestStatus;
import edu.asu.smartcity.server.model.RequestType;
import edu.asu.smartcity.server.web.ApiException;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.Assert.*;

/** Checks citizen request submission validation and its initial state. */
public class RequestServiceTest {

    private static class FakeRequestDAO extends RequestDAO {
        private Request saved;

        FakeRequestDAO() {
            super(null);
        }

        @Override
        public RequestDto create(Request request) {
            saved = request;
            return new RequestDto(1, request.getType(), request.getDescription(), request.getLocation(),
                    RequestStatus.valueOf(request.getStatusName()), request.getCitizenId(), "Alex Rivera",
                    null, null, null, null, null, null, LocalDateTime.now(), LocalDateTime.now(), null);
        }
    }

    private final FakeRequestDAO requests = new FakeRequestDAO();
    private final RequestService service = new RequestService(requests);

    @Test
    public void createTrimsFieldsAndStartsInSubmittedState() throws Exception {
        RequestDto response = service.create(
                new RequestCreateRequest(RequestType.POTHOLE, "  Large pothole  ", "  5th St  "), 7);

        assertEquals(RequestStatus.SUBMITTED, response.status());
        assertEquals(7, requests.saved.getCitizenId());
        assertEquals("Large pothole", requests.saved.getDescription());
        assertEquals("5th St", requests.saved.getLocation());
        assertEquals(RequestStatus.SUBMITTED.name(), requests.saved.getStatusName());
    }

    @Test
    public void createReportsAllInvalidFields() {
        ApiException error = expectValidationError(
                new RequestCreateRequest(null, "  ", "x".repeat(256)));

        assertEquals(400, error.status());
        assertEquals("VALIDATION_FAILED", error.code());
        assertTrue(error.fields().containsKey("type"));
        assertTrue(error.fields().containsKey("description"));
        assertTrue(error.fields().containsKey("location"));
        assertNull(requests.saved);
    }

    @Test
    public void createRejectsDescriptionOverLimit() {
        ApiException error = expectValidationError(
                new RequestCreateRequest(RequestType.OTHER, "x".repeat(2001), "Location"));

        assertEquals(Map.of("description", "Must be at most 2000 characters."), error.fields());
        assertNull(requests.saved);
    }

    @Test
    public void createRejectsMissingBody() {
        ApiException error = expectValidationError(null);

        assertTrue(error.fields().containsKey("request"));
        assertNull(requests.saved);
    }

    private ApiException expectValidationError(RequestCreateRequest request) {
        try {
            service.create(request, 7);
        } catch (ApiException e) {
            return e;
        } catch (Exception e) {
            throw new AssertionError("Expected validation error but got " + e, e);
        }
        fail("Expected validation error");
        return null;
    }
}
