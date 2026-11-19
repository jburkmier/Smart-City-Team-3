package edu.asu.smartcity.server.dto;

import edu.asu.smartcity.server.model.AnnouncementType;

/** Body of POST /announcements. */
public record AnnouncementCreateRequest(AnnouncementType type, String title, String message) {
}
