package de.nidal.taskboard.task;

public record TaskResponse(Long id, String title, TaskStatus status) {

    public static TaskResponse from(Task task) {
        return new TaskResponse(task.getId(), task.getTitle(), task.getStatus());
    }
}