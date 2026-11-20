package edu.asu.smartcity.server.service;

import edu.asu.smartcity.server.dao.RequestDAO;
import edu.asu.smartcity.server.dao.TaskDAO;
import edu.asu.smartcity.server.domain.DepartmentManager;
import edu.asu.smartcity.server.domain.Worker;
import edu.asu.smartcity.server.dto.RequestDto;
import edu.asu.smartcity.server.dto.RequestResolveRequest;
import edu.asu.smartcity.server.dto.TaskAssignRequest;
import edu.asu.smartcity.server.dto.TaskAssignmentResult;
import edu.asu.smartcity.server.dto.TaskDto;
import edu.asu.smartcity.server.model.NotificationType;
import edu.asu.smartcity.server.model.RequestStatus;
import edu.asu.smartcity.server.model.RequestType;
import edu.asu.smartcity.server.observer.Subject;
import edu.asu.smartcity.server.web.ApiException;
import org.junit.Test;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;

public class RequestExecutionServiceTest {

    private static class FakeRequestDAO extends RequestDAO {
        private RequestDto current = requestDto(RequestStatus.ASSIGNED, null, null);

        FakeRequestDAO() {
            super(null);
        }

        @Override
        public Optional<RequestDto> findById(int requestId) {
            return Optional.of(current);
        }

        @Override
        public boolean start(int requestId, Integer assignedWorkerId) {
            if (current.status() != RequestStatus.ASSIGNED) {
                return false;
            }
            if (assignedWorkerId != null
                    && (current.assignedWorkerId() == null || current.assignedWorkerId() != assignedWorkerId)) {
                return false;
            }
            current = requestDto(RequestStatus.IN_PROGRESS, null, null);
            return true;
        }

        @Override
        public boolean resolve(int requestId, String notes, Integer assignedWorkerId) {
            if (current.status() != RequestStatus.IN_PROGRESS) {
                return false;
            }
            if (assignedWorkerId != null
                    && (current.assignedWorkerId() == null || current.assignedWorkerId() != assignedWorkerId)) {
                return false;
            }
            current = requestDto(RequestStatus.RESOLVED, notes, null);
            return true;
        }

        private static RequestDto requestDto(RequestStatus status, String resolutionNotes, Integer workerId) {
            return new RequestDto(2, RequestType.POTHOLE, "Large pothole", "5th St", status,
                    7, "Alex Rivera", 1, "Public Works", workerId, workerId == null ? null : "Sam Lee",
                    null, resolutionNotes, LocalDateTime.now(), LocalDateTime.now(),
                    status == RequestStatus.RESOLVED ? LocalDateTime.now() : null);
        }
    }

    private static class FakeTaskDAO extends TaskDAO {
        private TaskDto task;

        FakeTaskDAO() {
            super(null);
        }

        @Override
        public Optional<TaskAssignmentResult> assign(int requestId, int departmentId, int workerId,
                                                     int managerId, String notes) {
            if (workerId != 4 || departmentId != 1) {
                return Optional.empty();
            }
            boolean created = task == null;
            task = new TaskDto(1, requestId, RequestExecutionServiceTest.FakeRequestDAO
                    .requestDto(RequestStatus.ASSIGNED, null, workerId), workerId, "Sam Lee", managerId,
                    notes, LocalDateTime.now());
            return Optional.of(new TaskAssignmentResult(task, created));
        }

        @Override
        public Optional<TaskDto> findByRequestId(int requestId) {
            return Optional.ofNullable(task);
        }

        @Override
        public List<TaskDto> findByWorker(int workerId) {
            return task != null && task.workerId() == workerId ? List.of(task) : List.of();
        }
    }

    private static class FakeNotificationService extends NotificationService {
        private Subject lastSubject;
        private Collection<Integer> lastRecipients;

        FakeNotificationService() {
            super(null);
        }

        @Override
        public void send(Subject subject, Collection<Integer> recipientIds) {
            lastSubject = subject;
            lastRecipients = List.copyOf(recipientIds);
        }
    }

    private final FakeRequestDAO requests = new FakeRequestDAO();
    private final FakeTaskDAO tasks = new FakeTaskDAO();
    private final FakeNotificationService notifications = new FakeNotificationService();
    private final RequestService service = new RequestService(requests, tasks, notifications);

