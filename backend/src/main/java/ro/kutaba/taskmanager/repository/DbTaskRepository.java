package ro.kutaba.taskmanager.repository;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ro.kutaba.taskmanager.model.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import ro.kutaba.taskmanager.model.Task;
import ro.kutaba.taskmanager.model.TaskEntity;
import ro.kutaba.taskmanager.model.User;
import ro.kutaba.taskmanager.storage.TaskRepository;

import java.util.List;
import java.util.Optional;

@Repository
@Primary
public class DbTaskRepository implements TaskRepository{
    private final TaskJpaRepository jpa;

    public DbTaskRepository (TaskJpaRepository jpa){
        this.jpa = jpa;
    }

    @Override
    public Page<Task> findAll(Pageable pageable){
        return jpa.findAll(pageable)
                  .map(DbTaskRepository::toModel);
    }

    @Override
    public Optional<Task> findById(Integer id){
        if (id == null) {
            return Optional.empty();
        }
        return jpa.findById(id.longValue()).map(DbTaskRepository::toModel);
    }

    @Override
    @Transactional
    public Task save(Task t, User user){
        TaskEntity e;

        if (t.getId() != null) {
            //update
            e = jpa.findById(t.getId().longValue())
                    .orElseThrow(() -> new RuntimeException("Task not found"));
        } else {
            //create
            e = new TaskEntity();
        }

        e.setUser(user);
        e.setTitle(t.getTitle());
        e.setDescription(t.getDescription());
        e.setPriority(t.getPriority());
        e.setDeadline(t.getDeadline());
        e.setStatus(t.getStatus());
        e.setEstimatedMinutes(t.getEstimatedMinutes());

        return toModel(jpa.save(e));
    }

    @Override
    public boolean deleteById(Integer id){
        if (id == null || !jpa.existsById(id.longValue())) return false;
        jpa.deleteById(id.longValue());
        return true;
    }

    @Override
    public Page<Task> findByUser(User user, Pageable pageable){
        return jpa.findByUser(user, pageable)
                  .map(DbTaskRepository::toModel);
    }

    @Override
    public Page<Task> findByUserAndStatus(User user, TaskStatus status, Pageable pageable){
        return jpa.findByUserAndStatus(user, status, pageable)
                  .map(DbTaskRepository::toModel);
    }

    @Override
    public List<Task> findByUsername(String username){
        return jpa.findByUserUsername(username).stream()
                    .map(DbTaskRepository::toModel)
                    .toList();
    }

    @Override
    public Optional<Task> findByIdAndUsername(Integer id, String username){
        if (id == null) {
            return Optional.empty();
        }
        return jpa.findByIdAndUserUsername(id.longValue(), username)
                .map(DbTaskRepository::toModel);
    } 

    @Override
    public boolean deleteByIdAndUsername(Integer id, String username){
        if (id == null) {
            return false;
        }
        Optional<TaskEntity> e = jpa.findByIdAndUserUsername(id.longValue(), username);
        if (e.isEmpty()) return false;

        jpa.delete(e.get());
        return true;
    }    

    @Override
    public Page<Task> findByUserAndTitleContaining(User user, String text, Pageable pageable){
        return jpa.findByUserAndTitleContaining(user, text, pageable)
                  .map(DbTaskRepository::toModel);
    }

    @Override
    public Page<Task> findByUserAndTitleContainingAndStatus(User user, String text, TaskStatus status, Pageable pageable){
        return jpa.findByUserAndTitleContainingAndStatus(user, text, status, pageable)
                  .map(DbTaskRepository::toModel);
    }
    
    @Override
    public Page<Task> findByTitleContaining(String text, Pageable pageable){
        return jpa.findByTitleContaining(text, pageable)
                .map(DbTaskRepository::toModel);
    }

    @Override
    public Page<Task> findByStatus(TaskStatus status, Pageable pageable){
        return jpa.findByStatus(status, pageable)
                .map(DbTaskRepository::toModel);
    }

    @Override
    public Page<Task> findByTitleContainingAndStatus(String text, TaskStatus status, Pageable pageable){
        return jpa.findByTitleContainingAndStatus(text, status, pageable)
                .map(DbTaskRepository::toModel);
    }

    // Entity -> Model
    public static Task toModel(TaskEntity e){
        return new Task(
            e.getId() == null ? null : e.getId().intValue(),
            e.getTitle(),
            e.getDescription(),
            e.getPriority(),
            e.getDeadline(),
            e.getStatus(),
            e.getEstimatedMinutes()
        );  
    }

    // Model -> Entity
    public static TaskEntity toEntity(Task t){
        TaskEntity e = new TaskEntity();

        e.setTitle(t.getTitle());
        e.setDescription(t.getDescription());
        e.setPriority(t.getPriority());
        e.setDeadline(t.getDeadline());
        e.setStatus(t.getStatus());
        e.setEstimatedMinutes(t.getEstimatedMinutes());

        return e;
    } 
}
