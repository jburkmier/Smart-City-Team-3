package edu.asu.smartcity.server.service;

import edu.asu.smartcity.server.dao.NotificationDAO;
import edu.asu.smartcity.server.dto.NotificationDto;
import edu.asu.smartcity.server.observer.Subject;
import edu.asu.smartcity.server.observer.UserInbox;
import edu.asu.smartcity.server.web.ApiException;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

/** Sends notifications through the Observer pattern and serves the caller's own notifications. */
public class NotificationService {

    private final NotificationDAO notifications;

    public NotificationService(NotificationDAO notifications) {
        this.notifications = notifications;
    }

    /**
     * Notifies each recipient once about the subject's current state: attaches one UserInbox per
     * recipient, calls notifyObservers(), then detaches them so a later send doesn't repeat them.
     * Features call this after their change is saved, for example:
     * {@code notificationService.send(announcement, citizenIds);}
     */
    public void send(Subject subject, Collection<Integer> recipientIds) throws SQLException {
        List<UserInbox> inboxes = new ArrayList<>();
        // A LinkedHashSet drops duplicate IDs, so nobody gets the same notification twice.
        for (int userId : new LinkedHashSet<>(recipientIds)) {
            UserInbox inbox = new UserInbox(userId, notifications);
            subject.attach(inbox);
            inboxes.add(inbox);
        }
        try {
            subject.notifyObservers();
        } finally {
            for (UserInbox inbox : inboxes) {
                subject.detach(inbox);
            }
        }
    }

    public List<NotificationDto> findMine(int userId, boolean unreadOnly) throws SQLException {
        return notifications.findByUser(userId, unreadOnly);
    }

    /** Marks one of the caller's notifications read; 404 if it doesn't exist or isn't theirs. */
    public void markRead(int userId, int notificationId) throws SQLException {
        // 404 rather than 403 for someone else's notification, so its ID doesn't reveal that it exists.
        if (!notifications.markRead(notificationId, userId)) {
            throw new ApiException(404, "NOT_FOUND", "Notification not found.");
        }
    }

    public void markAllRead(int userId) throws SQLException {
        notifications.markAllRead(userId);
    }
}
