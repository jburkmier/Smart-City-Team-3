package edu.asu.smartcity.server.dto;

import edu.asu.smartcity.server.model.RequestType;

/** Body of POST /api/requests. */
public record RequestCreateRequest(RequestType type, String description, String location) {
}
