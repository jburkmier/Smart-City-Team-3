package edu.asu.smartcity.server.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** An event as sent to the client (docs/api.md section 4). */
public record EventDto(int id, String title, String description, String category, LocalDateTime startTime,
                       String district, String locationDetail, BigDecimal price, int registrationCount,
                       boolean registeredByMe, int createdById) {
}
