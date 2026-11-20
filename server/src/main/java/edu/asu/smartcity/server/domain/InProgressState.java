package edu.asu.smartcity.server.domain;

import edu.asu.smartcity.server.model.RequestStatus;

/** State entered when the assigned worker begins work on a request. */
public class InProgressState extends RequestState {

    @Override
    public RequestStatus getStatus() {
        return RequestStatus.IN_PROGRESS;
    }

    @Override
    public RequestState resolve() {
        return new ResolvedState();
    }
}
