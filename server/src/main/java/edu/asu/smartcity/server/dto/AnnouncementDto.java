package edu.asu.smartcity.server.dto;

import edu.asu.smartcity.server.domain.Announcement;
import edu.asu.smartcity.server.model.AnnouncementType;

import java.time.LocalDateTime;

/** An announcement as sent to the client (docs/api.md, AnnouncementDto). */
public record AnnouncementDto(int id, AnnouncementType type, String title, String message, int createdById,
                              LocalDateTime createdAt) {

    public static AnnouncementDto from(Announcement announcement) {
        return new AnnouncementDto(announcement.getId(), announcement.getType(), announcement.getTitle(),
                announcement.getMessage(), announcement.getCreatedById(), announcement.getCreatedAt());
    }
}
