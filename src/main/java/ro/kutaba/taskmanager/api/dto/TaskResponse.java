package ro.kutaba.taskmanager.api.dto;

import ro.kutaba.taskmanager.model.TaskStatus;
import java.time.LocalDate;

public record TaskResponse(
    int id,
    String title,
    String description,
    int priority,
    LocalDate deadline,
    TaskStatus status,
    int estimatedMinutes
){}