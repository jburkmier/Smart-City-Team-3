package edu.asu.smartcity.server.domain;

import edu.asu.smartcity.server.model.NotificationType;
import edu.asu.smartcity.server.observer.Notice;
import edu.asu.smartcity.server.observer.Subject;

/**
 * Observer pattern: a concrete Subject. When an administrator cancels an event, every citizen
 * registered for it is attached as an observer and notified; each pulls the notice from getState().
 */
public class EventCancellation extends Subject {

    private final int eventId;
    private final String title;

    public EventCancellation(int eventId, String title) {
        this.eventId = eventId;
        this.title = title;
    }

    @Override
    public Notice getState() {
        return new Notice(NotificationType.EVENT_CANCELLED, "Event cancelled: " + title, eventId);
    }
}
