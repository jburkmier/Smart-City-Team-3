package edu.asu.smartcity.server.dto;

import edu.asu.smartcity.server.model.Role;

/**
 * A user as sent to the client. Never contains the password or its hash.
 * address and phone may be null; departmentId is set only for DEPARTMENT_MANAGER and WORKER.
 */
public record UserDto(int id, String name, String email, Role role, String address, String phone,
                      Integer departmentId) {
}
