package edu.asu.smartcity.server.service;

import edu.asu.smartcity.server.dao.RequestDAO;
import edu.asu.smartcity.server.domain.Request;
import edu.asu.smartcity.server.dto.RequestCreateRequest;
import edu.asu.smartcity.server.dto.RequestAssignRequest;
import edu.asu.smartcity.server.dto.RequestDto;
import edu.asu.smartcity.server.model.RequestStatus;
import edu.asu.smartcity.server.model.RequestType;
import edu.asu.smartcity.server.web.ApiException;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.*;

/** Checks citizen request submission validation and its initial state. */
public class RequestServiceTest {

    private static class FakeRequestDAO extends RequestDAO {
        private Request saved;
        private RequestDto current;
        private boolean conflictOnUpdate;
        private boolean validDepartment = true;

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

        @Override
        public Optional<RequestDto> findById(int requestId) {
            return Optional.ofNullable(current);
        }

        @Override
        public boolean updateStatus(int requestId, RequestStatus expectedStatus, RequestStatus nextStatus) {
            if (conflictOnUpdate) {
                current = requestDto(nextStatus);
                return false;
            }
            if (current == null || current.status() != expectedStatus) {
                return false;
            }
            current = requestDto(nextStatus);
            return true;
        }

        @Override
        public boolean departmentExists(int departmentId) {
            return validDepartment && departmentId == 1;
        }

        @Override
        public boolean assign(int requestId, RequestStatus expectedStatus, RequestStatus nextStatus,
                              int departmentId) {
            if (conflictOnUpdate) {
                current = requestDto(nextStatus, departmentId);
                return false;
            }
            if (current == null || current.status() != expectedStatus || !departmentExists(departmentId)) {
                return false;
            }
            current = requestDto(nextStatus, departmentId);
            return true;
        }

        private RequestDto requestDto(RequestStatus status) {
            return requestDto(status, null);
        }

        private RequestDto requestDto(RequestStatus status, Integer departmentId) {
            return new RequestDto(2, RequestType.POTHOLE, "Large pothole", "5th St", status,
                    7, "Alex Rivera", departmentId, departmentId == null ? null : "Public Works",
                    null, null, null, null,
                    LocalDateTime.now(), LocalDateTime.now(), null);
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

    @Test
    public void reviewMovesSubmittedRequestToUnderReview() throws Exception {
        requests.current = requests.requestDto(RequestStatus.SUBMITTED);

        RequestDto response = service.review(2);

        assertEquals(RequestStatus.UNDER_REVIEW, response.status());
        assertEquals(RequestStatus.UNDER_REVIEW, requests.current.status());
    }

    @Test
    public void reviewRejectsRequestInWrongState() {
        requests.current = requests.requestDto(RequestStatus.UNDER_REVIEW);

        ApiException error = expectReviewError();

        assertEquals(409, error.status());
        assertEquals("INVALID_TRANSITION", error.code());
        assertEquals("Cannot review a request that is UNDER_REVIEW.", error.getMessage());
    }

    @Test
    public void reviewReturnsNotFoundForUnknownRequest() {
        ApiException error = expectReviewError();

        assertEquals(404, error.status());
        assertEquals("NOT_FOUND", error.code());
    }

    @Test
    public void reviewReportsConcurrentStateChange() {
        requests.current = requests.requestDto(RequestStatus.SUBMITTED);
        requests.conflictOnUpdate = true;

        ApiException error = expectReviewError();

        assertEquals(409, error.status());
        assertEquals("INVALID_TRANSITION", error.code());
        assertEquals("Cannot review a request that is UNDER_REVIEW.", error.getMessage());
    }

    @Test
    public void assignMovesUnderReviewRequestAndSetsDepartment() throws Exception {
        requests.current = requests.requestDto(RequestStatus.UNDER_REVIEW);

        RequestDto response = service.assign(2, new RequestAssignRequest(1));

        assertEquals(RequestStatus.ASSIGNED, response.status());
        assertEquals(Integer.valueOf(1), response.departmentId());
        assertEquals("Public Works", response.departmentName());
    }

    @Test
    public void assignRejectsUnknownDepartment() {
        requests.validDepartment = false;

        ApiException error = expectAssignError(new RequestAssignRequest(99));

        assertEquals(400, error.status());
        assertEquals("VALIDATION_FAILED", error.code());
        assertEquals("Department does not exist.", error.fields().get("departmentId"));
    }

    @Test
    public void assignRejectsMissingOrInvalidDepartmentId() {
        ApiException missingId = expectAssignError(new RequestAssignRequest(null));
        ApiException nonPositiveId = expectAssignError(new RequestAssignRequest(0));

        assertEquals("Must be a positive integer.", missingId.fields().get("departmentId"));
        assertEquals("Must be a positive integer.", nonPositiveId.fields().get("departmentId"));
    }

    @Test
    public void assignRejectsRequestInWrongState() {
        requests.current = requests.requestDto(RequestStatus.SUBMITTED);

        ApiException error = expectAssignError(new RequestAssignRequest(1));

        assertEquals(409, error.status());
        assertEquals("INVALID_TRANSITION", error.code());
        assertEquals("Cannot assign a request that is SUBMITTED.", error.getMessage());
    }

    @Test
    public void assignReportsConcurrentStateChange() {
        requests.current = requests.requestDto(RequestStatus.UNDER_REVIEW);
        requests.conflictOnUpdate = true;

        ApiException error = expectAssignError(new RequestAssignRequest(1));

        assertEquals(409, error.status());
        assertEquals("INVALID_TRANSITION", error.code());
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

    private ApiException expectReviewError() {
        try {
            service.review(2);
        } catch (ApiException e) {
            return e;
        } catch (Exception e) {
            throw new AssertionError("Expected review error but got " + e, e);
        }
        fail("Expected review error");
        return null;
    }

    private ApiException expectAssignError(RequestAssignRequest assignment) {
        try {
            service.assign(2, assignment);
        } catch (ApiException e) {
            return e;
        } catch (Exception e) {
            throw new AssertionError("Expected assignment error but got " + e, e);
        }
        fail("Expected assignment error");
        return null;
    }
}