    @Test
    public void managerAssignsWorkerAndNotifiesThem() throws Exception {
        TaskAssignmentResult result = service.assignTask(2, new TaskAssignRequest(4, "Bring cold patch."),
                manager(3, 1));

        assertTrue(result.created());
        assertEquals(4, result.task().workerId());
        assertEquals("Bring cold patch.", result.task().notes());
        assertEquals(List.of(4), notifications.lastRecipients);
        assertEquals(NotificationType.TASK_ASSIGNED, notifications.lastSubject.getState().type());
    }

    @Test
    public void managerCannotAssignAcrossDepartments() {
        ApiException error = expectApiError(() ->
                service.assignTask(2, new TaskAssignRequest(4, null), manager(5, 2)));

        assertEquals(403, error.status());
        assertEquals("FORBIDDEN", error.code());
    }

    @Test
    public void workerMustBeAssignedBeforeStarting() {
        requests.current = FakeRequestDAO.requestDto(RequestStatus.ASSIGNED, null, 4);

        ApiException error = expectApiError(() -> service.start(2, worker(4, 1)));

        assertEquals(409, error.status());
        assertEquals("INVALID_TRANSITION", error.code());
        assertEquals("Cannot start a request without an assigned worker.", error.getMessage());
    }

    @Test
    public void workerStartsAssignedTaskAndNotifiesCitizen() throws Exception {
        requests.current = FakeRequestDAO.requestDto(RequestStatus.ASSIGNED, null, 4);
        tasks.task = new TaskDto(1, 2, requests.current, 4, "Sam Lee", 3, null, LocalDateTime.now());

        RequestDto response = service.start(2, worker(4, 1));

        assertEquals(RequestStatus.IN_PROGRESS, response.status());
        assertEquals(List.of(7), notifications.lastRecipients);
        assertEquals(NotificationType.REQUEST_STATUS, notifications.lastSubject.getState().type());
    }

    @Test
    public void differentWorkerCannotStartAssignedTask() {
        requests.current = FakeRequestDAO.requestDto(RequestStatus.ASSIGNED, null, 4);
        tasks.task = new TaskDto(1, 2, requests.current, 4, "Sam Lee", 3, null, LocalDateTime.now());

        ApiException error = expectApiError(() -> service.start(2, worker(9, 1)));

        assertEquals(403, error.status());
        assertEquals("FORBIDDEN", error.code());
    }

    @Test
    public void managerResolvesRequestAndTrimsNotes() throws Exception {
        requests.current = FakeRequestDAO.requestDto(RequestStatus.IN_PROGRESS, null, 4);

        RequestDto response = service.resolve(2, new RequestResolveRequest("  Patched and sealed.  "), manager(3, 1));

        assertEquals(RequestStatus.RESOLVED, response.status());
        assertEquals("Patched and sealed.", response.resolutionNotes());
        assertEquals(List.of(7), notifications.lastRecipients);
        assertEquals(NotificationType.REQUEST_STATUS, notifications.lastSubject.getState().type());
    }

    @Test
    public void resolveRejectsNotesOverLimit() {
        ApiException error = expectApiError(() ->
                service.resolve(2, new RequestResolveRequest("x".repeat(1001)), manager(3, 1)));

        assertEquals(400, error.status());
        assertEquals("Must be at most 1000 characters.",
                error.fields().get("resolutionNotes"));
    }

    private static DepartmentManager manager(int id, int departmentId) {
        DepartmentManager manager = new DepartmentManager();
        manager.setUserId(id);
        manager.setDepartmentId(departmentId);
        return manager;
    }

    private static Worker worker(int id, int departmentId) {
        Worker worker = new Worker();
        worker.setUserId(id);
        worker.setDepartmentId(departmentId);
        return worker;
    }

    private static ApiException expectApiError(ThrowingAction action) {
        try {
            action.run();
        } catch (ApiException e) {
            return e;
        } catch (Exception e) {
            throw new AssertionError("Expected API error but got " + e, e);
        }
        fail("Expected API error");
        return null;
    }

    @FunctionalInterface
    private interface ThrowingAction {
        void run() throws Exception;
    }
}
