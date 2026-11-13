package edu.asu.smartcity.server.service;

import edu.asu.smartcity.server.dao.RequestDAO;
import edu.asu.smartcity.server.domain.Request;
import edu.asu.smartcity.server.domain.RequestState;
import edu.asu.smartcity.server.domain.RequestTransitionException;
import edu.asu.smartcity.server.domain.SubmittedState;
import edu.asu.smartcity.server.dto.RequestCreateRequest;
import edu.asu.smartcity.server.dto.RequestDto;
import edu.asu.smartcity.server.model.RequestStatus;
import edu.asu.smartcity.server.model.RequestType;
import edu.asu.smartcity.server.web.ApiException;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Coordinates service-request submission and listing. */
public class RequestService {

    private final RequestDAO requests;

    public RequestService(RequestDAO requests) {
        this.requests = requests;
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

    private static ApiException invalidTransition(RequestTransitionException e) {
        return new ApiException(409, "INVALID_TRANSITION", e.getMessage());
    }
}
