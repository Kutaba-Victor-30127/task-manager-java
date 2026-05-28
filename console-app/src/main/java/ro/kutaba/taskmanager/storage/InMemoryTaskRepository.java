package ro.kutaba.taskmanager.storage;

import ro.kutaba.taskmanager.model.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InMemoryTaskRepository implements TaskRepository {

    private final List<Task> tasks = new ArrayList<>();
    private int nextId = 1;

    @Override
    public List<Task> findAll() {
        return new ArrayList<>(tasks);
    }

    @Override
    public Optional<Task> findById(int id) {
        return tasks.stream()
                .filter(t -> t.getId() == id)
                .findFirst();
    }

    @Override
    public Task save(Task task) {
        if (task.getId() == 0) {
            task.setId(nextId++);
            tasks.add(task);
            return task;
        }

        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).getId() == task.getId()) {
                tasks.set(i, task);
                return task;
            }
        }

        tasks.add(task);
        if (task.getId() >= nextId) {
            nextId = task.getId() + 1;
        }
        return task;
    }

    @Override
    public boolean deleteById(int id) {
        return tasks.removeIf(t -> t.getId() == id);
    }
}