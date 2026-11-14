package edu.asu.smartcity.server.dto;

/** A worker in a department, for the "assign worker" dropdown. skill may be null. */
public record WorkerDto(int id, String name, String skill) {
}
