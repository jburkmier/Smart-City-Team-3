package edu.asu.smartcity.server.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Request body for POST /api/events. */
public record EventCreateRequest(String title, String description, String category, LocalDateTime startTime,
                                String district, String locationDetail, BigDecimal price) {
}
