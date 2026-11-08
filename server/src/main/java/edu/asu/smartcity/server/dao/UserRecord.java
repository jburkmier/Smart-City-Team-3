package edu.asu.smartcity.server.dao;

import edu.asu.smartcity.server.model.Role;

/**
 * One row of the users table, as read by UserDAO.
 * The DAO only moves data; turning a row into a Citizen, Worker, etc. is the Factory's job.
 * departmentId and skill are null for roles that don't use them.
 */
public record UserRecord(int id, String name, String email, String passwordHash, Role role,
                         String address, String phone, Integer departmentId, String skill) {
}
