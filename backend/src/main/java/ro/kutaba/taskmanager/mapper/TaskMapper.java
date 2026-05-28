package ro.kutaba.taskmanager.mapper;

import ro.kutaba.taskmanager.api.dto.CreateTaskRequest;
import ro.kutaba.taskmanager.api.dto.TaskResponse;
import ro.kutaba.taskmanager.model.Task;

public class TaskMapper{

    public static TaskResponse toResponse(Task t){
        if (t == null) return null;

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

    public static Task toEntity(CreateTaskRequest r){
        if (r == null) return null;

        return new Task(
            null,
            r.title(),
            r.description(),
            r.priority(),
            r.deadline(),
            r.status(),
            r.estimatedMinutes()
        );
    }
    
}

