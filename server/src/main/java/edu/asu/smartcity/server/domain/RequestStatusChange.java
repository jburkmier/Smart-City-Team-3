package edu.asu.smartcity.server.domain;

import edu.asu.smartcity.server.model.NotificationType;
import edu.asu.smartcity.server.model.RequestStatus;
import edu.asu.smartcity.server.model.RequestType;
import edu.asu.smartcity.server.observer.Notice;
import edu.asu.smartcity.server.observer.Subject;

/** Observer subject for a citizen-facing request status notification. */
public class RequestStatusChange extends Subject {

    private final int requestId;
    private final RequestType type;
    private final RequestStatus status;

    public RequestStatusChange(int requestId, RequestType type, RequestStatus status) {
        this.requestId = requestId;
        this.type = type;
        this.status = status;
    }

    @Override
    public Notice getState() {
        return new Notice(NotificationType.REQUEST_STATUS,
                "Your request #" + requestId + " (" + type + ") is now " + status + ".", requestId);
    }
}
