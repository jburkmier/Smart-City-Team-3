package edu.asu.smartcity.server.dto;

/** A city department as sent to the client (docs/api.md section 3). */
public record DepartmentDto(int id, String name) {
}
