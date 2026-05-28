package ro.kutaba.taskmanager.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ro.kutaba.taskmanager.model.Task;
import ro.kutaba.taskmanager.model.TaskStatus;
import ro.kutaba.taskmanager.storage.InMemoryTaskRepository;
import ro.kutaba.taskmanager.storage.AuditLogger;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.contains;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TaskServiceTest {

    private TaskService service;

    @BeforeEach
    void setUp() {
        service = new TaskService(
                new InMemoryTaskRepository(),
                message -> {} // fake AuditLogger
        );
    }

    @Test
    void shouldCreateTask() {
        Task task = service.createTask(
                "Sala",
                "Picioare",
                4,
                LocalDate.now().plusDays(3),
                TaskStatus.TODO,
                60
        );

        assertEquals(1, task.getId());
        assertEquals("Sala", task.getTitle());
        assertEquals(1, service.getAllTasks().size());
    }

    @Test
    void shouldLogWhenTaskIsCreated() {
        AuditLogger audit = mock(AuditLogger.class);

        TaskService service = new TaskService(
                new InMemoryTaskRepository(),
                audit
        );

        service.createTask(
                "Task test",
                "",
                3,
                LocalDate.now().plusDays(1),
                TaskStatus.TODO,
                30
        );

        verify(audit).log(contains("CREATE"));
    }

    @Test
    void shouldFindTaskById() {
        Task task = createSampleTask("Task unu", TaskStatus.TODO);

        Task found = service.findById(task.getId());

        assertNotNull(found);
        assertEquals(task.getId(), found.getId());
    }

    @Test
    void shouldReturnNullWhenTaskNotFound() {
        Task found = service.findById(999);

        assertNull(found);
    }

    @Test
    void shouldDeleteTask() {
        Task task = createSampleTask("Task delete", TaskStatus.TODO);

        boolean deleted = service.deleteById(task.getId());

        assertTrue(deleted);
        assertTrue(service.getAllTasks().isEmpty());
    }

    @Test
    void shouldReturnFalseWhenDeletingUnknownTask() {
        boolean deleted = service.deleteById(999);

        assertFalse(deleted);
    }

    @Test
    void shouldUpdateStatusWithValidTransition() {
        Task task = createSampleTask("Task status", TaskStatus.TODO);

        boolean updated = service.updateStatus(task.getId(), TaskStatus.IN_PROGRESS);

        assertTrue(updated);
        assertEquals(TaskStatus.IN_PROGRESS, service.findById(task.getId()).getStatus());
    }

    @Test
    void shouldThrowWhenInvalidStatusTransitionFromDone() {
        Task task = createSampleTask("Task done", TaskStatus.TODO);

        service.updateStatus(task.getId(), TaskStatus.IN_PROGRESS);
        service.updateStatus(task.getId(), TaskStatus.DONE);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.updateStatus(task.getId(), TaskStatus.TODO)
        );
    }

    @Test
    void shouldSearchByTitleOrDescription() {
        createSampleTask("Java proiect", TaskStatus.TODO);
        service.createTask(
                "Sala",
                "Antrenament picioare",
                3,
                LocalDate.now().plusDays(4),
                TaskStatus.TODO,
                45
        );

        List<Task> result = service.searchByTitleOrDescription("java");

        assertEquals(1, result.size());
        assertEquals("Java proiect", result.get(0).getTitle());
    }

    @Test
    void shouldFilterByStatus() {
        createSampleTask("Task todo", TaskStatus.TODO);
        createSampleTask("Task blocked", TaskStatus.BLOCKED);

        List<Task> result = service.filterByStatus(TaskStatus.BLOCKED);

        assertEquals(1, result.size());
        assertEquals(TaskStatus.BLOCKED, result.get(0).getStatus());
    }

    @Test
    void shouldSortByPriorityDescThenDeadlineAsc() {
        service.createTask("Low", "", 1, LocalDate.now().plusDays(1), TaskStatus.TODO, 10);
        service.createTask("High later", "", 5, LocalDate.now().plusDays(10), TaskStatus.TODO, 10);
        service.createTask("High sooner", "", 5, LocalDate.now().plusDays(2), TaskStatus.TODO, 10);

        List<Task> result = service.sortByPriorityDescThenDeadlineAsc();

        assertEquals("High sooner", result.get(0).getTitle());
        assertEquals("High later", result.get(1).getTitle());
        assertEquals("Low", result.get(2).getTitle());
    }

    @Test
    void shouldUndoCreate() {
        createSampleTask("Undo task", TaskStatus.TODO);

        boolean undone = service.undo();

        assertTrue(undone);
        assertTrue(service.getAllTasks().isEmpty());
    }

    @Test
    void shouldRedoCreateAfterUndo() {
        createSampleTask("Redo task", TaskStatus.TODO);

        service.undo();
        boolean redone = service.redo();

        assertTrue(redone);
        assertEquals(1, service.getAllTasks().size());
        assertEquals("Redo task", service.getAllTasks().get(0).getTitle());
    }

    @Test
    void shouldUndoDelete() {
        Task task = createSampleTask("Deleted task", TaskStatus.TODO);

        service.deleteById(task.getId());
        service.undo();

        assertEquals(1, service.getAllTasks().size());
        assertEquals("Deleted task", service.getAllTasks().get(0).getTitle());
    }

    @Test
    void shouldClearRedoWhenNewActionIsRecorded() {
        createSampleTask("Task 1", TaskStatus.TODO);

        service.undo();

        createSampleTask("Task 2", TaskStatus.TODO);

        boolean redone = service.redo();

        assertFalse(redone);
        assertEquals(1, service.getAllTasks().size());
        assertEquals("Task 2", service.getAllTasks().get(0).getTitle());
    }

    @Test
    void shouldQueryTasksWithFilterSortAndPagination() {
        createSampleTask("Alpha", TaskStatus.TODO);
        createSampleTask("Beta", TaskStatus.IN_PROGRESS);
        createSampleTask("Gamma", TaskStatus.TODO);

        TaskQuery query = new TaskQuery(
                TaskStatus.TODO,
                "",
                TaskService.SortBy.TITLE,
                false,
                1,
                10
        );

        List<Task> result = service.queryTasks(query);

        assertEquals(2, result.size());
        assertEquals("Alpha", result.get(0).getTitle());
        assertEquals("Gamma", result.get(1).getTitle());
    }

    @Test
    void shouldCountByStatus() {
        createSampleTask("Todo 1", TaskStatus.TODO);
        createSampleTask("Todo 2", TaskStatus.TODO);
        createSampleTask("Blocked", TaskStatus.BLOCKED);

        Map<TaskStatus, Integer> counts = service.countByStatus();

        assertEquals(2, counts.get(TaskStatus.TODO));
        assertEquals(1, counts.get(TaskStatus.BLOCKED));
        assertEquals(0, counts.get(TaskStatus.DONE));
    }

    @Test
    void shouldGetMostUrgentTaskIgnoringDone() {
        service.createTask("Done urgent", "", 5, LocalDate.now().plusDays(1), TaskStatus.DONE, 10);
        service.createTask("Active urgent", "", 3, LocalDate.now().plusDays(2), TaskStatus.TODO, 10);
        service.createTask("Active later", "", 4, LocalDate.now().plusDays(10), TaskStatus.TODO, 10);

        Task mostUrgent = service.getMostUrgentTask();

        assertNotNull(mostUrgent);
        assertEquals("Active urgent", mostUrgent.getTitle());
    }

    @Test
    void shouldRejectPastDeadline() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.createTask(
                        "Past task",
                        "",
                        3,
                        LocalDate.now().minusDays(1),
                        TaskStatus.TODO,
                        20
                )
        );
    }

    private Task createSampleTask(String title, TaskStatus status) {
        return service.createTask(
                title,
                "descriere",
                3,
                LocalDate.now().plusDays(5),
                status,
                30
        );
    }
}