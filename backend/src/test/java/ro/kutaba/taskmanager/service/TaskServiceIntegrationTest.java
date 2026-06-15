package ro.kutaba.taskmanager.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import ro.kutaba.taskmanager.api.dto.PageResponse;
import ro.kutaba.taskmanager.api.dto.TaskResponse;
import ro.kutaba.taskmanager.model.Role;
import ro.kutaba.taskmanager.model.Task;
import ro.kutaba.taskmanager.model.TaskEntity;
import ro.kutaba.taskmanager.model.TaskStatus;
import ro.kutaba.taskmanager.model.User;
import ro.kutaba.taskmanager.repository.TaskJpaRepository;
import ro.kutaba.taskmanager.repository.UserRepository;
import ro.kutaba.taskmanager.storage.TaskRepository;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class TaskServiceIntegrationTest {

    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskJpaRepository taskJpaRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabaseBeforeEachTest() {
        SecurityContextHolder.clearContext();
        taskJpaRepository.deleteAll();
        userRepository.deleteAll();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createTask_persistsTaskForAuthenticatedUser() {
        User alice = saveUser("alice", Role.USER);
        authenticateAs("alice", "ROLE_USER");

        Task created = taskService.createTask(
                "Integration task",
                "Saved through real repository",
                3,
                LocalDate.now().plusDays(2),
                TaskStatus.TODO,
                45
        );

        assertNotNull(created.getId());

        List<TaskEntity> storedTasks = taskJpaRepository.findByUserUsername("alice");
        assertEquals(1, storedTasks.size());
        assertEquals("Integration task", storedTasks.get(0).getTitle());
        assertEquals(alice.getId(), storedTasks.get(0).getUser().getId());
    }

    @Test
    void getTasksFiltered_returnsOnlyTasksOfCurrentUser() {
        User alice = saveUser("alice", Role.USER);
        User bob = saveUser("bob", Role.USER);
        saveTaskEntity(alice, "Alice task", TaskStatus.TODO);
        saveTaskEntity(bob, "Bob task", TaskStatus.TODO);

        authenticateAs("alice", "ROLE_USER");

        PageResponse<TaskResponse> response = taskService.getTasksFiltered(
                0,
                10,
                "id",
                "asc",
                "",
                null
        );

        assertEquals(1, response.content().size());
        assertEquals("Alice task", response.content().get(0).title());
    }

    @Test
    void getAllTasks_returnsTasksFromAllUsers() {
        User admin = saveUser("admin", Role.ADMIN);
        User alice = saveUser("alice", Role.USER);
        User bob = saveUser("bob", Role.USER);

        saveTaskEntity(alice, "Task A", TaskStatus.TODO);
        saveTaskEntity(bob, "Task B", TaskStatus.IN_PROGRESS);

        authenticateAs(admin.getUsername(), "ROLE_ADMIN");

        PageResponse<TaskResponse> response = taskService.getAllTasks(
                0,
                10,
                "id",
                "asc",
                "",
                null
        );

        assertEquals(2, response.totalElements());
        assertEquals(2, response.content().size());
    }

    @Test
    void deleteTask_regularUserCannotDeleteAnotherUsersTask() {
        User alice = saveUser("alice", Role.USER);
        User bob = saveUser("bob", Role.USER);
        TaskEntity bobTask = saveTaskEntity(bob, "Private task", TaskStatus.TODO);

        authenticateAs(alice.getUsername(), "ROLE_USER");

        boolean deleted = taskService.deleteTask(bobTask.getId().intValue());

        assertFalse(deleted);
        assertTrue(taskJpaRepository.findById(bobTask.getId()).isPresent());
    }

    @Test
    void updateStatusForUser_validTransition_persistsNewStatus() {
        User alice = saveUser("alice", Role.USER);
        TaskEntity taskEntity = saveTaskEntity(alice, "Status task", TaskStatus.TODO);

        authenticateAs("alice", "ROLE_USER");

        boolean updated = taskService.updateStatusForUser(taskEntity.getId().intValue(), TaskStatus.IN_PROGRESS);

        assertTrue(updated);
        Task reloaded = taskRepository.findById(taskEntity.getId().intValue()).orElseThrow();
        assertEquals(TaskStatus.IN_PROGRESS, reloaded.getStatus());
    }

    private User saveUser(String username, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode("password123"));
        user.setRole(role);
        return userRepository.save(user);
    }

    private TaskEntity saveTaskEntity(User user, String title, TaskStatus status) {
        TaskEntity entity = new TaskEntity(
                title,
                "Stored directly for integration test setup",
                3,
                LocalDate.now().plusDays(5),
                status,
                60,
                user
        );
        return taskJpaRepository.save(entity);
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
}
