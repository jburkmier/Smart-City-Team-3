package edu.asu.smartcity.server.service;

import edu.asu.smartcity.server.dao.RequestDAO;
import edu.asu.smartcity.server.dao.TaskDAO;
import edu.asu.smartcity.server.domain.Request;
import edu.asu.smartcity.server.domain.RequestStatusChange;
import edu.asu.smartcity.server.domain.RequestState;
import edu.asu.smartcity.server.domain.RequestTransitionException;
import edu.asu.smartcity.server.domain.StaffMember;
import edu.asu.smartcity.server.domain.SubmittedState;
import edu.asu.smartcity.server.domain.TaskAssignmentNotice;
import edu.asu.smartcity.server.domain.SystemUser;
import edu.asu.smartcity.server.dto.RequestAssignRequest;
import edu.asu.smartcity.server.dto.RequestCreateRequest;
import edu.asu.smartcity.server.dto.RequestDto;
import edu.asu.smartcity.server.dto.RequestRejectRequest;
import edu.asu.smartcity.server.dto.RequestResolveRequest;
import edu.asu.smartcity.server.dto.TaskAssignRequest;
import edu.asu.smartcity.server.dto.TaskAssignmentResult;
import edu.asu.smartcity.server.dto.TaskDto;
import edu.asu.smartcity.server.model.Role;
import edu.asu.smartcity.server.service.NotificationService;
import edu.asu.smartcity.server.model.RequestStatus;
import edu.asu.smartcity.server.model.RequestType;
import edu.asu.smartcity.server.web.ApiException;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Coordinates service-request submission, listing, and workflow actions. */
public class RequestService {

    private final RequestDAO requests;
    private final TaskDAO tasks;
    private final NotificationService notifications;

    public RequestService(RequestDAO requests) {
        this(requests, null, null);
    }

    public RequestService(RequestDAO requests, TaskDAO tasks, NotificationService notifications) {
        this.requests = requests;
        this.tasks = tasks;
        this.notifications = notifications;
    }

    public RequestDto create(RequestCreateRequest request, int citizenId) throws SQLException {
        Map<String, String> errors = new LinkedHashMap<>();
        if (request == null) {
            errors.put("request", "Request body is required.");
        } else {
            if (request.type() == null) {
                errors.put("type", "Type is required.");
            }
            validateText(request.description(), "description", 2000, errors);
            validateText(request.location(), "location", 255, errors);
        }
        if (!errors.isEmpty()) {
            throw new ApiException(400, "VALIDATION_FAILED", "Some fields are invalid.", errors);
        }

        Request newRequest = new Request(citizenId, request.type(), request.description().trim(),
                request.location().trim(), new SubmittedState());
        return requests.create(newRequest);
    }

    public RequestDto review(int requestId) throws SQLException {
        RequestDto current = findRequest(requestId);
        Request request = new Request(current.citizenId(), current.type(), current.description(), current.location(),
                RequestState.fromStatus(current.status()));
        try {
            request.review();
        } catch (RequestTransitionException e) {
            throw invalidTransition(e);
        }

        if (!requests.updateStatus(requestId, current.status(), request.getState().getStatus())) {
            RequestDto latest = findRequest(requestId);
            throw new ApiException(409, "INVALID_TRANSITION",
                    "Cannot review a request that is " + latest.status() + ".");
        }
        return requests.findById(requestId).orElseThrow(
                () -> new SQLException("Reviewed request could not be loaded."));
    }

    public RequestDto assign(int requestId, RequestAssignRequest assignment) throws SQLException {
        if (assignment == null || assignment.departmentId() == null || assignment.departmentId() <= 0) {
            throw new ApiException(400, "VALIDATION_FAILED", "Some fields are invalid.",
                    Map.of("departmentId", "Must be a positive integer."));
        }
        int departmentId = assignment.departmentId();
        if (!requests.departmentExists(departmentId)) {
            throw invalidDepartment();
        }

        RequestDto current = findRequest(requestId);
        Request request = new Request(current.citizenId(), current.type(), current.description(), current.location(),
                RequestState.fromStatus(current.status()));
        try {
            request.assign();
        } catch (RequestTransitionException e) {
            throw invalidTransition(e);
        }

        if (!requests.assign(requestId, current.status(), request.getState().getStatus(), departmentId)) {
            if (!requests.departmentExists(departmentId)) {
                throw invalidDepartment();
            }
            RequestDto latest = findRequest(requestId);
            throw new ApiException(409, "INVALID_TRANSITION",
                    "Cannot assign a request that is " + latest.status() + ".");
        }
        return requests.findById(requestId).orElseThrow(
                () -> new SQLException("Assigned request could not be loaded."));
    }

