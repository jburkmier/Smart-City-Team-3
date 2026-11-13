package edu.asu.smartcity.server.domain;

import edu.asu.smartcity.server.model.RequestStatus;

/** Initial state assigned when a citizen submits a service request. */
public class SubmittedState extends RequestState {

    @Override
    public RequestStatus getStatus() {
        return RequestStatus.SUBMITTED;
    }

    @Override
    public RequestState review() {
        return new UnderReviewState();
    }
}
