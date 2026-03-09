package ro.kutaba.taskmanager.mapper;

import ro.kutaba.taskmanager.api.dto.TaskResponse;
import ro.kutaba.taskmanager.model.Task;

public class TaskMapper{
    public static TaskResponse toResponse(Task t){
        return new TaskResponse(
            t.getId(),
            t.getTitle(),
            t.getDescription(),
            t.getPriority(),
            t.getDeadline(),
            t.getStatus(),
            t.getEstimatedMinutes()
        );
    }
}

