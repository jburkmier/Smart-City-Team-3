package edu.asu.smartcity.server.domain;

import edu.asu.smartcity.server.model.RequestStatus;

/** A request state supplies its status and handles workflow actions allowed in that state. */
public abstract class RequestState {

    public abstract RequestStatus getStatus();

    public RequestState review() {
        throw new RequestTransitionException(
                "Cannot review a request that is " + getStatusName() + ".");
    }

    public RequestState assign() {
        throw new RequestTransitionException(
                "Cannot assign a request that is " + getStatusName() + ".");
    }

    public RequestState reject() {
        throw new RequestTransitionException(
                "Cannot reject a request that is " + getStatusName() + ".");
    }

    public final String getStatusName() {
        return getStatus().name();
    }

    public static RequestState fromStatus(RequestStatus status) {
        return switch (status) {
            case SUBMITTED -> new SubmittedState();
            case UNDER_REVIEW -> new UnderReviewState();
            case ASSIGNED -> new AssignedState();
            case IN_PROGRESS -> new InProgressState();
            case RESOLVED -> new ResolvedState();
            case REJECTED -> new RejectedState();
        };
    }
}
