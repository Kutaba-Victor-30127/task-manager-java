package ro.kutaba.taskmanager.service;

import org.springframework.stereotype.Service;
import ro.kutaba.taskmanager.model.User;
import ro.kutaba.taskmanager.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import ro.kutaba.taskmanager.api.error.InvalidCredentialsException;
import ro.kutaba.taskmanager.api.error.UsernameAlreadyExistsException;
import ro.kutaba.taskmanager.model.Role;

@Service
public class UserService{
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder){
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User register(User user){
        if (userRepository.findByUsername(user.getUsername()).isPresent()){
            throw new UsernameAlreadyExistsException(user.getUsername());
        }

        user.setRole(Role.USER);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    public User login(String username, String password){
        User user = userRepository.findByUsername(username)
                    .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(password, user.getPassword())){
            throw new InvalidCredentialsException();
        }

        return user;
    }
}

