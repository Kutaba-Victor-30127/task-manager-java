package ro.kutaba.taskmanager.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ro.kutaba.taskmanager.model.Task;
import ro.kutaba.taskmanager.model.TaskStatus;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JsonTaskRepositoryTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldSaveTaskToJsonFile() {
        Path path = tempDir.resolve("tasks.json");
        JsonTaskRepository repo = new JsonTaskRepository(path);

        repo.save(new Task(
                0,
                "Task JSON",
                "descriere",
                4,
                LocalDate.now().plusDays(3),
                TaskStatus.TODO,
                40
        ));

        assertTrue(Files.exists(path));
    }

    @Test
    void shouldLoadTasksFromJsonFileWhenRepositoryIsRecreated() {
        Path path = tempDir.resolve("tasks.json");

        JsonTaskRepository repo1 = new JsonTaskRepository(path);

        Task saved = repo1.save(new Task(
                0,
                "Persistent task",
                "ramane in fisier",
                5,
                LocalDate.now().plusDays(4),
                TaskStatus.IN_PROGRESS,
                90
        ));

        JsonTaskRepository repo2 = new JsonTaskRepository(path);

        List<Task> tasks = repo2.findAll();

        assertEquals(1, tasks.size());
        assertEquals(saved.getId(), tasks.get(0).getId());
        assertEquals("Persistent task", tasks.get(0).getTitle());
    }

    @Test
    void shouldContinueNextIdAfterLoadingFromJson() {
        Path path = tempDir.resolve("tasks.json");

        JsonTaskRepository repo1 = new JsonTaskRepository(path);

        repo1.save(new Task(
                0,
                "First",
                "",
                2,
                LocalDate.now().plusDays(1),
                TaskStatus.TODO,
                10
        ));

        JsonTaskRepository repo2 = new JsonTaskRepository(path);

        Task second = repo2.save(new Task(
                0,
                "Second",
                "",
                2,
                LocalDate.now().plusDays(2),
                TaskStatus.TODO,
                10
        ));

        assertEquals(2, second.getId());
    }

    @Test
    void shouldDeleteTaskAndPersistDeletion() {
        Path path = tempDir.resolve("tasks.json");

        JsonTaskRepository repo1 = new JsonTaskRepository(path);

        Task task = repo1.save(new Task(
                0,
                "Delete me",
                "",
                2,
                LocalDate.now().plusDays(2),
                TaskStatus.TODO,
                10
        ));

        boolean deleted = repo1.deleteById(task.getId());

        JsonTaskRepository repo2 = new JsonTaskRepository(path);

        assertTrue(deleted);
        assertTrue(repo2.findAll().isEmpty());
    }
}