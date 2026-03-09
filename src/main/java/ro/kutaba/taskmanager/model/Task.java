package ro.kutaba.taskmanager.model;

import java.time.LocalDate;
import java.util.Objects;

public class Task {
    private final int id;
    private String title;
    private String description;
    private int priority; // 1-5
    private LocalDate deadline;
    private TaskStatus status;
    private int estimatedMinutes;

    public Task(int id,
                String title,
                String description,
                int priority,
                LocalDate deadline,
                TaskStatus status,
                int estimatedMinutes) {

        if (id <= 0) {
            throw new IllegalArgumentException("id trebuie sa fie pozitiv");
        }

        this.id = id;
        setTitle(title);
        setDescription(description);
        setPriority(priority);
        setDeadline(deadline);
        setStatus(status);
        setEstimatedMinutes(estimatedMinutes);
    }

    public int getId() {
        return id;
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
        if (title == null) throw new IllegalArgumentException("Titlul nu poate fi null");
        String v = title.trim();
        if (v.isEmpty()) throw new IllegalArgumentException("Titlul nu poate fi gol");
        if (v.length() < 3) throw new IllegalArgumentException("Titlu trebuie sa aiba minim 3 caractere");
        if (v.length() > 100) throw new IllegalArgumentException("Titlul trebuie sa aiba maxim 100 de caractere");
 
        this.title = v;
    }

    public void setDescription(String description) {
        String v = (description == null) ? "" : description.trim();
        if (v.length() > 500) throw new IllegalArgumentException("Descrierea trebuie sa aiba maxim 500 de caractere");
        this.description = v; 
    }

    public void setPriority(int priority) {
        if (priority < 1 || priority > 5) {
            throw new IllegalArgumentException("priority trebuie sa fie intre 1 si 5");
        }
        this.priority = priority;
    }

    public void setDeadline(LocalDate deadline) {
        if (deadline == null) {
            throw new IllegalArgumentException("deadline nu poate fi null");
        }
        this.deadline = deadline;
    }

    public void setStatus(TaskStatus status) {
        if (status == null) throw new IllegalArgumentException("Status nu poate fi null");
        this.status = status;
    }

    public void setEstimatedMinutes(int estimatedMinutes) {
        if (estimatedMinutes < 1) throw new IllegalArgumentException("Timpul estimat trebuie sa fie >= 1 minut");
        if (estimatedMinutes > 10_000) throw new IllegalArgumentException("Timp estimat nerealist (max 10000)");
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
        return id == task.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return """
               ID: %d
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
