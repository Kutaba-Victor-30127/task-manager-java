package ro.kutaba.taskmanager.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import ro.kutaba.taskmanager.model.Role;
import ro.kutaba.taskmanager.model.TaskEntity;
import ro.kutaba.taskmanager.model.TaskStatus;
import ro.kutaba.taskmanager.model.User;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
class TaskJpaRepositoryTest {

    @Autowired
    private TaskJpaRepository taskJpaRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanDatabase() {
        taskJpaRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void findByUserAndStatus_returnsMatchingTasksOnly() {
        User alice = saveUser("alice");
        saveTask(alice, "Todo task", TaskStatus.TODO);
        saveTask(alice, "Done task", TaskStatus.DONE);

        Page<TaskEntity> result = taskJpaRepository.findByUserAndStatus(
                alice,
                TaskStatus.TODO,
                PageRequest.of(0, 10)
        );

        assertEquals(1, result.getTotalElements());
        assertEquals("Todo task", result.getContent().get(0).getTitle());
    }

    @Test
    void findByIdAndUserUsername_returnsTaskOnlyForOwner() {
        User alice = saveUser("alice");
        saveUser("bob");
        TaskEntity task = saveTask(alice, "Private task", TaskStatus.TODO);

        Optional<TaskEntity> ownerResult = taskJpaRepository.findByIdAndUserUsername(task.getId(), "alice");
        Optional<TaskEntity> otherUserResult = taskJpaRepository.findByIdAndUserUsername(task.getId(), "bob");

        assertTrue(ownerResult.isPresent());
        assertFalse(otherUserResult.isPresent());
        assertEquals("Private task", ownerResult.get().getTitle());
    }

    @Test
    void findByTitleContainingAndStatus_filtersAcrossUsers() {
        User alice = saveUser("alice");
        User bob = saveUser("bob");
        saveTask(alice, "API bug", TaskStatus.BLOCKED);
        saveTask(bob, "API docs", TaskStatus.BLOCKED);
        saveTask(bob, "Frontend bug", TaskStatus.TODO);

        Page<TaskEntity> result = taskJpaRepository.findByTitleContainingAndStatus(
                "API",
                TaskStatus.BLOCKED,
                PageRequest.of(0, 10)
        );

        assertEquals(2, result.getTotalElements());
    }

    @Test
    void findByUserAndTitleContainingAndStatus_returnsPagedResults() {
        User alice = saveUser("alice");
        saveTask(alice, "Prepare report", TaskStatus.TODO);
        saveTask(alice, "Prepare slides", TaskStatus.TODO);
        saveTask(alice, "Deploy backend", TaskStatus.IN_PROGRESS);

        Page<TaskEntity> result = taskJpaRepository.findByUserAndTitleContainingAndStatus(
                alice,
                "Prepare",
                TaskStatus.TODO,
                PageRequest.of(0, 1)
        );

        assertEquals(2, result.getTotalElements());
        assertEquals(1, result.getContent().size());
    }

    private User saveUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setPassword("encoded-password");
        user.setRole(Role.USER);
        return userRepository.save(user);
    }

    private TaskEntity saveTask(User user, String title, TaskStatus status) {
        TaskEntity entity = new TaskEntity(
                title,
                "Persisted for repository tests",
                3,
                LocalDate.now().plusDays(5),
                status,
                60,
                user
        );
        return taskJpaRepository.save(entity);
    }
}
