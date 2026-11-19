package edu.asu.smartcity.server.observer;

import java.sql.SQLException;

/**
 * Observer pattern: the Observer. A Subject calls update() on every attached observer
 * when its state changes.
 *
 * Uses the GoF "pull" model from the Phase II design: update() only says that the subject
 * changed, and the observer calls subject.getState() to get the details it needs.
 *
 * update() declares SQLException because the concrete observer (UserInbox) saves a
 * notifications row, so a database outage reaches ServerApp's usual 503/500 handling.
 */
public interface Observer {

    void update(Subject subject) throws SQLException;
}
