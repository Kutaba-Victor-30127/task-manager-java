package ro.kutaba.taskmanager.model;

import java.time.LocalDate;
import java.util.Objects;

public class Task {
    private Integer id;
    private String title;
    private String description;
    private int priority;
    private LocalDate deadline;
    private TaskStatus status;
    private int estimatedMinutes;

    public Task(Integer id,
                String title,
                String description,
                int priority,
                LocalDate deadline,
                TaskStatus status,
                int estimatedMinutes
                ) {
        setId(id);
        setTitle(title);
        setDescription(description);
        setPriority(priority);
        setDeadline(deadline);
        setStatus(status);
        setEstimatedMinutes(estimatedMinutes);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        if (id != null && id < 0) {
            throw new IllegalArgumentException("Task id must not be negative");
        }
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getPriority() {
        return priority;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public int getEstimatedMinutes() {
        return estimatedMinutes;
    }

    public void setTitle(String title) {
        if (title == null) throw new IllegalArgumentException("Title must not be null");
        String v = title.trim();
        if (v.isEmpty()) throw new IllegalArgumentException("Title must not be blank");
        if (v.length() < 3) throw new IllegalArgumentException("Title must contain at least 3 characters");
        if (v.length() > 100) throw new IllegalArgumentException("Title must contain at most 100 characters");

        this.title = v;
    }

    public void setDescription(String description) {
        String v = (description == null) ? "" : description.trim();
        if (v.length() > 500) throw new IllegalArgumentException("Description must contain at most 500 characters");
        this.description = v;
    }

    public void setPriority(int priority) {
        if (priority < 1 || priority > 5) {
            throw new IllegalArgumentException("Priority must be between 1 and 5");
        }
        this.priority = priority;
    }

    public void setDeadline(LocalDate deadline) {
        if (deadline == null) {
            throw new IllegalArgumentException("Deadline must not be null");
        }
        this.deadline = deadline;
    }

    public void setStatus(TaskStatus status) {
        if (status == null) throw new IllegalArgumentException("Status must not be null");
        this.status = status;
    }

    public void setEstimatedMinutes(int estimatedMinutes) {
        if (estimatedMinutes < 1) throw new IllegalArgumentException("Estimated time must be at least 1 minute");
        if (estimatedMinutes > 10_000) throw new IllegalArgumentException("Estimated time is too large");
        this.estimatedMinutes = estimatedMinutes;
    }

    public Task copy(){
        return new Task(
            id,
            title,
            description,
            priority,
            deadline,
            status,
            estimatedMinutes
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Task task)) return false;
        if (id == null || task.id == null) return false;
        return id.equals(task.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return """
               ID: %s
               Title: %s
               Description: %s
               Priority: %d/5
               Deadline: %s
               Status: %s
               Estimated time: %d minutes
               ----------------------------------------
               """.formatted(
                id,
                title,
                description,
                priority,
                deadline,
                status,
                estimatedMinutes
        );
    }
}
