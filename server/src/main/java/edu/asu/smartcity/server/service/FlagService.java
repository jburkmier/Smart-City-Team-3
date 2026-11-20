package edu.asu.smartcity.server.service;

import edu.asu.smartcity.server.dao.FlagDAO;
import edu.asu.smartcity.server.dao.UserDAO;
import edu.asu.smartcity.server.domain.SystemUser;
import edu.asu.smartcity.server.dto.FlagCreateRequest;
import edu.asu.smartcity.server.dto.FlagDto;
import edu.asu.smartcity.server.model.FlagContentType;
import edu.asu.smartcity.server.model.FlagStatus;
import edu.asu.smartcity.server.model.Role;
import edu.asu.smartcity.server.observer.FlagSubmission;
import edu.asu.smartcity.server.web.ApiException;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Coordinates flag validation, administrator notification, and moderation. */
public class FlagService {

    private final FlagDAO flags;
    private final UserDAO users;
    private final NotificationService notifications;

    public FlagService(FlagDAO flags, UserDAO users, NotificationService notifications) {
        this.flags = flags;
        this.users = users;
        this.notifications = notifications;
    }

    public FlagDto create(FlagCreateRequest request, SystemUser reporter) throws SQLException {
        Map<String, String> errors = new LinkedHashMap<>();
        if (request == null) {
            errors.put("request", "Request body is required.");
        } else {
            if (request.contentType() == null) {
                errors.put("contentType", "Is required.");
            }
            if (request.contentId() == null || request.contentId() <= 0) {
                errors.put("contentId", "Must be a positive integer.");
            }
            if (request.reason() == null || request.reason().isBlank()) {
                errors.put("reason", "Is required.");
            } else if (request.reason().trim().length() > 500) {
                errors.put("reason", "Must be at most 500 characters.");
            }
        }
        if (!errors.isEmpty()) {
            throw new ApiException(400, "VALIDATION_FAILED", "Some fields are invalid.", errors);
        }

        if (request.contentType() == FlagContentType.REQUEST
                && reporter.getRole() != Role.CITY_ADMINISTRATOR
                && reporter.getRole() != Role.DEPARTMENT_MANAGER
                && reporter.getRole() != Role.WORKER) {
            throw new ApiException(403, "FORBIDDEN", "You don't have permission to flag a request.");
        }
        if (!flags.contentIsVisible(request.contentType(), request.contentId())) {
            throw new ApiException(404, "NOT_FOUND", "Content not found.");
        }

        FlagCreateRequest normalized = new FlagCreateRequest(
                request.contentType(), request.contentId(), request.reason().trim());
        FlagDto flag = flags.create(normalized, reporter.getUserId());
        notifications.send(new FlagSubmission(flag.id(), flag.contentSummary()),
                users.findIdsByRole(Role.CITY_ADMINISTRATOR));
        return flag;
    }

    public List<FlagDto> findAll(FlagStatus status) throws SQLException {
        return flags.findAll(status);
    }

    public FlagDto dismiss(int flagId) throws SQLException {
        return moderate(flagId, FlagStatus.DISMISSED);
    }

    public FlagDto remove(int flagId) throws SQLException {
        return moderate(flagId, FlagStatus.REMOVED);
    }

    private FlagDto moderate(int flagId, FlagStatus status) throws SQLException {
        if (!flags.moderate(flagId, status)) {
            Optional<FlagDto> existing = flags.findById(flagId);
            if (existing.isEmpty()) {
                throw new ApiException(404, "NOT_FOUND", "Flag not found.");
            }
            throw new ApiException(409, "INVALID_TRANSITION", "Only open flags can be moderated.");
        }
        return flags.findById(flagId).orElseThrow(
                () -> new SQLException("Moderated flag could not be loaded."));
    }
}
