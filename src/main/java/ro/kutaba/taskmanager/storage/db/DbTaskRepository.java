package ro.kutaba.taskmanager.storage.db;

import org.springframework.stereotype.Repository;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;

import ro.kutaba.taskmanager.model.Task;
import ro.kutaba.taskmanager.storage.TaskRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
@Primary
public class DbTaskRepository implements TaskRepository {

    private final TaskJpaRepository jpa;

    public DbTaskRepository(TaskJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<Task> findAll() {
        List<Task> result = new ArrayList<>();

        for (TaskEntity e : jpa.findAll()) {
            result.add(toModel(e));
        }

        return result;
    }

    @Override
    public Optional<Task> findById(int id) {
        return jpa.findById(id).map(DbTaskRepository::toModel);
    }

    @Override
    @Transactional
    public Task save(Task t) {
        TaskEntity saved = jpa.save(toEntity(t));
        return toModel(saved);
    }

    @Override
    public boolean deleteById(int id) {
        if (!jpa.existsById(id)) return false;
        jpa.deleteById(id);
        return true;
    }

    private static Task toModel(TaskEntity e) {
        Task t = new Task(
                e.getId(),
                e.getTitle(),
                e.getDescription(),
                e.getPriority(),
                e.getDeadline(),
                e.getStatus(),
                e.getEstimatedMinutes()
        );
        return t;
    }

    private static TaskEntity toEntity(Task t) {
        TaskEntity e = new TaskEntity();

        e.setId(t.getId());
        e.setTitle(t.getTitle());
        e.setDescription(t.getDescription());
        e.setPriority(t.getPriority());
        e.setDeadline(t.getDeadline());
        e.setStatus(t.getStatus());
        e.setEstimatedMinutes(t.getEstimatedMinutes());

        return e;
    }
}