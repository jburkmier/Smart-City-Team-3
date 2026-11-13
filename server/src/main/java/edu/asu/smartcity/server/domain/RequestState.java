package edu.asu.smartcity.server.domain;

import edu.asu.smartcity.server.model.RequestStatus;

/** A request's state supplies its status; state-specific actions are added with workflow features. */
public abstract class RequestState {

    public abstract RequestStatus getStatus();

    public final String getStatusName() {
        return getStatus().name();
    }
}
