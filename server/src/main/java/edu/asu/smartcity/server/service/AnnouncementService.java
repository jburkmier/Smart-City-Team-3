package edu.asu.smartcity.server.service;

import edu.asu.smartcity.server.dao.AnnouncementDAO;
import edu.asu.smartcity.server.dao.UserDAO;
import edu.asu.smartcity.server.domain.Announcement;
import edu.asu.smartcity.server.dto.AnnouncementCreateRequest;
import edu.asu.smartcity.server.dto.AnnouncementDto;
import edu.asu.smartcity.server.model.Role;
import edu.asu.smartcity.server.web.ApiException;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Posts, lists, and deletes announcements; posting notifies every citizen through the Observer pattern. */
public class AnnouncementService {

    static final int MAX_TITLE_LENGTH = 150;
    static final int MAX_MESSAGE_LENGTH = 2000;

    private final AnnouncementDAO announcements;
    private final UserDAO users;
    private final NotificationService notifications;

    public AnnouncementService(AnnouncementDAO announcements, UserDAO users, NotificationService notifications) {
        this.announcements = announcements;
        this.users = users;
        this.notifications = notifications;
    }

    /**
     * Saves the announcement, then notifies every citizen. If notifying fails (database outage),
     * the error goes to the caller, but the announcement stays posted and citizens notified before
     * the failure keep their notification.
     */
    public AnnouncementDto create(AnnouncementCreateRequest request, int administratorId) throws SQLException {
        Map<String, String> errors = new LinkedHashMap<>();
        if (request == null) {
            errors.put("request", "Request body is required.");
        } else {
            if (request.type() == null) {
                errors.put("type", "Is required.");
            }
            validateText(request.title(), "title", MAX_TITLE_LENGTH, errors);
            validateText(request.message(), "message", MAX_MESSAGE_LENGTH, errors);
        }
        if (!errors.isEmpty()) {
            throw new ApiException(400, "VALIDATION_FAILED", "Some fields are invalid.", errors);
        }

        Announcement announcement = announcements.create(request.type(), request.title().trim(),
                request.message().trim(), administratorId);
        notifications.send(announcement, users.findIdsByRole(Role.CITIZEN));
        return AnnouncementDto.from(announcement);
    }

    public List<AnnouncementDto> findActive() throws SQLException {
        return announcements.findActive().stream().map(AnnouncementDto::from).toList();
    }

    /** Deletes an announcement; 404 if it doesn't exist or was already deleted. */
    public void delete(int announcementId) throws SQLException {
        if (!announcements.delete(announcementId)) {
            throw new ApiException(404, "NOT_FOUND", "Announcement not found.");
        }
    }

    private static void validateText(String value, String field, int maximumLength, Map<String, String> errors) {
        if (value == null || value.isBlank()) {
            errors.put(field, "Is required.");
        } else if (value.trim().length() > maximumLength) {
            errors.put(field, "Must be at most " + maximumLength + " characters.");
        }
    }
}
