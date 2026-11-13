package edu.asu.smartcity.server.domain;

import edu.asu.smartcity.server.model.RequestStatus;

/** Terminal state for a request rejected by an administrator. */
public class RejectedState extends RequestState {

    @Override
    public RequestStatus getStatus() {
        return RequestStatus.REJECTED;
    }
}
