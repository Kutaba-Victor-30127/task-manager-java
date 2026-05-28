package ro.kutaba.taskmanager.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class TaskTest {

    @Test
    void shouldCreateValidTask() {
        Task task = new Task(
                1,
                "Task valid",
                "descriere",
                3,
                LocalDate.now().plusDays(1),
                TaskStatus.TODO,
                30
        );

        assertEquals(1, task.getId());
        assertEquals("Task valid", task.getTitle());
        assertEquals(TaskStatus.TODO, task.getStatus());
    }

    @Test
    void shouldRejectTitleThatIsTooShort() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Task(
                        1,
                        "ab",
                        "",
                        3,
                        LocalDate.now().plusDays(1),
                        TaskStatus.TODO,
                        30
                )
        );
    }

    @Test
    void shouldRejectInvalidPriority() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Task(
                        1,
                        "Task valid",
                        "",
                        6,
                        LocalDate.now().plusDays(1),
                        TaskStatus.TODO,
                        30
                )
        );
    }

    @Test
    void shouldRejectNullStatus() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Task(
                        1,
                        "Task valid",
                        "",
                        3,
                        LocalDate.now().plusDays(1),
                        null,
                        30
                )
        );
    }

    @Test
    void shouldRejectInvalidEstimatedMinutes() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Task(
                        1,
                        "Task valid",
                        "",
                        3,
                        LocalDate.now().plusDays(1),
                        TaskStatus.TODO,
                        0
                )
        );
    }

    @Test
    void shouldCopyTask() {
        Task original = new Task(
                1,
                "Original",
                "descriere",
                3,
                LocalDate.now().plusDays(1),
                TaskStatus.TODO,
                30
        );

        Task copy = original.copy();

        assertNotSame(original, copy);
        assertEquals(original.getId(), copy.getId());
        assertEquals(original.getTitle(), copy.getTitle());
        assertEquals(original.getStatus(), copy.getStatus());
    }
}