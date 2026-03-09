package ro.kutaba.taskmanager.storage;

import ro.kutaba.taskmanager.model.Task;
import ro.kutaba.taskmanager.model.TaskStatus;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FileTaskRepository implements TaskRepository {

    private final Path filePath;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    public FileTaskRepository(Path filePath){
        this.filePath = filePath;
    }

    // helper intern
    public List<Task> loadAll(){
        List<Task> result = new ArrayList<>();

        if (Files.notExists(filePath)){
            return result;
        }

        try {
            for (String line : Files.readAllLines(filePath)) {
                if (line.isBlank() || line.startsWith("#")) continue;

                String[] parts = line.split(";", -1);
                if (parts.length != 7) continue;

                int id = Integer.parseInt(parts[0]);
                String title = parts[1];
                String desc = parts[2];
                int priority = Integer.parseInt(parts[3]);
                LocalDate deadline = LocalDate.parse(parts[4], DATE_FMT);
                TaskStatus status = TaskStatus.valueOf(parts[5]);
                int estimatedMinutes = Integer.parseInt(parts[6]);

                result.add(new Task(id, title, desc, priority, deadline, status, estimatedMinutes));
            }
        } catch (IOException | RuntimeException e) {
            System.err.println("Nu am reușit să încarc task-urile din " + filePath + ": " + e.getMessage());
        }

        return result;
    }

    // helper intern
    public void saveAll(List<Task> tasks){
        try{
            Files.createDirectories(filePath.getParent());

            List<String> lines = new ArrayList<>();
            lines.add("# id;title;description;priority;deadline;status;estimatedMinutes");

            for(Task t : tasks){
                lines.add(String.join(";",
                        String.valueOf(t.getId()),
                        clean(t.getTitle()),
                        clean(t.getDescription()),
                        String.valueOf(t.getPriority()),
                        t.getDeadline().format(DATE_FMT),
                        t.getStatus().name(),
                        String.valueOf(t.getEstimatedMinutes())
                ));
            }

            Files.write(filePath, lines,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);

        }catch (IOException e){
            throw new RuntimeException("Eroare scriere fisier", e);
        }
    }

    // ===== IMPLEMENTAREA INTERFETEI =====

    @Override
    public List<Task> findAll() {
        return loadAll();
    }

    @Override
    public Optional<Task> findById(int id) {
        return loadAll().stream()
                .filter(t -> t.getId() == id)
                .findFirst();
    }

    @Override
    public Task save(Task t) {
        List<Task> tasks = loadAll();

        boolean updated = false;

        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).getId() == t.getId()) {
                tasks.set(i, t);
                updated = true;
                break;
            }
        }

        if (!updated) {
            tasks.add(t);
        }

        saveAll(tasks);

        return t;
    }

    @Override
    public boolean deleteById(int id) {
        List<Task> tasks = loadAll();
        boolean removed = tasks.removeIf(t -> t.getId() == id);

        if (removed) {
            saveAll(tasks);
        }

        return removed;
    }

    private String clean(String s){
        return s == null ? "" : s.replace(";",",");
    }
}