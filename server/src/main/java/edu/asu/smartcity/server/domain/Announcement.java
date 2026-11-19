package edu.asu.smartcity.server.domain;

import edu.asu.smartcity.server.model.AnnouncementType;
import edu.asu.smartcity.server.model.NotificationType;
import edu.asu.smartcity.server.observer.Notice;
import edu.asu.smartcity.server.observer.Subject;

import java.time.LocalDateTime;

/**
 * A city announcement (Observer pattern: a concrete Subject). When an administrator posts one,
 * every citizen is attached as an observer and notified; each pulls the notice from getState().
 *
 * Phase II's separate EmergencyAlert class is this class with type EMERGENCY.
 */
public class Announcement extends Subject {

    private final int id;
    private final AnnouncementType type;
    private final String title;
    private final String message;
    private final int createdById;
    private final LocalDateTime createdAt;

    public Announcement(int id, AnnouncementType type, String title, String message, int createdById,
                        LocalDateTime createdAt) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.message = message;
        this.createdById = createdById;
        this.createdAt = createdAt;
    }

    /**
     * The notice each citizen receives: the type and title, linked to this announcement.
     * The full message stays in the announcements list, because it can be longer than a notification.
     */
    @Override
    public Notice getState() {
        if (type == AnnouncementType.EMERGENCY) {
            return new Notice(NotificationType.EMERGENCY_ALERT, "Emergency alert: " + title, id);
        }
        return new Notice(NotificationType.ANNOUNCEMENT, "New announcement: " + title, id);
    }

    public int getId() {
        return id;
    }

    public AnnouncementType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public int getCreatedById() {
        return createdById;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
