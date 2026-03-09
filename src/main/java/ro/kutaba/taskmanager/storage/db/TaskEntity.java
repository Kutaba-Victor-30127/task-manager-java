package ro.kutaba.taskmanager.storage.db;

import jakarta.persistence.*;
import ro.kutaba.taskmanager.model.TaskStatus;

import java.time.LocalDate;

@Entity
@Table(name = "tasks")
public class TaskEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(nullable = false)
    private int priority;

    @Column(nullable = false)
    private LocalDate deadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskStatus status;

    @Column(nullable = false)
    private int estimatedMinutes;

    public TaskEntity(){}

    // --- getters/setters ---
    public Integer getId(){ return id; }
    public void setId(Integer id){ this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }    

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }

    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }

    public int getEstimatedMinutes() { return estimatedMinutes; }
    public void setEstimatedMinutes(int estimatedMinutes) { this.estimatedMinutes = estimatedMinutes; }
    
}