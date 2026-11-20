package edu.asu.smartcity.server.dto;

import edu.asu.smartcity.server.model.FlagContentType;

public record FlagCreateRequest(FlagContentType contentType, Integer contentId, String reason) {
}
