package edu.asu.smartcity.server.domain;

import edu.asu.smartcity.server.model.RequestStatus;

/** State entered after an administrator reviews a submitted request. */
public class UnderReviewState extends RequestState {

    @Override
    public RequestStatus getStatus() {
        return RequestStatus.UNDER_REVIEW;
    }
}
