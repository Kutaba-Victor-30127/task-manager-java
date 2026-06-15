package ro.kutaba.taskmanager.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import ro.kutaba.taskmanager.api.dto.LoginRequest;
import ro.kutaba.taskmanager.api.dto.UserRequest;
import ro.kutaba.taskmanager.model.Role;
import ro.kutaba.taskmanager.model.User;
import ro.kutaba.taskmanager.repository.TaskJpaRepository;
import ro.kutaba.taskmanager.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class AuthControllerMockMvcTest {

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
        taskJpaRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void register_validRequest_returnsCreated() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserRequest("alice", "secret123"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("User registered successfully"));

        User savedUser = userRepository.findByUsername("alice").orElseThrow();
        assertTrue(passwordEncoder.matches("secret123", savedUser.getPassword()));
        assertFalse("secret123".equals(savedUser.getPassword()));
    }

    @Test
    void register_duplicateUsername_returnsConflict() throws Exception {
        saveUser("alice", "encoded-password", Role.USER);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserRequest("alice", "secret123"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Username already exists: alice"));
    }

    @Test
    void login_validRequest_returnsToken() throws Exception {
        saveUser("alice", passwordEncoder.encode("secret123"), Role.USER);

        LoginRequest request = new LoginRequest();
        request.setUsername("alice");
        request.setPassword("secret123");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertFalse(body.get("token").asText().isBlank());
    }

    @Test
    void login_invalidBody_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fields.username").value("Username is required"))
                .andExpect(jsonPath("$.fields.password").value("Password is required"));
    }

    private User saveUser(String username, String password, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setRole(role);
        return userRepository.save(user);
    }
}
