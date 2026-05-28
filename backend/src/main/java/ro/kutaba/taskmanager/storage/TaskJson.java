package ro.kutaba.taskmanager.storage;

import ro.kutaba.taskmanager.model.TaskStatus;
import java.time.LocalDate;

public record TaskJson(
    int id,
    String title,
    String description,
    int priority,
    LocalDate deadline,
    TaskStatus status,
    int estimatedMinutes
){}