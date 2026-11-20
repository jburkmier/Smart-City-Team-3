package edu.asu.smartcity.server.domain;

import edu.asu.smartcity.server.model.NotificationType;
import edu.asu.smartcity.server.observer.Notice;
import edu.asu.smartcity.server.observer.Subject;

/** Observer subject notifying a worker that a request has been assigned to them. */
public class TaskAssignmentNotice extends Subject {

    private final int requestId;
    private final String requestType;

    public TaskAssignmentNotice(int requestId, String requestType) {
        this.requestId = requestId;
        this.requestType = requestType;
    }

    @Override
    public Notice getState() {
        return new Notice(NotificationType.TASK_ASSIGNED,
                "You have been assigned to request #" + requestId + " (" + requestType + ").", requestId);
    }
}
