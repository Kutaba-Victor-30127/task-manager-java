package ro.kutaba.taskmanager.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ro.kutaba.taskmanager.model.Role;
import ro.kutaba.taskmanager.model.TaskEntity;
import ro.kutaba.taskmanager.model.TaskStatus;
import ro.kutaba.taskmanager.model.User;
import ro.kutaba.taskmanager.repository.TaskJpaRepository;
import ro.kutaba.taskmanager.repository.UserRepository;
import ro.kutaba.taskmanager.service.JwtService;

import java.time.LocalDate;
import java.util.Base64;
import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JwtSecurityMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskJpaRepository taskJpaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @BeforeEach
    void cleanDatabase() {
        taskJpaRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void protectedUserEndpoint_withoutToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required to access this resource"));
    }

    @Test
    void protectedUserEndpoint_withInvalidToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/tasks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedUserEndpoint_withExpiredToken_returnsUnauthorized() throws Exception {
        User user = saveUser("alice", Role.USER);
        String expiredToken = expiredTokenFor(user);

        mockMvc.perform(get("/api/tasks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedUserEndpoint_withValidToken_returnsOk() throws Exception {
        User user = saveUser("alice", Role.USER);
        saveTask(user, "Secure task", TaskStatus.TODO);
        String token = jwtService.generateToken(user);

        mockMvc.perform(get("/api/tasks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Secure task"));
    }

    @Test
    void adminEndpoint_withUserToken_returnsForbidden() throws Exception {
        User user = saveUser("alice", Role.USER);
        String token = jwtService.generateToken(user);

        mockMvc.perform(get("/api/admin/all-tasks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access is denied"));
    }

    @Test
    void publicEndpoint_withoutToken_isAccessibleToAnonymousUsers() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "",
                                  "password": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    private User saveUser(String username, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode("secret123"));
        user.setRole(role);
        return userRepository.save(user);
    }

    private TaskEntity saveTask(User user, String title, TaskStatus status) {
        TaskEntity entity = new TaskEntity(
                title,
                "Stored for security test",
                3,
                LocalDate.now().plusDays(3),
                status,
                60,
                user
        );
        return taskJpaRepository.save(entity);
    }

    private String expiredTokenFor(User user) {
        byte[] keyBytes = Base64.getDecoder().decode(jwtSecret);
        Date now = new Date();
        Date issuedAt = new Date(now.getTime() - 7_200_000);
        Date expiration = new Date(now.getTime() - 3_600_000);

        return Jwts.builder()
                .setSubject(user.getUsername())
                .claim("role", user.getRole().name())
                .setIssuedAt(issuedAt)
                .setExpiration(expiration)
                .signWith(Keys.hmacShaKeyFor(keyBytes), SignatureAlgorithm.HS256)
                .compact();
    }
}
