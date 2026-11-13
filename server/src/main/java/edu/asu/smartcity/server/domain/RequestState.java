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

    public final String getStatusName() {
        return getStatus().name();
    }

    public static RequestState fromStatus(RequestStatus status) {
        if (status == RequestStatus.SUBMITTED) {
            return new SubmittedState();
        }
        if (status == RequestStatus.UNDER_REVIEW) {
            return new UnderReviewState();
        }
        return new StoredState(status);
    }

    private static final class StoredState extends RequestState {
        private final RequestStatus status;

        private StoredState(RequestStatus status) {
            this.status = status;
        }

        @Override
        public RequestStatus getStatus() {
            return status;
        }
    }
}
