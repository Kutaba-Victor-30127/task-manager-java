package ro.kutaba.taskmanager.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ro.kutaba.taskmanager.api.dto.CreateTaskRequest;
import ro.kutaba.taskmanager.model.Role;
import ro.kutaba.taskmanager.model.TaskEntity;
import ro.kutaba.taskmanager.model.TaskStatus;
import ro.kutaba.taskmanager.model.User;
import ro.kutaba.taskmanager.repository.TaskJpaRepository;
import ro.kutaba.taskmanager.repository.UserRepository;

import java.time.LocalDate;
import java.util.Collections;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class TaskControllerMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskJpaRepository taskJpaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        SecurityContextHolder.clearContext();
        taskJpaRepository.deleteAll();
        userRepository.deleteAll();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getTasks_withSortAlias_returnsSortedPage() throws Exception {
        User alice = saveUser("alice", Role.USER);
        saveTask(alice, "Second task", TaskStatus.TODO, LocalDate.now().plusDays(3));
        saveTask(alice, "First task", TaskStatus.TODO, LocalDate.now().plusDays(1));
        authenticateAs("alice", "ROLE_USER");

        mockMvc.perform(get("/api/tasks")
                        .param("sort", "deadline")
                        .param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("First task"))
                .andExpect(jsonPath("$.content[1].title").value("Second task"));
    }

    @Test
    void getTaskById_missingTask_returnsNotFound() throws Exception {
        saveUser("alice", Role.USER);
        authenticateAs("alice", "ROLE_USER");

        mockMvc.perform(get("/api/tasks/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Task not found: id=99"));
    }

    @Test
    void create_invalidBody_returnsValidationErrors() throws Exception {
        CreateTaskRequest request = new CreateTaskRequest(
                "",
                "",
                0,
                null,
                null,
                0
        );

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.title").value("Title must not be blank"))
                .andExpect(jsonPath("$.fields.description").value("Description must not be blank"))
                .andExpect(jsonPath("$.fields.priority").value("Priority must be at least 1"))
                .andExpect(jsonPath("$.fields.deadline").value("Deadline is required"))
                .andExpect(jsonPath("$.fields.status").value("Status is required"))
                .andExpect(jsonPath("$.fields.estimatedMinutes").value("Estimated minutes must be at least 1"));
    }

    @Test
    void updateStatus_whenServiceRejectsTransition_returnsBadRequest() throws Exception {
        User alice = saveUser("alice", Role.USER);
        TaskEntity task = saveTask(alice, "Finished task", TaskStatus.DONE, LocalDate.now().plusDays(2));
        authenticateAs("alice", "ROLE_USER");

        mockMvc.perform(patch("/api/tasks/" + task.getId() + "/status")
                        .param("status", "TODO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid status transition: DONE -> TODO"));
    }

    private User saveUser(String username, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode("secret123"));
        user.setRole(role);
        return userRepository.save(user);
    }

    private TaskEntity saveTask(User user, String title, TaskStatus status, LocalDate deadline) {
        TaskEntity entity = new TaskEntity(
                title,
                "Persisted for controller tests",
                3,
                deadline,
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
