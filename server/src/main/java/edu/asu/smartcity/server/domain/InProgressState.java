package edu.asu.smartcity.server.domain;

import edu.asu.smartcity.server.model.RequestStatus;

/** State entered when work on an assigned request begins. */
public class InProgressState extends RequestState {

    @Override
    public RequestStatus getStatus() {
        return RequestStatus.IN_PROGRESS;
    }
}
