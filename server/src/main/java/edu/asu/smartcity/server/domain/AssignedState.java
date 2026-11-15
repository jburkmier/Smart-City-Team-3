package edu.asu.smartcity.server.domain;

import edu.asu.smartcity.server.model.RequestStatus;

/** State entered after an administrator assigns a request to a department. */
public class AssignedState extends RequestState {

    @Override
    public RequestStatus getStatus() {
        return RequestStatus.ASSIGNED;
    }
}
