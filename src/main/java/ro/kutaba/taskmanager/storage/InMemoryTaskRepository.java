package ro.kutaba.taskmanager.storage;

import ro.kutaba.taskmanager.model.Task;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InMemoryTaskRepository implements TaskRepository {

    private final List<Task> data = new ArrayList<>();

    @Override
    public List<Task> findAll() {
        return new ArrayList<>(data);
    }

    @Override
    public Optional<Task> findById(int id) {
        return data.stream().filter(t -> t.getId() == id).findFirst();
    }

    @Override
    public Task save(Task t) {
        Optional<Task> existing = findById(t.getId());

        if (existing.isPresent()) {
            int idx = data.indexOf(existing.get());
            data.set(idx, t);
        } else {
            data.add(t);
        }

        return t;
    }

    @Override
    public boolean deleteById(int id) {
        return data.removeIf(t -> t.getId() == id);
    }
}