package ro.kutaba.taskmanager.storage;

import ro.kutaba.taskmanager.model.Task;
import ro.kutaba.taskmanager.model.TaskStatus;
import ro.kutaba.taskmanager.model.User;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TaskRepository {

    Page<Task> findAll(Pageable pageable);

    Optional<Task> findById(Integer id);

    Task save(Task t, User user);  // create + update

    boolean deleteById(Integer id);   

    List<Task> findByUsername(String username);

    Optional<Task> findByIdAndUsername(Integer id, String username);

    boolean deleteByIdAndUsername(Integer id, String username);

    Page<Task> findByUser(User user, Pageable pageable);

    Page<Task> findByUserAndStatus(User user, TaskStatus status, Pageable pageable);

    Page<Task> findByUserAndTitleContaining(User user, String text, Pageable pageable);

    Page<Task> findByUserAndTitleContainingAndStatus(User user, String text, TaskStatus status, Pageable pageable);

    Page<Task> findByTitleContaining(String text, Pageable pageable);

    Page<Task> findByStatus(TaskStatus status, Pageable pageable);

    Page<Task> findByTitleContainingAndStatus(String text, TaskStatus status, Pageable pageable);
}
