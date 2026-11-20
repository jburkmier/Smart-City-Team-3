package edu.asu.smartcity.server.dto;

/** Count of open requests assigned to a department worker. */
public record WorkerTaskLoadDto(int workerId, String workerName, int openTasks) {
}
