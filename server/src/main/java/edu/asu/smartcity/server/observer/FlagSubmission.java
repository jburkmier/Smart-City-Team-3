package edu.asu.smartcity.server.observer;

import edu.asu.smartcity.server.model.NotificationType;

/** A notice sent to city administrators when new content is flagged. */
public class FlagSubmission extends Subject {

    private final int flagId;
    private final String summary;

    public FlagSubmission(int flagId, String summary) {
        this.flagId = flagId;
        this.summary = summary;
    }

    @Override
    public Notice getState() {
        return new Notice(NotificationType.FLAG_SUBMITTED,
                "New content flag #" + flagId + ": " + summary, flagId);
    }
}
