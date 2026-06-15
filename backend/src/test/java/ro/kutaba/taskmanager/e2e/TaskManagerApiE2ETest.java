package ro.kutaba.taskmanager.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import ro.kutaba.taskmanager.api.dto.CreateTaskRequest;
import ro.kutaba.taskmanager.api.dto.LoginRequest;
import ro.kutaba.taskmanager.api.dto.UserRequest;
import ro.kutaba.taskmanager.model.Role;
import ro.kutaba.taskmanager.model.TaskStatus;
import ro.kutaba.taskmanager.model.User;
import ro.kutaba.taskmanager.repository.TaskJpaRepository;
import ro.kutaba.taskmanager.repository.UserRepository;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class TaskManagerApiE2ETest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskJpaRepository taskJpaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        taskJpaRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void registerLoginCreateTaskAndFetchTasks_fullUserFlow() {
        ResponseEntity<String> registerResponse = restTemplate.postForEntity(
                "/api/auth/register",
                new UserRequest("flow-user", "secret123"),
                String.class
        );
        assertEquals(HttpStatus.CREATED, registerResponse.getStatusCode());

        String token = loginAndGetToken("flow-user", "secret123");
        assertNotNull(token);
        assertFalse(token.isBlank());

        CreateTaskRequest createRequest = new CreateTaskRequest(
                "E2E task",
                "Created through full API flow",
                3,
                LocalDate.now().plusDays(2),
                TaskStatus.TODO,
                40
        );

        ResponseEntity<JsonNode> createResponse = restTemplate.exchange(
                "/api/tasks",
                HttpMethod.POST,
                new HttpEntity<>(createRequest, bearerHeaders(token)),
                JsonNode.class
        );
        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        assertEquals("E2E task", createResponse.getBody().get("title").asText());

        ResponseEntity<JsonNode> tasksResponse = restTemplate.exchange(
                "/api/tasks?page=0&size=5&sortBy=id&direction=asc&text=",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(token)),
                JsonNode.class
        );

        assertEquals(HttpStatus.OK, tasksResponse.getStatusCode());
        assertEquals(1, tasksResponse.getBody().get("content").size());
        assertEquals("E2E task", tasksResponse.getBody().get("content").get(0).get("title").asText());
    }

    @Test
    void fetchTasksWithoutAuthentication_returnsClientError() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/tasks", String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void adminCanLoginAndFetchAllTasks_createdByDifferentUser() {
        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);

        restTemplate.postForEntity(
                "/api/auth/register",
                new UserRequest("normal-user", "secret123"),
                String.class
        );

        String userToken = loginAndGetToken("normal-user", "secret123");

        CreateTaskRequest createRequest = new CreateTaskRequest(
                "User owned task",
                "Visible to admin endpoint",
                4,
                LocalDate.now().plusDays(3),
                TaskStatus.TODO,
                55
        );

        ResponseEntity<JsonNode> createResponse = restTemplate.exchange(
                "/api/tasks",
                HttpMethod.POST,
                new HttpEntity<>(createRequest, bearerHeaders(userToken)),
                JsonNode.class
        );
        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());

        String adminToken = loginAndGetToken("admin", "admin123");

        ResponseEntity<JsonNode> adminTasksResponse = restTemplate.exchange(
                "/api/admin/all-tasks?page=0&size=10&sort=id&direction=asc&text=",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(adminToken)),
                JsonNode.class
        );

        assertEquals(HttpStatus.OK, adminTasksResponse.getStatusCode());
        assertTrue(adminTasksResponse.getBody().get("content").size() >= 1);
        assertEquals("User owned task", adminTasksResponse.getBody().get("content").get(0).get("title").asText());
    }

    private String loginAndGetToken(String username, String password) {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(username);
        loginRequest.setPassword(password);

        ResponseEntity<JsonNode> loginResponse = restTemplate.postForEntity(
                "/api/auth/login",
                loginRequest,
                JsonNode.class
        );

        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
        return loginResponse.getBody().get("token").asText();
    }

    private HttpHeaders bearerHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return headers;
    }
}
