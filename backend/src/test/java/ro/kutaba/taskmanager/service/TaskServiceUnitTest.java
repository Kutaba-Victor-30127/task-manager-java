package ro.kutaba.taskmanager.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import ro.kutaba.taskmanager.api.dto.PageResponse;
import ro.kutaba.taskmanager.api.dto.TaskResponse;
import ro.kutaba.taskmanager.api.error.TaskNotFoundException;
import ro.kutaba.taskmanager.model.Role;
import ro.kutaba.taskmanager.model.Task;
import ro.kutaba.taskmanager.model.TaskStatus;
import ro.kutaba.taskmanager.model.User;
import ro.kutaba.taskmanager.repository.UserRepository;
import ro.kutaba.taskmanager.storage.AuditLogger;
import ro.kutaba.taskmanager.storage.TaskRepository;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceUnitTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogger auditLogger;

    @InjectMocks
    private TaskService taskService;

    @BeforeEach
    void setUpAuthentication() {
        authenticateAs("alice", "ROLE_USER");
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createTask_validInput_savesTaskForCurrentUser() {
        User alice = user("alice", Role.USER);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));

        LocalDate deadline = LocalDate.now().plusDays(2);
        Task savedTask = new Task(1, "Write tests", "Unit test service", 3, deadline, TaskStatus.TODO, 90);
        when(taskRepository.save(any(Task.class), eq(alice))).thenReturn(savedTask);

        Task result = taskService.createTask(
                "Write tests",
                "Unit test service",
                3,
                deadline,
                TaskStatus.TODO,
                90
        );

        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("Write tests", result.getTitle());

        verify(taskRepository).save(any(Task.class), eq(alice));
        verify(auditLogger).log(contains("TASK_CREATED"));
    }

    @Test
    void createTask_pastDeadline_throwsExceptionAndDoesNotSave() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> taskService.createTask(
                        "Past task",
                        "Should fail",
                        2,
                        LocalDate.now().minusDays(1),
                        TaskStatus.TODO,
                        30
                )
        );

        assertEquals("Deadline must not be in the past", exception.getMessage());
        verify(taskRepository, never()).save(any(Task.class), any(User.class));
        verify(auditLogger, never()).log(any(String.class));
    }

    @Test
    void findAllForAdmin_delegatesToRepository() {
        Pageable pageable = PageRequest.of(0, 5);
        Page<Task> expected = new PageImpl<>(List.of(
                new Task(1, "Admin task", "Visible to admin", 2, LocalDate.now().plusDays(1), TaskStatus.TODO, 20)
        ));
        when(taskRepository.findAll(pageable)).thenReturn(expected);

        Page<Task> result = taskService.findAllForAdmin(pageable);

        assertEquals(1, result.getContent().size());
        verify(taskRepository).findAll(pageable);
    }

    @Test
    void getTasksForCurrentUser_usesAuthenticatedUsername() {
        List<Task> tasks = List.of(
                new Task(1, "Task A", "Owned by Alice", 2, LocalDate.now().plusDays(1), TaskStatus.TODO, 30)
        );
        when(taskRepository.findByUsername("alice")).thenReturn(tasks);

        List<Task> result = taskService.getTasksForCurrentUser();

        assertEquals(1, result.size());
        assertEquals("Task A", result.get(0).getTitle());
        verify(taskRepository).findByUsername("alice");
    }

    @Test
    void findByIdForCurrentUser_missingTask_throwsNotFound() {
        when(taskRepository.findByIdAndUsername(55, "alice")).thenReturn(Optional.empty());

        TaskNotFoundException exception = assertThrows(
                TaskNotFoundException.class,
                () -> taskService.findByIdForCurrentUser(55)
        );

        assertEquals("Task not found: id=55", exception.getMessage());
    }

    @Test
    void deleteTaskForCurrentUser_existingTask_logsAndReturnsTrue() {
        when(taskRepository.deleteByIdAndUsername(5, "alice")).thenReturn(true);

        boolean deleted = taskService.deleteTaskForCurrentUser(5);

        assertTrue(deleted);
        verify(taskRepository).deleteByIdAndUsername(5, "alice");
        verify(auditLogger).log(contains("TASK_DELETE_REQUEST"));
    }

    @Test
    void deleteTask_adminRole_usesGlobalDelete() {
        authenticateAs("admin", "ROLE_ADMIN");
        when(taskRepository.deleteById(10)).thenReturn(true);

        boolean deleted = taskService.deleteTask(10);

        assertTrue(deleted);
        verify(taskRepository).deleteById(10);
        verify(taskRepository, never()).deleteByIdAndUsername(any(Integer.class), any(String.class));
    }

    @Test
    void updateStatusForUser_invalidTransition_throwsAndDoesNotSave() {
        Task existingTask = new Task(
                4,
                "Finished feature",
                "Already done",
                4,
                LocalDate.now().plusDays(1),
                TaskStatus.DONE,
                60
        );
        when(taskRepository.findByIdAndUsername(4, "alice")).thenReturn(Optional.of(existingTask));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> taskService.updateStatusForUser(4, TaskStatus.TODO)
        );

        assertEquals("Invalid status transition: DONE -> TODO", exception.getMessage());
        verify(taskRepository, never()).save(any(Task.class), any(User.class));
        verify(auditLogger, never()).log(any(String.class));
    }

    @Test
    void updateTaskFullForUser_validInput_savesUpdatedTask() {
        User alice = user("alice", Role.USER);
        Task existingTask = new Task(
                9,
                "Old title",
                "Old description",
                2,
                LocalDate.now().plusDays(1),
                TaskStatus.TODO,
                30
        );
        when(taskRepository.findByIdAndUsername(9, "alice")).thenReturn(Optional.of(existingTask));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(taskRepository.save(existingTask, alice)).thenReturn(existingTask);

        boolean updated = taskService.updateTaskFullForUser(
                9,
                "New title",
                "New description",
                5,
                LocalDate.now().plusDays(7),
                TaskStatus.IN_PROGRESS,
                120
        );

        assertTrue(updated);
        assertEquals("New title", existingTask.getTitle());
        assertEquals(TaskStatus.IN_PROGRESS, existingTask.getStatus());
        verify(taskRepository).save(existingTask, alice);
        verify(auditLogger).log(contains("TASK_UPDATED"));
    }

    @Test
    void getTasksFiltered_statusAndText_usesCombinedRepositoryQuery() {
        User alice = user("alice", Role.USER);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));

        Task task = new Task(
                7,
                "Prepare report",
                "Quarterly report",
                5,
                LocalDate.now().plusDays(3),
                TaskStatus.TODO,
                120
        );
        Page<Task> page = new PageImpl<>(List.of(task));
        when(taskRepository.findByUserAndTitleContainingAndStatus(
                eq(alice),
                eq("report"),
                eq(TaskStatus.TODO),
                any(Pageable.class)
        )).thenReturn(page);

        PageResponse<TaskResponse> response = taskService.getTasksFiltered(
                0,
                5,
                "unknownField",
                "desc",
                "report",
                TaskStatus.TODO
        );

        assertEquals(1, response.content().size());
        assertEquals("Prepare report", response.content().get(0).title());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(taskRepository).findByUserAndTitleContainingAndStatus(
                eq(alice),
                eq("report"),
                eq(TaskStatus.TODO),
                pageableCaptor.capture()
        );

        Pageable usedPageable = pageableCaptor.getValue();
        assertEquals(0, usedPageable.getPageNumber());
        assertEquals(5, usedPageable.getPageSize());
        assertTrue(usedPageable.getSort().getOrderFor("id").isDescending());
    }

    @Test
    void getAllTasks_statusOnly_usesGlobalStatusQuery() {
        Task task = new Task(
                14,
                "Blocked task",
                "Global admin view",
                4,
                LocalDate.now().plusDays(2),
                TaskStatus.BLOCKED,
                70
        );
        when(taskRepository.findByStatus(eq(TaskStatus.BLOCKED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(task)));

        PageResponse<TaskResponse> response = taskService.getAllTasks(
                0,
                10,
                "priority",
                "asc",
                "",
                TaskStatus.BLOCKED
        );

        assertEquals(1, response.content().size());
        assertEquals("Blocked task", response.content().get(0).title());
        verify(taskRepository).findByStatus(eq(TaskStatus.BLOCKED), any(Pageable.class));
    }

    private static void authenticateAs(String username, String role) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority(role))
                );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private static User user(String username, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setPassword("encoded-password");
        user.setRole(role);
        return user;
    }
}
