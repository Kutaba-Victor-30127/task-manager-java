package ro.kutaba.taskmanager.api.dto;

import jakarta.validation.constraints.*;
import ro.kutaba.taskmanager.model.TaskStatus;

import java.time.LocalDate;

public record UpdateTaskRequest(
        @NotBlank @Size(min = 3, max = 100) String title,
        @Size(max = 500) String description,
        @Min(1) @Max(5) int priority,
        @NotNull LocalDate deadline,
        @NotNull TaskStatus status,
        @Min(1) @Max(10000) int estimatedMinutes
) {}