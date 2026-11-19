package edu.asu.smartcity.server.domain;

import edu.asu.smartcity.server.model.RequestStatus;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;

public class RequestStateTest {

    @Test
    public void fromStatusRestoresAConcreteStateForEveryStatus() {
        Map<RequestStatus, Class<? extends RequestState>> expectedStates = Map.of(
                RequestStatus.SUBMITTED, SubmittedState.class,
                RequestStatus.UNDER_REVIEW, UnderReviewState.class,
                RequestStatus.ASSIGNED, AssignedState.class,
                RequestStatus.IN_PROGRESS, InProgressState.class,
                RequestStatus.RESOLVED, ResolvedState.class,
                RequestStatus.REJECTED, RejectedState.class);

        expectedStates.forEach((status, stateClass) -> {
            RequestState state = RequestState.fromStatus(status);
            assertEquals(stateClass, state.getClass());
            assertEquals(status, state.getStatus());
        });
    }
}
