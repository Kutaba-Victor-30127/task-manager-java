package ro.kutaba.taskmanager.storage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ro.kutaba.taskmanager.model.Task;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JsonTaskRepository implements TaskRepository {

    private final Path path;
    private final ObjectMapper mapper;
    private final List<Task> tasks = new ArrayList<>();
    private int nextId = 1;

    public JsonTaskRepository(Path path) {
        if (path == null) {
            throw new IllegalArgumentException("path nu poate fi null");
        }

        this.path = path;
        this.mapper = new ObjectMapper();

        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        load();
    }

    @Override
    public List<Task> findAll() {
        return new ArrayList<>(tasks);
    }

    @Override
    public Optional<Task> findById(int id) {
        return tasks.stream()
                .filter(task -> task.getId() == id)
                .findFirst();
    }

    @Override
    public Task save(Task task) {
        if (task.getId() == 0) {
            task.setId(nextId++);
            tasks.add(task);
        } else {
            boolean updated = false;

            for (int i = 0; i < tasks.size(); i++) {
                if (tasks.get(i).getId() == task.getId()) {
                    tasks.set(i, task);
                    updated = true;
                    break;
                }
            }

            if (!updated) {
                tasks.add(task);
            }

            if (task.getId() >= nextId) {
                nextId = task.getId() + 1;
            }
        }

        persist();
        return task;
    }

    @Override
    public boolean deleteById(int id) {
        boolean removed = tasks.removeIf(task -> task.getId() == id);

        if (removed) {
            persist();
        }

        return removed;
    }

    private void load(){
        if (Files.notExists(path)){
            return;
        }

        try{
            List<TaskJson> loaded = mapper.readValue(path.toFile(), new TypeReference<List<TaskJson>>(){});

            tasks.clear();

            for (TaskJson tj : loaded){
                Task t = new Task(
                        tj.id(),
                        tj.title(),
                        tj.description(),
                        tj.priority(),
                        tj.deadline(),
                        tj.status(),
                        tj.estimatedMinutes()
                );

                tasks.add(t);

                if (t.getId() >= nextId){
                    nextId = t.getId() + 1;
                }
            }       
        }catch(IOException e){
            throw new RuntimeException("Nu pot incarca task-urile din JSON", e);
        }
    }

    private void persist() {
        try{
        Path parent = path.getParent();

        if(parent != null){
            Files.createDirectories(parent);
        }

        List<TaskJson> toSave = new ArrayList<>();

        for (Task t : tasks){
            toSave.add(new TaskJson(
                    t.getId(),
                    t.getTitle(),
                    t.getDescription(),
                    t.getPriority(),
                    t.getDeadline(),
                    t.getStatus(),
                    t.getEstimatedMinutes()
            ));
        }

        mapper.writeValue(path.toFile(), toSave);
    }catch(IOException e){
        throw new RuntimeException("Nu pot salva task-urile in JSON", e);
        }
    }
}