    public RequestDto reject(int requestId, RequestRejectRequest rejection) throws SQLException {
        Map<String, String> errors = new LinkedHashMap<>();
        if (rejection == null || rejection.reason() == null || rejection.reason().isBlank()) {
            errors.put("reason", "Is required.");
        } else if (rejection.reason().trim().length() > 500) {
            errors.put("reason", "Must be at most 500 characters.");
        }
        if (!errors.isEmpty()) {
            throw new ApiException(400, "VALIDATION_FAILED", "Some fields are invalid.", errors);
        }
        String reason = rejection.reason().trim();

        RequestDto current = findRequest(requestId);
        Request request = new Request(current.citizenId(), current.type(), current.description(), current.location(),
                RequestState.fromStatus(current.status()));
        try {
            request.reject();
        } catch (RequestTransitionException e) {
            throw invalidTransition(e);
        }

        if (!requests.reject(requestId, current.status(), request.getState().getStatus(), reason)) {
            RequestDto latest = findRequest(requestId);
            throw new ApiException(409, "INVALID_TRANSITION",
                    "Cannot reject a request that is " + latest.status() + ".");
        }
        return requests.findById(requestId).orElseThrow(
                () -> new SQLException("Rejected request could not be loaded."));
    }

    public TaskAssignmentResult assignTask(int requestId, TaskAssignRequest assignment, SystemUser caller)
            throws SQLException {
        if (caller.getRole() != Role.DEPARTMENT_MANAGER || !(caller instanceof StaffMember manager)
                || manager.getDepartmentId() == null) {
            throw new ApiException(403, "FORBIDDEN", "Only a department manager can assign a worker.");
        }
        Map<String, String> errors = new LinkedHashMap<>();
        if (assignment == null || assignment.workerId() == null || assignment.workerId() <= 0) {
            errors.put("workerId", "Must be a positive integer.");
        }
        if (assignment != null && assignment.notes() != null && assignment.notes().trim().length() > 1000) {
            errors.put("notes", "Must be at most 1000 characters.");
        }
        if (!errors.isEmpty()) {
            throw new ApiException(400, "VALIDATION_FAILED", "Some fields are invalid.", errors);
        }

        RequestDto current = findRequest(requestId);
        requireManagerDepartment(manager.getDepartmentId(), current);
        requireTaskableStatus(current.status());

        Optional<TaskAssignmentResult> result = taskDAO().assign(requestId, manager.getDepartmentId(),
                assignment.workerId(), caller.getUserId(),
                assignment.notes() == null || assignment.notes().isBlank() ? null : assignment.notes().trim());
        if (result.isEmpty()) {
            RequestDto latest = findRequest(requestId);
            requireManagerDepartment(manager.getDepartmentId(), latest);
            requireTaskableStatus(latest.status());
            throw new ApiException(400, "VALIDATION_FAILED", "Some fields are invalid.",
                    Map.of("workerId", "Worker must belong to your department."));
        }

        TaskAssignmentResult assigned = result.get();
        notificationService().send(
                new TaskAssignmentNotice(requestId, assigned.task().request().type().name()),
                List.of(assigned.task().workerId()));
        return assigned;
    }

    public List<TaskDto> findMyTasks(int workerId) throws SQLException {
        return taskDAO().findByWorker(workerId);
    }

    public RequestDto start(int requestId, SystemUser caller) throws SQLException {
        RequestDto current = findRequest(requestId);
        requireCanActOnRequest(caller, current);
        Request request = toRequest(current);
        try {
            request.start();
        } catch (RequestTransitionException e) {
            throw invalidTransition(e);
        }
        if (taskDAO().findByRequestId(requestId).isEmpty()) {
            throw new ApiException(409, "INVALID_TRANSITION",
                    "Cannot start a request without an assigned worker.");
        }
        Integer workerId = caller.getRole() == Role.WORKER ? caller.getUserId() : null;
        if (!requests.start(requestId, workerId)) {
            throw currentTransitionError(requestId, "start", caller);
        }
        RequestDto updated = findRequest(requestId);
        notifyCitizen(updated);
        return updated;
    }

