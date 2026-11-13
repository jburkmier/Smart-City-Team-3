package edu.asu.smartcity.server.domain;

import edu.asu.smartcity.server.model.RequestType;

/** A citizen's service request; its status is represented by its current state. */
public class Request {

    private final int citizenId;
    private final RequestType type;
    private final String description;
    private final String location;
    private RequestState state;

    public Request(int citizenId, RequestType type, String description, String location, RequestState state) {
        this.citizenId = citizenId;
        this.type = type;
        this.description = description;
        this.location = location;
        this.state = state;
    }

    public int getCitizenId() {
        return citizenId;
    }

    public RequestType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public String getLocation() {
        return location;
    }

    public RequestState getState() {
        return state;
    }

    public void changeState(RequestState state) {
        this.state = state;
    }

    public String getStatusName() {
        return state.getStatusName();
    }
}
