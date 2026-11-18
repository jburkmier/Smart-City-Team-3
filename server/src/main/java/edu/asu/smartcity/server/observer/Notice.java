package edu.asu.smartcity.server.observer;

import edu.asu.smartcity.server.model.NotificationType;

/**
 * The state a Subject reports through getState(): what changed, as one notification
 * that each Observer saves for its recipient. It is not saved itself; a saved copy
 * is a row in the notifications table (sent to clients as NotificationDto).
 *
 * @param relatedId the request, event, or announcement the notice is about; may be null
 */
public record Notice(NotificationType type, String message, Integer relatedId) {

    /** The longest message the notifications.message column holds. */
    public static final int MAX_MESSAGE_LENGTH = 500;

    public Notice {
        if (type == null) {
            throw new IllegalArgumentException("A notice needs a type.");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("A notice needs a message.");
        }
        if (message.length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException(
                    "A notice message must be at most " + MAX_MESSAGE_LENGTH + " characters.");
        }
    }
}
