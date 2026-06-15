package ro.kutaba.taskmanager.api;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ro.kutaba.taskmanager.model.Role;
import ro.kutaba.taskmanager.model.TaskEntity;
import ro.kutaba.taskmanager.model.TaskStatus;
import ro.kutaba.taskmanager.model.User;
import ro.kutaba.taskmanager.repository.TaskJpaRepository;
import ro.kutaba.taskmanager.repository.UserRepository;

import java.time.LocalDate;
import java.util.Collections;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class AdminControllerMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

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
    void getAllTasks_withSortAlias_returnsPage() throws Exception {
        User admin = saveUser("admin", Role.ADMIN);
        User alice = saveUser("alice", Role.USER);
        saveTask(alice, "Lower priority", TaskStatus.BLOCKED, 2);
        saveTask(alice, "Higher priority", TaskStatus.BLOCKED, 5);
        authenticateAs(admin.getUsername(), "ROLE_ADMIN");

        mockMvc.perform(get("/api/admin/all-tasks")
                        .param("sort", "priority")
                        .param("direction", "desc")
                        .param("status", "BLOCKED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Higher priority"));
    }

    private User saveUser(String username, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode("secret123"));
        user.setRole(role);
        return userRepository.save(user);
    }

    private TaskEntity saveTask(User user, String title, TaskStatus status, int priority) {
        TaskEntity entity = new TaskEntity(
                title,
                "Persisted for admin controller tests",
                priority,
                LocalDate.now().plusDays(4),
                status,
                45,
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
