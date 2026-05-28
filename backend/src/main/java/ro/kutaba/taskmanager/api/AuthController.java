package ro.kutaba.taskmanager.api;

import org.springframework.web.bind.annotation.*;
import ro.kutaba.taskmanager.model.User;
import ro.kutaba.taskmanager.model.Role;
import ro.kutaba.taskmanager.api.dto.AuthResponse;
import ro.kutaba.taskmanager.api.dto.LoginRequest;
import ro.kutaba.taskmanager.service.JwtService;
import ro.kutaba.taskmanager.api.dto.UserRequest;
import ro.kutaba.taskmanager.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import ro.kutaba.taskmanager.api.dto.AuthResponse;

@RestController
@RequestMapping("/api/auth")
public class AuthController{
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository, JwtService jwtService, PasswordEncoder passwordEncoder){
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public String register(@RequestBody UserRequest request){
        if (userRepository.existsByUsername(request.username())){
            throw new IllegalArgumentException("Username deja exista");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.USER);
        userRepository.save(user);

        return "User creat";
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request){
        User user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new IllegalArgumentException("User nu exista"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())){
            throw new IllegalArgumentException("Parola gresita");
        }     

        String token = jwtService.generateToken(user);
        return new AuthResponse(token); 
    }
}
