package edu.asu.smartcity.server.service;

import edu.asu.smartcity.server.dao.RequestDAO;
import edu.asu.smartcity.server.domain.Request;
import edu.asu.smartcity.server.dto.RequestCreateRequest;
import edu.asu.smartcity.server.dto.RequestAssignRequest;
import edu.asu.smartcity.server.dto.RequestDto;
import edu.asu.smartcity.server.dto.RequestRejectRequest;
import edu.asu.smartcity.server.model.RequestStatus;
import edu.asu.smartcity.server.model.NotificationType;
import edu.asu.smartcity.server.model.RequestType;
import edu.asu.smartcity.server.observer.Notice;
import edu.asu.smartcity.server.observer.Subject;
import edu.asu.smartcity.server.web.ApiException;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
        private boolean contentUpdateSucceeds = true;

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
        public boolean updateContent(int requestId, RequestType type, String description, String location) {
            if (!contentUpdateSucceeds || current == null) {
                return false;
            }
            current = new RequestDto(current.id(), type, description, location, current.status(),
                    current.citizenId(), current.citizenName(), current.departmentId(), current.departmentName(),
                    current.assignedWorkerId(), current.assignedWorkerName(), current.rejectionReason(),
                    current.resolutionNotes(), current.submittedAt(), current.updatedAt(), current.resolvedAt());
            return true;
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

        @Override
        public boolean reject(int requestId, RequestStatus expectedStatus, RequestStatus nextStatus, String reason) {
            if (conflictOnUpdate) {
                current = requestDto(nextStatus, null, reason);
                return false;
            }
            if (current == null || current.status() != expectedStatus) {
                return false;
            }
            current = requestDto(nextStatus, null, reason);
            return true;
        }

        private RequestDto requestDto(RequestStatus status) {
            return requestDto(status, null, null);
        }

        private RequestDto requestDto(RequestStatus status, Integer departmentId) {
            return requestDto(status, departmentId, null);
        }

        private RequestDto requestDto(RequestStatus status, Integer departmentId, String rejectionReason) {
            return new RequestDto(2, RequestType.POTHOLE, "Large pothole", "5th St", status,
                    7, "Alex Rivera", departmentId, departmentId == null ? null : "Public Works",
                    null, null, rejectionReason, null,
                    LocalDateTime.now(), LocalDateTime.now(), null);
        }
    }

    /** Records each notice and its recipients instead of writing to the database. */
    private static class FakeNotificationService extends NotificationService {
        private final List<Notice> notices = new ArrayList<>();
        private final List<Collection<Integer>> recipients = new ArrayList<>();

        FakeNotificationService() {
            super(null);
        }

        @Override
        public void send(Subject subject, Collection<Integer> recipientIds) {
            notices.add(subject.getState());
            recipients.add(List.copyOf(recipientIds));
        }
    }

    private final FakeRequestDAO requests = new FakeRequestDAO();
    private final FakeNotificationService notifications = new FakeNotificationService();
    private final RequestService service = new RequestService(requests, null, notifications);

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
    public void updateContentTrimsFieldsAndPreservesWorkflowState() throws Exception {
        requests.current = requests.requestDto(RequestStatus.IN_PROGRESS, 1);

        RequestDto response = service.updateContent(2,
                new RequestCreateRequest(RequestType.GRAFFITI, "  New description  ", "  New location  "));

        assertEquals(RequestType.GRAFFITI, response.type());
        assertEquals("New description", response.description());
        assertEquals("New location", response.location());
        assertEquals(RequestStatus.IN_PROGRESS, response.status());
        assertEquals(Integer.valueOf(1), response.departmentId());
    }

    @Test
    public void updateContentReturnsNotFoundWhenRequestDoesNotExist() {
        requests.current = null;
        requests.contentUpdateSucceeds = false;

        ApiException error = assertThrows(ApiException.class, () -> service.updateContent(99,
                new RequestCreateRequest(RequestType.OTHER, "Description", "Location")));

        assertEquals(404, error.status());
        assertEquals("NOT_FOUND", error.code());
    }

    @Test
    public void reviewMovesSubmittedRequestToUnderReview() throws Exception {
        requests.current = requests.requestDto(RequestStatus.SUBMITTED);

        RequestDto response = service.review(2);

        assertEquals(RequestStatus.UNDER_REVIEW, response.status());
        assertEquals(RequestStatus.UNDER_REVIEW, requests.current.status());
        assertCitizenNotified(response, "Your request #2 (POTHOLE) is now UNDER_REVIEW.");
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
        assertCitizenNotified(response, "Your request #2 (POTHOLE) is now ASSIGNED.");
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
        assertTrue("A failed transition must not notify the citizen.", notifications.notices.isEmpty());
    }

    @Test
    public void rejectMovesUnderReviewRequestAndTrimsReason() throws Exception {
        requests.current = requests.requestDto(RequestStatus.UNDER_REVIEW);

        RequestDto response = service.reject(2, new RequestRejectRequest("  Duplicate request.  "));

        assertEquals(RequestStatus.REJECTED, response.status());
        assertEquals("Duplicate request.", response.rejectionReason());
        assertEquals("Duplicate request.", requests.current.rejectionReason());
        assertCitizenNotified(response, "Your request #2 (POTHOLE) is now REJECTED.");
    }

    @Test
    public void rejectRequiresReasonWithinLimit() {
        ApiException missing = expectRejectError(new RequestRejectRequest(null));
        ApiException blank = expectRejectError(new RequestRejectRequest(" \t "));
        ApiException tooLong = expectRejectError(new RequestRejectRequest("x".repeat(501)));

        assertEquals("Is required.", missing.fields().get("reason"));
        assertEquals("Is required.", blank.fields().get("reason"));
        assertEquals("Must be at most 500 characters.", tooLong.fields().get("reason"));
    }

    @Test
    public void rejectRejectsRequestInWrongState() {
        requests.current = requests.requestDto(RequestStatus.ASSIGNED);

        ApiException error = expectRejectError(new RequestRejectRequest("Not actionable."));

        assertEquals(409, error.status());
        assertEquals("INVALID_TRANSITION", error.code());
        assertEquals("Cannot reject a request that is ASSIGNED.", error.getMessage());
    }

    @Test
    public void rejectReturnsNotFoundForUnknownRequest() {
        ApiException error = expectRejectError(new RequestRejectRequest("Invalid request."));

        assertEquals(404, error.status());
        assertEquals("NOT_FOUND", error.code());
    }

    @Test
    public void rejectReportsConcurrentStateChange() {
        requests.current = requests.requestDto(RequestStatus.UNDER_REVIEW);
        requests.conflictOnUpdate = true;

        ApiException error = expectRejectError(new RequestRejectRequest("Duplicate."));

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

    private ApiException expectRejectError(RequestRejectRequest rejection) {
        try {
            service.reject(2, rejection);
        } catch (ApiException e) {
            return e;
        } catch (Exception e) {
            throw new AssertionError("Expected rejection error but got " + e, e);
        }
        fail("Expected rejection error");
        return null;
    }

    private void assertCitizenNotified(RequestDto request, String message) {
        assertEquals(1, notifications.notices.size());
        Notice notice = notifications.notices.get(0);
        assertEquals(NotificationType.REQUEST_STATUS, notice.type());
        assertEquals(message, notice.message());
        assertEquals(Integer.valueOf(request.id()), notice.relatedId());
        assertEquals(List.of(request.citizenId()), notifications.recipients.get(0));
    }
}
