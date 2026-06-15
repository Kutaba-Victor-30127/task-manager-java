package ro.kutaba.taskmanager.api.dto;

import jakarta.validation.constraints.*;
import ro.kutaba.taskmanager.model.TaskStatus;

import java.time.LocalDate;

public record CreateTaskRequest(
        @NotBlank(message = "Title must not be blank")
        @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
        String title,
        @NotBlank(message = "Description must not be blank")
        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description,
        @Min(value = 1, message = "Priority must be at least 1")
        @Max(value = 5, message = "Priority must be at most 5")
        int priority,
        @NotNull(message = "Deadline is required")
        LocalDate deadline,
        @NotNull(message = "Status is required")
        TaskStatus status,
        @Min(value = 1, message = "Estimated minutes must be at least 1")
        @Max(value = 10000, message = "Estimated minutes must be at most 10000")
        int estimatedMinutes
) {}
