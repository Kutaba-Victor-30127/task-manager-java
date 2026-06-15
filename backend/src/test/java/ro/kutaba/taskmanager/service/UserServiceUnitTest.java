package ro.kutaba.taskmanager.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import ro.kutaba.taskmanager.api.error.InvalidCredentialsException;
import ro.kutaba.taskmanager.api.error.UsernameAlreadyExistsException;
import ro.kutaba.taskmanager.model.Role;
import ro.kutaba.taskmanager.model.User;
import ro.kutaba.taskmanager.repository.UserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void register_newUser_encodesPasswordAndAssignsUserRole() {
        User input = new User();
        input.setUsername("alice");
        input.setPassword("secret123");

        when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret123")).thenReturn("encoded-secret");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User saved = userService.register(input);

        assertEquals(Role.USER, saved.getRole());
        assertEquals("encoded-secret", saved.getPassword());
        verify(userRepository).save(saved);
    }

    @Test
    void register_duplicateUsername_throwsException() {
        User existing = new User();
        existing.setUsername("alice");

        User input = new User();
        input.setUsername("alice");
        input.setPassword("secret123");

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(existing));

        UsernameAlreadyExistsException exception = assertThrows(
                UsernameAlreadyExistsException.class,
                () -> userService.register(input)
        );

        assertEquals("Username already exists: alice", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_validCredentials_returnsUser() {
        User user = new User();
        user.setUsername("alice");
        user.setPassword("encoded-secret");

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret123", "encoded-secret")).thenReturn(true);

        User result = userService.login("alice", "secret123");

        assertEquals("alice", result.getUsername());
    }

    @Test
    void login_wrongPassword_throwsInvalidCredentials() {
        User user = new User();
        user.setUsername("alice");
        user.setPassword("encoded-secret");

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "encoded-secret")).thenReturn(false);

        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> userService.login("alice", "wrong-password")
        );

        assertEquals("Invalid username or password", exception.getMessage());
    }

    @Test
    void login_unknownUser_throwsInvalidCredentials() {
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> userService.login("missing", "secret123")
        );

        assertTrue(exception.getMessage().contains("Invalid username or password"));
    }
}