    public RequestDto resolve(int requestId, RequestResolveRequest resolution, SystemUser caller)
            throws SQLException {
        String notes = resolution == null ? null : resolution.resolutionNotes();
        if (notes != null && notes.trim().length() > 1000) {
            throw new ApiException(400, "VALIDATION_FAILED", "Some fields are invalid.",
                    Map.of("resolutionNotes", "Must be at most 1000 characters."));
        }
        RequestDto current = findRequest(requestId);
        requireCanActOnRequest(caller, current);
        Request request = toRequest(current);
        try {
            request.resolve();
        } catch (RequestTransitionException e) {
            throw invalidTransition(e);
        }
        Integer workerId = caller.getRole() == Role.WORKER ? caller.getUserId() : null;
        if (!requests.resolve(requestId, notes == null || notes.isBlank() ? null : notes.trim(), workerId)) {
            throw currentTransitionError(requestId, "resolve", caller);
        }
        RequestDto updated = findRequest(requestId);
        notifyCitizen(updated);
        return updated;
    }

    public List<RequestDto> findByCitizen(int citizenId) throws SQLException {
        return requests.findByCitizen(citizenId);
    }

    public List<RequestDto> findForAdministrator(RequestStatus status, RequestType type, Integer departmentId)
            throws SQLException {
        return requests.findForAdministrator(status, type, departmentId);
    }

    public List<RequestDto> findForDepartmentManager(int departmentId, RequestStatus status, RequestType type)
            throws SQLException {
        return requests.findForDepartmentManager(departmentId, status, type);
    }

    private static void validateText(String value, String field, int maximumLength, Map<String, String> errors) {
        if (value == null || value.isBlank()) {
            errors.put(field, "Is required.");
        } else if (value.trim().length() > maximumLength) {
            errors.put(field, "Must be at most " + maximumLength + " characters.");
        }
    }

    private RequestDto findRequest(int requestId) throws SQLException {
        Optional<RequestDto> request = requests.findById(requestId);
        return request.orElseThrow(() -> new ApiException(404, "NOT_FOUND", "Request not found."));
    }

    private static Request toRequest(RequestDto request) {
        return new Request(request.citizenId(), request.type(), request.description(), request.location(),
                RequestState.fromStatus(request.status()));
    }

    private static void requireManagerDepartment(int managerDepartmentId, RequestDto request) {
        if (request.departmentId() == null || request.departmentId() != managerDepartmentId) {
            throw new ApiException(403, "FORBIDDEN", "You can only manage requests in your department.");
        }
    }

    private static void requireTaskableStatus(RequestStatus status) {
        if (status != RequestStatus.ASSIGNED && status != RequestStatus.IN_PROGRESS) {
            throw new ApiException(409, "INVALID_TRANSITION",
                    "Cannot assign a task to a request that is " + status + ".");
        }
    }

    private static void requireCanActOnRequest(SystemUser caller, RequestDto request) {
        if (caller.getRole() == Role.WORKER) {
            if (request.assignedWorkerId() == null || request.assignedWorkerId() != caller.getUserId()) {
                throw new ApiException(403, "FORBIDDEN", "Only the assigned worker can update this request.");
            }
            return;
        }
        if (caller.getRole() == Role.DEPARTMENT_MANAGER && caller instanceof StaffMember manager
                && manager.getDepartmentId() != null) {
            requireManagerDepartment(manager.getDepartmentId(), request);
            return;
        }
        throw new ApiException(403, "FORBIDDEN", "You don't have permission to update this request.");
    }

    private ApiException currentTransitionError(int requestId, String action, SystemUser caller) throws SQLException {
        RequestDto latest = findRequest(requestId);
        requireCanActOnRequest(caller, latest);
        String message = "Cannot " + action + " a request that is " + latest.status() + ".";
        if (action.equals("start") && latest.status() == RequestStatus.ASSIGNED
                && taskDAO().findByRequestId(requestId).isEmpty()) {
            message = "Cannot start a request without an assigned worker.";
        }
        return new ApiException(409, "INVALID_TRANSITION", message);
    }

    private void notifyCitizen(RequestDto request) throws SQLException {
        notificationService().send(new RequestStatusChange(request.id(), request.type(), request.status()),
                List.of(request.citizenId()));
    }

    private TaskDAO taskDAO() {
        if (tasks == null) {
            throw new IllegalStateException("TaskDAO is required for task and request execution workflows.");
        }
        return tasks;
    }

    private NotificationService notificationService() {
        if (notifications == null) {
            throw new IllegalStateException("NotificationService is required for request workflow notifications.");
        }
        return notifications;
    }

    private static ApiException invalidTransition(RequestTransitionException e) {
        return new ApiException(409, "INVALID_TRANSITION", e.getMessage());
    }

    private static ApiException invalidDepartment() {
        return new ApiException(400, "VALIDATION_FAILED", "Some fields are invalid.",
                Map.of("departmentId", "Department does not exist."));
    }
}
