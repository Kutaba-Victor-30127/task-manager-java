package ro.kutaba.taskmanager.storage;

import ro.kutaba.taskmanager.model.Task;
import java.util.List;
import java.util.Optional;

public interface TaskRepository {

    List<Task> findAll();

    Optional<Task> findById(int id);

    Task save(Task t);  // create + update

    boolean deleteById(int id);
}
