package ro.kutaba.taskmanager.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.kutaba.taskmanager.api.dto.AuthResponse;
import ro.kutaba.taskmanager.api.dto.LoginRequest;
import ro.kutaba.taskmanager.api.dto.MessageResponse;
import ro.kutaba.taskmanager.api.dto.UserRequest;
import ro.kutaba.taskmanager.model.User;
import ro.kutaba.taskmanager.service.JwtService;
import ro.kutaba.taskmanager.service.UserService;

@RestController
@RequestMapping("/api/auth")
public class AuthController{
    private final UserService userService;
    private final JwtService jwtService;

    public AuthController(UserService userService, JwtService jwtService){
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<MessageResponse> register(@Valid @RequestBody UserRequest request){
        User user = new User();
        user.setUsername(request.username());
        user.setPassword(request.password());
        userService.register(user);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new MessageResponse("User registered successfully"));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request){
        User user = userService.login(request.getUsername(), request.getPassword());
        String token = jwtService.generateToken(user);
        return new AuthResponse(token);
    }
}
