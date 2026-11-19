package edu.asu.smartcity.server.observer;

import edu.asu.smartcity.server.dao.NotificationDAO;

import java.sql.SQLException;

/**
 * Observer pattern: the concrete Observer. One inbox stands for one recipient; when its
 * subject changes, it pulls the subject's state and saves it as a notifications row for
 * that user, who sees it through GET /me/notifications.
 *
 * Differences from the Phase II design:
 * - The concrete observer is UserInbox, not Citizen and Worker. Users are plain data objects
 *   with no database access, and an inbox only needs the recipient's ID, so the server
 *   doesn't load every recipient as a full user just to notify them.
 * - update() saves a row instead of pushing to a live screen, so recipients who are offline
 *   still get the notification the next time they open the app.
 */
public class UserInbox implements Observer {

    private final int userId;
    private final NotificationDAO notifications;

    public UserInbox(int userId, NotificationDAO notifications) {
        this.userId = userId;
        this.notifications = notifications;
    }

    public int getUserId() {
        return userId;
    }

    @Override
    public void update(Subject subject) throws SQLException {
        notifications.insert(userId, subject.getState());
    }
}
